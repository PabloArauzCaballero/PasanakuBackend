# Servicio `nucleo-financiero`

Modulo 10 de la boveda — Billetera, Custodia y Dinero Electrónico.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `nucleo_financiero` |
| **Rol de base** | `svc_nucleo_financiero` |
| **Prefijos de ruta** | `/billetera` · `/custodia` · `/puntos-atencion` · `/contabilidad` |
| **Contrato** | [`openapi/nucleo-financiero.yaml`](src/main/resources/openapi/nucleo-financiero.yaml) |
| **Paquete** | `bo.aportaya.nucleofinanciero` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| [CU-24](../../docs/CasosDeUso/CU-24%20Registrar%20el%20asiento%20contable%20de%20una%20operación.md) | Registrar el asiento contable de una operación | **Implementado** (carril 1B) |
| CU-40 | Evaluar límites antes de una operación | **Implementado** (carril 2A) |
| CU-13 | Retener y liberar saldo | **Implementado** (carril 2A) |
| CU-10 | Recargar saldo | **Implementado** (carril 2A) |
| CU-11 | Retirar saldo | **Implementado** (carril 2A) |
| CU-12 | Transferir saldo entre billeteras | **Implementado** (carril 2A) |
| CU-14 | Reversar una transacción | **Implementado** (carril 2A) |
| CU-15 | Emitir extracto y certificado de saldo | **Implementado** (carril 2A) |
| CU-16 | Cerrar billetera y devolver saldo | **Implementado** (carril 2A) |
| CU-17 | Bloquear saldo por orden de autoridad | **Implementado** (carril 2A) |
| CU-50 | Conciliar la custodia y verificar el encaje | **Implementado** (carril 2A) |
| CU-51 | Ejecutar el cierre diario | **Implementado** (carril 3A, mudado desde `aportes`) |
| CU-57 | Operar un punto de atención | **Obsoleto**, número reservado (ADR-039) |

**CU-40 vive acá y no en `cumplimiento`**, aunque planes/07 lo asigne al carril 1C:
su tabla central `consumo_limite` está en este esquema, y su propia descomposición
dice «se ejecuta dentro de la transacción de la operación» — que es esta.

**CU-57 no se implementa.** [[ADR-039]] retiró el efectivo del alcance el 20-08-2026;
el único ingreso de fondos es electrónico.

**CU-24 no tiene endpoint.** Lo invoca otro caso de uso —el que registra el hecho
económico— pasándole el `DSLContext` de **su** transacción, que es lo que hace que el
débito y su asiento confirmen juntos (invariante 12). Quien lo llame:

```java
// dentro del @Transactional + conContexto del organismo que mueve el dinero
cu24.ejecutar(dsl, new EntradaAsiento(OrigenAsiento.PAGO, pagoId, partidas, glosa), ctx);
```

Y para corregir, nunca editar: `cu24.reversar(dsl, asientoId, motivo, ctx)` crea el
asiento inverso enlazado por `asiento_reversa_id` (R-AUD-06).

## Eventos que emite

| Tema | Cuando |
| --- | --- |
| `aportaya.nucleo_financiero.asiento_registrado` | Se confirmó un asiento cuadrado (CU-24) |
| `aportaya.nucleo_financiero.asiento_reversado` | Se corrigió un asiento con su inverso (CU-24) |
| `aportaya.nucleo_financiero.aporte_aplicado` | Un `aportes.aporte_cobrado` movió el dinero: lleva `pagoId`, `obligacionId`, `transaccionId`, `asientoId`, `monto` y `moneda` (carril I), y desde 2026-10-16 `destino` (`GRUPO` o `FONDO_GARANTIA`) |

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| `aportaya.garantia.respaldo_reservado` · `respaldo_ampliado` · `respaldo_liberado` · `respaldo_recuperado` · `cobertura_empresarial_aplicada` | `garantia` | Registra en el mayor el `asiento` propuesto (CU-24), una sola vez por evento |
| `aportaya.garantia.cobertura_empresarial_reversada` | `garantia` | Reversa el asiento de la cobertura con un contra-asiento enlazado (nunca un `UPDATE`) |
| `aportaya.identidad.usuario_registrado` | `identidad` | Abre la billetera `USUARIO` de la persona (BOB, `SIMPLIFICADA`, con la politica vigente): una sola vez por evento y por titular (`AperturaDeBilleteras`, carril Q) |
| `aportaya.grupos.grupo_creado` | `grupos` | Abre la billetera `GRUPO` (BOB, titular el grupo, R-GRP-04): una sola vez por evento y por grupo (`AperturaDeBilleteras`, carril Q). El diseno lo llama `grupo_activado`, que `grupos` nunca emite |
| `aportaya.aportes.aporte_cobrado` | `aportes` | Con canal `BILLETERA_MOVIL`: debita la billetera del participante, acredita la del grupo (`APORTE_A_GRUPO`) y asienta DEBE `SALDO_USUARIOS` (2.1.01) / HABER `SALDO_GRUPOS` (2.1.02) en la MISMA transaccion (`ConsumoDeAportes`). Con `tipo = RECARGO_MORA` (2026-10-16) la contrapartida es la billetera de sistema `FONDO_GARANTIA` y la cuenta logica `RESERVA_RESPALDO`: el recargo no entra al pozo |

