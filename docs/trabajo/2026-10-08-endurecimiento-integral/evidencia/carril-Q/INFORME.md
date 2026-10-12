# Carril Q — brechas de dinero abiertas (tercera/cuarta vuelta) — informe honesto

> **RESULTADOS DE EJECUCION: ver la seccion «Ejecucion» al final.** Todo lo de arriba de esa seccion es lo que se escribio y se
> decidio; lo que no figure en «Ejecucion» con salida literal esta en peldaño WRITTEN.

Decisiones de negocio: **adoptadas por delegacion del dueno, 2026-10-11** («lo mas recomendado, pero cerralo»), coincidentes con
ADR-051/052 de carril W (DD-1 precio − cargos; DD-8 billeteras al evento `usuario_registrado` y `grupo_creado`; `grupo_activado` nunca
se emite). Cada una queda como supuesto declarado con su confirmador.

## 1. Nadie abria las billeteras USUARIO / GRUPO

| Que | Donde | Prueba |
|---|---|---|
| `AperturaDeBilleteras`: consume `identidad.usuario_registrado` (abre BOB `SIMPLIFICADA`, con politica vigente) y `grupos.grupo_creado` (abre BOB del GRUPO, R-GRP-04). Idempotente por evento (`evento_consumido`) y por titular (consulta previa + indice unico R-BIL-04 + `ON CONFLICT DO NOTHING`); no reabre una `CERRADA`; fuera de orden lo rechaza la FK y el reproceso la abre; sin politica no abre | `nucleo-financiero/.../aplicacion/AperturaDeBilleteras.java`, `infraestructura/CuentaBilleteraRepositorio.java` (`abrir`, `politicaVigente`), `consumo/ConsumidorDeUsuariosKafka.java`, `ConsumidorDeGruposKafka.java`, `SobreDeEvento.java` | `CUAperturaDeBilleterasTest` (9), `ConsumidorDeAperturasKafkaTest` (3), `CUAperturaYAporteTest` (2: aporte cuadrado desde el libro y el mayor sobre billeteras abiertas por los consumidores; el rechazo AP-CU12-02 se reprocesa y aplica al abrirse la del grupo) |
| Interruptores (encendidos por omision) y estado inicial | `application.yml` (`aportaya.billetera.apertura.*`), `scripts/generar_compose.py` (`APERTURA_USUARIOS_HABILITADO`, `APERTURA_GRUPOS_HABILITADO`, `APERTURA_ESTADO_INICIAL_USUARIO`), compose y Coolify regenerados | `generar_compose.py` + `--coolify` |

**Supuestos declarados (a confirmar):** (a) solo BOB (el plan minimo no tiene cuentas en USD) — Contabilidad; (b) la cuenta de la persona nace
`ACTIVA`/`SIMPLIFICADA`: quien impide operar a una persona sin verificar es `identidad` (no inicia sesion hasta que su KYC se aprueba);
la alternativa `EN_APERTURA` esta implementada y probada, pero `identidad` no emite hoy una senal de «KYC aprobado», asi que nadie la
activaria — Cumplimiento + Producto; (c) sin cuenta contable espejo (el mayor se mueve por cuentas logicas del plan); (d) el diseno dice
`grupo_activado`, que `grupos` nunca emite: se consume `grupo_creado`.
**Fuera de mi alcance, queda desactualizado (dueno: carril P, grupos/identidad):** `CU-20` (Supuesto A1) y `grupos.yaml` (`crearGrupo`) siguen
diciendo que `nucleo-financiero` «todavia no» abre la cuenta del grupo; `CU-01` sigue describiendo la apertura como sincrona.

## 2. `HechosPorHttp` preguntaba a aportes por usuario

`aportes` indexa por `participanteId` de grupos (una persona tiene uno POR GRUPO): con el `usuarioId` contestaba siempre «al dia» y
la billetera de quien debia se podia cerrar; ademas `comprometidoEnPozos` daba cero. Ahora: con el token de la persona,
`GET /grupos/participaciones` y luego el estado de CADA participacion en aportes. Si la sesion no es la del titular, o algo no contesta o
viene ilegible: «tiene obligaciones» (invariante 9) y lo comprometido se omite (nunca cero). Archivos: `HechosPorHttp.java`.
Pruebas: `HechosPorHttpPorUsuarioTest` (7: deuda en una de dos participaciones, al dia, sin participaciones, otra moneda, sesion ajena, caidas),
`ContratosConOtrosServiciosTest` (adaptado a las rutas nuevas), `CU16CierreConDeudaTest` (3, contra la base real: **con deuda abierta no cierra**,
con aportes caido no cierra, al dia se solicita). Riesgo residual: `grupos` solo lista participaciones vigentes (no `RETIRADO`/`EXPULSADO`/
`REEMPLAZADO`, tope 50): la deuda de quien ya se fue no frena el cierre — dueno: Producto/Riesgos (¿una salida con deuda?), y `grupos` si quiere exponer todas.

