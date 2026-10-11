# Plan del carril C (pozo-mercado) — 2026-10-08

Sub-plan operativo del carril C dentro de `../../PLAN.md` (H7.S1.M2, H8, H9, H13.S1.M1). No reemplaza ni edita
el PLAN.md raiz: lo reduce a las microtareas que este carril toca, con su CA y DoD. El estado de cada una se
actualiza aca en el momento (regla 50). Propiedad de archivos: `servicios/{entregas,aportes,garantia,transparencia,auditoria}/**`
y `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-C/`. Esquema compartido: solo Edit en bloques minimos.

## Hechos del descubrimiento (con ruta)

- Mayor unico: `nucleo_financiero.asiento_contable/movimiento_contable`; solo nucleo-financiero lo escribe (invariante 12).
  `origen_tipo` ya admite `COBERTURA` y `ENTREGA` (sql/10_tablas/03_aportes_pagos_qr/asiento_contable.sql).
  Ningun servicio de este carril registra asientos hoy: CU21 (aportes) solo emite `aportes.aporte_cobrado`; no hay consumidor en nucleo.
- CU-22 (`servicios/entregas/.../CU22LiquidarEntrega.java`) deduce del principal y rechaza bolsa incompleta con `recaudado` que le manda el cliente.
- CU-23 (`servicios/garantia/.../CU23CubrirIncumplimiento.java`) es el fondo mutual (`fondo_garantia`, `politica_cobertura`, topes). NO se toca.
- No existe capacidad/reserva/cobertura empresarial en el modelo (grep `corporativ|empresarial|reserva` en docs/entidades: sin resultados).
- Mercado: permuta/traspaso viven en `grupos` (CU62/CU64, ajeno). No hay oferta/precio/cesion en ningun servicio.
- nucleo-financiero expone `retenerSaldo` y `cerrarRetencion(LIBERADA|EJECUTADA)` y `transferirSaldo`; no existe una operacion "ejecutar retencion a favor de un tercero".
- UNIQUE existentes: `uq_entrega_turno`, `uq_entrega_periodo` (R-GRP-01).

## Decisiones tecnicas (patron seguido en cada una)

- D1. Respaldo empresarial vive en `garantia` (modulo 08, dueño de coberturas). Patron: `fondo_garantia/movimiento_fondo/cobertura_incumplimiento`. Tablas nuevas, sin tocar las mutuales.
- D2. Orquestacion del pozo completo vive en `entregas` como caso nuevo `CU22EntregarPozoCompleto` (patron `RecargasConProveedor` de nucleo: consultas remotas FUERA de la transaccion, escritura local atomica con outbox). CU22LiquidarEntrega queda intacto (sus 15 pruebas se conservan).
- D3. El recaudo confirmado lo calcula `aportes` (dueño de obligaciones) y lo expone por HTTP; `entregas` no lo recibe del cliente.
- D4. Mercado (cesion del derecho de cobro) vive en `entregas` (dueño del beneficiario del pozo y de `entrega_fondo`); no cambia membresia (grupos) ni aportes. Prefijo de ruta `/entregas/mercado/*` (ya reservado). ADR formal: pendiente de quien gobierna docs/Arquitectura.
- D5. Los asientos NO se escriben aca: cada operacion emite un evento con `partidas` (cuenta logica, debe, haber) para que el consumidor de nucleo-financiero las registre via CU-24. Mientras ese consumidor no exista se prueba contra un doble del mayor (regla 65) que valida cuadre debe=haber, idempotencia por origen y reversa.
- D6. Todo importe: `Dinero` (BigDecimal escala 2) + moneda; ningun literal de dinero en main (barrido `sin-umbral-literal`).

## Ambiguedades / DECISION_REQUIRED (supuesto tomado — a quien confirmar)

| # | Pregunta | Supuesto implementado | Confirmar con |
|---|---|---|---|
| A1 | Tarifa/costo de la cobertura empresarial | Cero: no se cobra al grupo ni al miembro; sin tarifa inventada | Direccion comercial + tarifas (H11) |
| A2 | Tope de capacidad empresarial y quien lo fija | Parametro `monto_tope` en `capacidad_respaldo`, cargado por ADMIN_PLATAFORMA; sin semilla; sin fila => denegar por omision. En pruebas es SINTETICO | Direccion financiera / gerencia |
| A3 | Politica de recuperacion frente al miembro (interes, recargo, plazo, acreedor) | El aporte tardio recupera la reserva por el monto cubierto de esa obligacion; sin interes ni recargo nuevo; el excedente no es de la empresa | Comercial + asesoria legal |
| A4 | Cobertura parcial cuando la reserva no alcanza | Todo o nada: si no alcanza, incidente + deuda conservada, sin cobertura parcial | Direccion financiera |
| A5 | Cuentas contables de la reserva (caja empresa / reserva / cobertura) | Cuentas LOGICAS en la carga del evento; no se inventa plan de cuentas. Mapeo a `cuenta_contable` lo hace contabilidad | Contabilidad (ERP) |
| A6 | Que se descuenta del pozo | Principal integro: `bruto = pozo`, `neto = pozo`, sin deducciones sobre el principal; comision va por cobro separado (H11); adeudos del beneficiario se reclaman por recuperacion, no se descuentan | Producto + legal |
| A7 | Reventa de un derecho ya cedido | No permitida (una cesion viva por turno) | Producto + legal |
| A8 | Comprador debe ser participante activo del mismo grupo | Si (la admision humana sigue siendo autoridad: BO) | Producto + backoffice |
| A9 | Cargos de la oferta (quien los paga, cuanto) | Importe informado por tarifas; el vendedor los absorbe; no se calculan aca | Tarifas (H11) |
| A10 | Naturaleza juridica de la cesion y responsabilidad del cedente por aportes pendientes | El cedente conserva sus aportes (PLAN §1.2); el comprador solo recibe el cobro | Asesoria legal |