### Aporte cobrado (carril I)

`consumo/ConsumidorDeAportesKafka` → `aplicacion/ConsumoDeAportes`. **Encendido por omision** (`CONSUMO_DE_APORTES_HABILITADO`,
independiente del consumo de asientos de garantia). Garantias: una sola vez por evento (`evento_consumido`) y por pago (clave
`aporte-cobrado:<pagoId>` en `uq_tx_idem`); asiento y transaccion cuadrados, enlazados en las dos direcciones y sin `UPDATE`;
moneda del evento = moneda de las dos billeteras y de las dos cuentas del plan (en USD no hay cuenta en el plan sembrado: se
rechaza, no se inventa); saldo desde el LIBRO. Si falta una billetera (`AP-CU12-02`) o no esta operativa (`AP-CU12-03`) NO se
escribe nada y el evento NO se marca consumido: el reproceso lo aplica cuando la causa se resuelva. Si NO hay saldo (`AP-CU12-01`)
el rechazo es DEFINITIVO: no se escribe en el libro ni en el mayor, el evento SE marca consumido y se publica
`nucleo_financiero.aporte_no_aplicado` (pacto `contratos/pactos/aportes-nucleo-financiero.json`) para que `aportes` revierta el pago
(`CU21CompensarAporte`, idempotente por pago). Canales distintos de `BILLETERA_MOVIL` no salen de una billetera: se marcan consumidos
y se avisa en el registro.

### Apertura de billeteras (carril Q)

`consumo/ConsumidorDeUsuariosKafka` y `consumo/ConsumidorDeGruposKafka` → `aplicacion/AperturaDeBilleteras`. **Encendidos por omision**
(`APERTURA_USUARIOS_HABILITADO`, `APERTURA_GRUPOS_HABILITADO`). Antes NADIE abria estas cuentas: sin la del grupo el aporte cobrado se
rechazaba (`AP-CU12-02`) y `GET /billetera/cuentas/propia` daba `AP-CU13-01` a toda persona. Garantias: una sola vez por evento
(`evento_consumido`) y por titular (consulta previa + indice unico R-BIL-04 + `ON CONFLICT DO NOTHING`); una cuenta `CERRADA` no se
reabre; fuera de orden (el usuario o el grupo todavia no existe) la clave foranea rechaza y el reproceso la abre; sin politica de
billetera vigente no se abre. Emite `nucleo_financiero.billetera_abierta`. **Supuestos (decision delegada por el dueno, 2026-10-11;
confirman Cumplimiento y Producto):** solo BOB (el plan minimo no tiene cuentas en USD); la persona nace `SIMPLIFICADA` (CU-01) y la
cuenta `ACTIVA` (`APERTURA_ESTADO_INICIAL_USUARIO`, o `EN_APERTURA` para dejarla cerrada a toda operacion), porque quien impide
operar a una persona sin verificar es `identidad` (no inicia sesion hasta que su KYC se aprueba); `identidad` no emite hoy una senal
de «KYC aprobado», asi que `EN_APERTURA` no tiene quien la active. La cuenta contable espejo no se crea (el mayor se mueve por cuentas
logicas del plan).

### Cierre de billetera con deuda (carril Q)