## 3. Aporte PAGADO sin saldo se compensa (saga)

`ConsumoDeAportes`: AP-CU12-01 pasa de excepcion (el aporte quedaba `PAGADO`, el evento se reintentaba y al final se descartaba) a rechazo
DEFINITIVO: no escribe libro ni mayor, marca el evento consumido y publica `nucleo_financiero.aporte_no_aplicado`. `aportes`:
`CU21CompensarAporte` + `ConsumidorDeAportesNoAplicadosKafka` (encendido por omision, `COMPENSACION_CONSUMO_HABILITADO`): pago `ACREDITADO`→`REVERSADO`
(precondicion en el `UPDATE`), obligacion de vuelta a `PENDIENTE`/`EN_MORA`, bitacora, `aportes.aporte_compensado`. Idempotente por evento y por pago;
no compensa un pago con reembolsos o en disputa (`NO_COMPENSABLE`, lo resuelve una persona). Pacto: `contratos/pactos/aportes-nucleo-financiero.json`
(productor: `CUAporteNoAplicadoTest.cumpleElPactoConAportes`; consumidor: `ConsumidorDeAportesNoAplicadosKafkaTest`).
Pruebas: nucleo `CUAporteNoAplicadoTest` (4) + `CUConsumoDeAportesRechazosTest`/`ConcurrenciaTest` (adaptados: el segundo aporte sin saldo ya no lanza,
devuelve `RECHAZADO_POR_SALDO`); aportes `CU21CompensaTest` (10: compensa, duplicado, mismo pago otro evento, pago parcial, vencida→EN_MORA, sobre incoherente,
reembolsado/en disputa, pago inexistente, concurrencia, caida a mitad).
Decision que SI es de negocio y queda declarada: AP-CU12-03 (cuenta bloqueada/congelada) y AP-CU12-02 (sin cuenta) siguen reintentandose sin compensar;
y la persona NO recibe aun un aviso («tu aporte no se aplico») — `notificaciones` no consume `aporte_compensado` (dueno: Producto/notificaciones).

## 4. Venta de un turno: PRECIO − CARGOS (decision delegada)

`pagarAlTercero`: el comprador paga el precio (= lo retenido, exacto), el vendedor cobra `precio − cargos`, la plataforma `cargos`, cada uno con su linea;
precio y cargos los afirma `entregas`. `CU13EjecutarConDestino`, `PagoDeCompraEnMercado`, `OfertasDelMercado`, `OfertasPorHttp`; pacto
`nucleo-financiero-entregas.json` v2; **aditivo minimo en `entregas`** (fuera de la lista literal de servicios, necesario para el pacto: `OfertaParaPago.cargos` en
`entregas.yaml`, `OfertaParaElPago`, `GestionDeOfertas`, `MercadoWeb`; su prueba de forma exige ya el campo). Pruebas: `CU13MercadoTest` (cuadre 500 = 480 + 20 con
sus tres lineas y moneda; centavos 100.00/33.33; sin cargos; cargos = precio; cargos > precio, negativos, otra moneda, precio distinto de lo retenido rechazan;
duplicado por clave; caida de entregas; reversa devuelve tambien los cargos; concurrencia), `PactoConEntregasContratoTest`, `OfertaParaPagoWebTest`, `CU130OfertaParaPagoTest`.
`despliegue/ensayos/integracion/recorridos.py` (H9.S1.M5): la brecha DECISION_REQUIRED A9 pasa a comprobacion; **no corrido** (exige el stack).

## 5. Solo documentado (no se inventa)

* Canales distintos de `BILLETERA_MOVIL`: no mueven billetera (entran por la conciliacion y su propio asiento). Dueno: Contabilidad/Producto (contrapartida de QR, tarjeta, transferencia, efectivo al organizador, fondo de garantia).
* Limites por concepto de CU-40 sobre el aporte: no se aplican. Dueno: Riesgos/Producto (¿un aporte cuenta contra el limite de transferencia?).
* Codigos 2.1.01/2.1.02 (Contabilidad); recargas/retiros no asientan en el mayor (2.1.01 queda negativo); eventos `aporte_cobrado` anteriores sin los campos nuevos se descartan (backfill pendiente); cola de mensajes muertos con alerta (Operaciones).

## Ejecucion

(pendiente — ver abajo)