## Microtareas

Estados: TODO | EN CURSO | HECHO | A MEDIAS | BLOQUEADO | DESCARTADO. Se actualizan al final del archivo (§Registro).

### H7.S1.M2 — Reservar capacidad empresarial sin sobreasignar
CA: Dados dos grupos que activan a la vez con capacidad para uno solo, cuando ambos reservan, entonces uno obtiene la reserva identificada por ciclo y el otro recibe rechazo claro; `monto_comprometido <= monto_tope` siempre.
DoD: `./gradlew :servicios:garantia:integrationTest --tests '*Respaldo*'` con prueba de dos hilos + idempotencia (misma clave) + violacion de CHECK en la base.

### H8.S1.M2 — Calcular faltante al corte
CA: Dado pozo 6.000 y 5.000 confirmados, el corte solicita 1.000; los pendientes no cuentan como caja; se listan las obligaciones faltantes.
DoD: prueba de dominio + prueba contra Postgres de `aportes` (consulta de recaudo) + prueba de `entregas` con el puerto doblado en tres niveles.

### H8.S1.M3 — Aplicar cobertura empresarial contra la reserva
CA: Dada reserva financiada, cubrir mueve `monto_aplicado`, registra movimiento append-only con exposicion y responsable y emite partidas que cuadran; una sola cobertura por turno.
DoD: pruebas garantia (cobertura, reversa, idempotencia, concurrencia).

### H8.S1.M4 — Entregar el pozo completo una sola vez
CA: Con fondeo completo, la entrega nace con bruto = pozo, neto = pozo, un fondeo por turno; reintento devuelve lo mismo.
DoD: pruebas entregas contra Postgres (flujo, idempotencia, concurrencia, caida de garantia/aportes).

### H8.S1.M5 — Aporte tardio y recuperacion
CA: Dada cobertura previa, el pago tardio recupera la reserva una sola vez (UNIQUE por pago), sin segunda entrega ni segundo cobro.
DoD: pruebas garantia (recuperacion, doble pago, tope por linea).

### H8.S1.M6 — Falta extraordinaria de caja
CA: Dado respaldo insuficiente sobre un compromiso existente, se abre incidencia FONDO_INCOMPLETO, la entrega queda BLOQUEADA conservando la deuda (`monto_pendiente`) y el reintento posterior la completa sin duplicar.
DoD: pruebas entregas + evento de incidente en outbox.

### H9.S1.M1..M6 — Mercado del derecho al turno
M1 objeto contractual (cesion de derecho de cobro, no cambia deudor de aportes ni membresia); M2 crear/publicar oferta (precio, vigencia, validaciones); M3 listado filtrado por permisos; M4 reserva concurrente (uno gana); M5 saga de liquidacion (vendedor cobra, comprador recibe titulo una vez); M6 carreras vencimiento/cancelacion/compra.
DoD: pruebas entregas contra Postgres + dobles de tres niveles de `grupos` (titular, membresia) y `nucleo-financiero` (retener/liquidar/liberar).

### H13.S1.M1 — Conciliacion ledger-proveedor-banco (si queda margen)
CA: lote de tres fuentes con discrepancias clasificadas (causa, responsable, resolucion) sin fabricar abonos.

## Registro de estado

Cierre de la reanudacion (2026-10-08, tras el corte por RAM). Evidencia: `11-completa-c-reanudacion-1.log`,
`12-entregas-reanudacion-2.log`, `13-gates-reanudacion.log`. Peldaño por area: TESTED (suites completas de
`entregas`, `aportes` y `garantia` en verde contra PostgreSQL real; sin E2E de punta a punta ni regresion global).

| ID | Estado | Nota |
|---|---|---|
| H7.S1.M2 | HECHO | `CU131ConcurrenciaTest` (dos grupos, cinco rondas) + R-GAR-08 en base |
| H8.S1.M2 | HECHO | `CU132RecaudoTest` (aportes) + `CU131FaltanteDelCorteTest` + `CU133PozoCompletoTest` |
| H8.S1.M3 | HECHO | `CU131CoberturaTest` (aplicar, reversa, idempotencia) |
| H8.S1.M4 | HECHO | `CU133PozoCompletoTest` / `CU133PozoCompletoConcurrenciaTest` |
| H8.S1.M5 | HECHO | `CU131RecuperacionTest` |
| H8.S1.M6 | HECHO | `CU133PozoCompletoTest` (BLOQUEADA + deuda + incidencia + reintento) |
| Hallazgo MEDIO cubrirConRespaldo | HECHO | el cuerpo solo lleva ids; pozo/confirmado/lineas salen de `aportes` |
| H9.S1.M1..M6 | HECHO (contra dobles de grupos y nucleo) | `CU130*Test`; integracion real con nucleo pendiente de su operacion `pagarAlVendedor` |
| H13.S1.M1 | HECHO | `CU132ConciliacionTest` (sin fabricar abonos, idempotente) |

Correcciones de la reanudacion: `MercadoWebTest` (el numero JSON `5500.00` se coacciona a cadena y pasa el patron:
ahora se prueba con `5500.0`, que si rompe el patron) y `CU130CompraTest.nucleoCaidoAlPagar` (`fallaAlPagar(2)`
consumia las dos primeras llamadas, asi que la segunda no podia liquidar; la intencion era una sola caida: `fallaAlPagar(1)`).
Ambos eran TEST_BUG; el codigo de produccion no cambio.