`HechosPorHttp` ya no le pregunta a `aportes` por el `usuarioId` (aportes indexa por `participanteId` de grupos y contestaba siempre
«al dia»): con el token de la persona pide a `grupos` sus participaciones (`GET /grupos/participaciones`) y a `aportes` el estado de
cada una. Si la sesion no es la del titular (un operador, un trabajo programado), `grupos` o `aportes` no contestan, o el cuerpo es
ilegible, se asume que **si** tiene obligaciones (invariante 9). Riesgo residual: `grupos` solo devuelve participaciones vigentes
(no `RETIRADO`/`EXPULSADO`/`REEMPLAZADO`, hasta 50): la deuda de quien ya se fue de un grupo no frena el cierre.

### Venta de un turno (carril Q)

`pagarAlTercero` (CU-13/CU-130): el comprador paga el PRECIO (lo retenido), el vendedor cobra `precio - cargos` y la plataforma ingresa los
`cargos`, cada uno con su linea (regla 91.3.4). Precio y cargos los afirma `entregas` con la oferta (pacto
`contratos/pactos/nucleo-financiero-entregas.json`, version 2); sin cargos, o con un precio que no es lo retenido, o cargos negativos o
mayores que el precio (`AP-CU13-06`), no se paga. Decision adoptada por delegacion del dueno, 2026-10-11.

**Pendiente (declarado):** (1) los limites por concepto de CU-40 no se aplican al aporte (decision de Riesgos); (2) los codigos
2.1.01/2.1.02 los confirma Contabilidad; (3) recargas y retiros no asientan en el mayor, asi que 2.1.01 queda negativo hasta que se
decida; (4) la reversa de un aporte son dos operaciones separadas (contra-asiento de CU-24 y reverso de CU-14); (5) los canales
distintos de `BILLETERA_MOVIL` entran por la conciliacion y su propio asiento (no se inventa su contrapartida); (6) `AP-CU12-02`/`03`
(sin cuenta, cuenta bloqueada) se reintentan y, agotados los reintentos del contenedor de Kafka, el evento se descarta: falta la cola
de mensajes muertos con alerta (decision de Operaciones).

El consumidor (`consumo/ConsumidorDeAsientosKafka`) esta **apagado por omision**
(`CONSUMO_DE_ASIENTOS_HABILITADO=false`; el compose desplegado de TEST lo enciende): las cuentas son LOGICAS
(`CAJA_EMPRESA`, `RESERVA_RESPALDO`, `POZO_GRUPO`) y el codigo del plan de cuentas de cada una, por moneda, lo
define contabilidad (`CUENTA_LOGICA_<LOGICA>_<BOB|USD>` → `aportaya.contabilidad.cuentas-logicas.<LOGICA>.<MONEDA>`).
Sin mapeo cada evento se rechaza sin asentar nada. **TEST usa un mapeo SINTETICO** (arbol 9 de
`seeders/minimos/22-cuentas-de-sistema.json`, `apto_produccion=false`): con el perfil `production` y el consumo
encendido el proceso NO arranca mientras `CUENTAS_LOGICAS_APTO_PRODUCCION` no sea `true` (`GuardiaDeCuentasLogicas`).
Verificado con el nucleo y Postgres reales (`CU24EventosTest`, `CU24SemillasTest`); NO verificado contra un broker real.

## Operaciones de servicio a servicio

No son de usuario final: un usuario con `BILLETERA_OPERAR` recibe 403.

| Ruta | Identidad exigida | Lo pide | Que hace |
| --- | --- | --- | --- |
| `POST /billetera/inversiones/debitos` | `SERVICIO_INVERSIONES` | `inversiones` | Ejecuta la retencion `INVERSION_VOLUNTARIA` hacia la custodia de inversion |
| `POST /billetera/inversiones/acreditaciones` | `SERVICIO_INVERSIONES` | `inversiones` | Acredita un rescate desde la custodia al titular |
| `POST /billetera/retenciones/{id}/pago-a-tercero` | `SERVICIO_ENTREGAS` | `entregas` | Ejecuta la retencion `COMPRA_EN_MERCADO` a favor del vendedor de la oferta y los cargos |

`SERVICIO_*` son roles de sistema del catalogo (`seeders/minimos/10-roles-y-permisos.json`); aprovisionar la
credencial del servicio que los porta es de identidad. El pago a tercero no recibe beneficiario ni reparto:
los trae `entregas` (`GET /entregas/mercado/ofertas/{id}`, contrato PROPUESTO que `entregas` aun no publica).

## Trabajos programados

| Bloqueo | Cron | Que hace |
| --- | --- | --- |
| | | |

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:nucleo-financiero:generateJooq
./gradlew :servicios:nucleo-financiero:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.
