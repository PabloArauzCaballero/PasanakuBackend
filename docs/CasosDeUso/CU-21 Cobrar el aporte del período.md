---
tags:
  - caso-uso
  - modulo/03-aportes-pagos-qr-y-conciliacion
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-21
criticidad: alta
actores: [Participante, Sistema]
normas: [ASFI conciliación y contabilidad, UIF]
---

# CU-21 — Cobrar el aporte del período

> **Objetivo.** Que "pagué" signifique "el dinero está en la cuenta del grupo",
> nunca una declaración del usuario, y que un reintento no cobre dos veces.

## Actores y disparador

- **Actor principal:** participante.
- **Disparadores:** apertura del período; recordatorio; débito programado
  autorizado.

## Precondiciones

1. Existe [[periodo]] abierto y [[obligacion_aporte]] generada por cupo
   (`R-GRP-03`: una obligación de tipo `APORTE_PERIODICO` por período y cupo).
2. La cuenta del participante está operativa.

## Flujo principal

1. Al abrir el período se generan las [[obligacion_aporte]] con `monto_esperado`,
   `fecha_vencimiento` y `fecha_fin_gracia` según [[politica_mora]] (H8.S1.M1): **una por cupo**
   (`R-GRP-03`), con su titular (`participante_id`), el turno (número de período) y el estado
   `PENDIENTE`. Quién es titular de cada cupo y cuánto aporta lo afirma `grupos`, no el cliente.
   La fecha **contractual** es la del reglamento; `fecha_vencimiento` guarda la de **liquidación**:
   si la contractual cae en feriado o fin de semana se corre al siguiente día hábil ([[CU-59 Mantener el calendario de días no hábiles]]), el fin de gracia conserva su largo, y se avisa al grupo.
   Generar dos veces —o a la vez— no duplica nada y no emite un segundo evento.
   El hecho lo publica `grupos`: `GET /grupos/periodos/{periodoId}/calendario-de-aportes` (titulares de los cupos
   OCUPADOS, importe de cada uno, turno y fechas del reglamento; permiso `ADMIN_PLATAFORMA`, cuyo token viaja en la
   llamada; un periodo inexistente responde `AP-CU21-08`). `aportes` lo consume con tiempo máximo y *circuit breaker*, y
   si no contesta rechaza (flujo alternativo 1a).
2. El participante elige medio de pago:
   - **saldo** → [[CU-12 Transferir saldo entre billeteras]] hacia la cuenta del grupo;
   - **QR / pasarela** → se emite [[orden_cobro]] con `clave_idempotencia` y
     [[qr_cobro]]; el pago llega como [[pago]] y se concilia ([[conciliacion]]).
3. **En la misma transacción** que acredita:
   - se actualiza `obligacion_aporte.monto_pagado` y su `estado`;
   - se crea la [[transaccion_billetera]] con crédito a la cuenta del grupo;
   - se registra el [[asiento_contable]].
4. Si hay débito programado, la [[retencion_saldo]] creada el día de la
   autorización se ejecuta en la fecha ([[CU-13 Retener y liberar saldo]]).
5. Se evalúan umbrales UIF sobre la operación.
6. Si vence sin pagar, se aplica recargo como **obligación nueva** que apunta a la
   original (`obligacion_origen_id`), nunca modificando el monto original.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | Webhook duplicado | Idempotencia: un solo pago acreditado (`R-BIL-06`) |
| 2b | Pago por monto distinto | Se acredita como parcial (`PAGADO_PARCIAL`) o genera excepción de conciliación |
| 1a | `grupos` no contesta, contesta por otro periodo o informa cupos o importes inutilizables | Rechazo `PERIODO_NO_CONFIRMADO`: no se genera nada |
| 1b | No hay calendario de feriados cargado para el año del vencimiento | Rechazo `CALENDARIO_NO_DISPONIBLE`: no se calcula, no se cuenta corrido |
| 1c | El período no está `ABIERTO` | Rechazo `PERIODO_CERRADO` |
| 3b | El pago salió de la billetera y `nucleo-financiero` no pudo aplicarlo por falta de saldo (`AP-CU12-01`) | `nucleo-financiero` no mueve nada, marca el evento consumido y publica `aporte_no_aplicado`; `aportes` lo consume, el pago pasa a `REVERSADO` y la obligación vuelve a `PENDIENTE` (o `EN_MORA` si ya venció) por ese importe. Una vez por pago, aunque el aviso se repita. Un pago con reembolsos o en disputa no se compensa solo: lo resuelve una persona |
| 3a | El pago no concilia con el extracto bancario | [[excepcion_conciliacion]] abierta; **bloquea el cierre diario** (`R-BIL-12`) |
| 6a | Vence el plazo de gracia | Se genera recargo y, superado el umbral, [[registro_incumplimiento]] |
| — | El participante paga de más | El excedente queda como saldo a favor, no se "pierde" |

## Postcondiciones

- La obligación refleja exactamente lo pagado, lo condonado y lo cubierto.
- La bolsa del grupo creció en el importe acreditado, con asiento espejo.

## Contrato · `openapi/aportes.yaml`

```ts
export const EntradaCU21 = z.object({
  claveIdempotencia: z.string().uuid(),
  obligacionId: z.string().uuid(),
  monto: MontoSchema,
  medio: z.enum(['SALDO','QR','TRANSFERENCIA']),
}).strict()

export const SalidaCU21 = z.object({
  pagoId: z.string().uuid().nullable(),
  ordenCobroId: z.string().uuid().nullable(),
  estado: z.enum(['PENDIENTE','PAGADO_PARCIAL','PAGADO','EN_VERIFICACION']),
  saldoPendiente: MontoSchema,
  qr: z.object({ payloadEmv: z.string() }).nullable(),
}).strict()

export const SalidaGenerarCU21 = z.object({         // POST /aportes/periodos/{periodoId}/obligaciones
  periodoId: z.string().uuid(), grupoId: z.string().uuid(), turno: z.number().int().min(1),
  fechaContractual: z.string().date(), fechaDeLiquidacion: z.string().date(),
  corridaPorNoHabil: z.boolean(), creadas: z.number().int().min(0),
  diasSalteados: z.array(z.object({ fecha: z.string().date(), motivo: z.string() })),
  obligaciones: z.array(z.object({
    obligacionId: z.string().uuid(), cupoId: z.string().uuid(), titularParticipanteId: z.string().uuid(),
    turno: z.number().int(), monto: MontoSchema, estado: z.string(),
    fechaContractual: z.string().date(), fechaDeLiquidacion: z.string().date(),
    finDeGracia: z.string().date(), esNueva: z.boolean(),
  })),
}).strict()

export const ErroresCU21 = {
  OBLIGACION_NO_VIGENTE: 'AP-CU21-01',
  MONTO_NO_COINCIDE: 'AP-CU21-02',
  SALDO_INSUFICIENTE: 'AP-CU21-03',
  PERIODO_CERRADO: 'AP-CU21-04',
  RECAUDO_INCONSISTENTE: 'AP-CU21-05',
  OBLIGACION_AJENA: 'AP-CU21-06',
  NO_VERIFICABLE: 'AP-CU21-07',
  PERIODO_NO_CONFIRMADO: 'AP-CU21-08',
  CALENDARIO_NO_DISPONIBLE: 'AP-CU21-09',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `OBLIGACION_NO_VIGENTE` | Está anulada, condonada o ya saldada |
| `MONTO_NO_COINCIDE` | No coincide con lo esperado y el grupo no acepta parcial |
| `SALDO_INSUFICIENTE` | Paga con saldo y no le alcanza |
| `PERIODO_CERRADO` | El período ya se liquidó, o no está abierto al generar las obligaciones |
| `RECAUDO_INCONSISTENTE` | El período no tiene obligaciones, o mezcla monedas, al medir el recaudo |
| `OBLIGACION_AJENA` | La obligación no es de quien paga |
| `NO_VERIFICABLE` | `grupos` no contesta al verificar la obligación: se deniega |
| `PERIODO_NO_CONFIRMADO` | `grupos` no confirma el período o sus cupos al generar las obligaciones |
| `CALENDARIO_NO_DISPONIBLE` | No hay calendario de feriados cargado para el año del vencimiento |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `aplicarPagoAObligacion` | Distribuye el pago entre capital y recargo; puro |
| Átomo | `clasificarMora` | Días de atraso y su severidad según política |
| Molécula | `ObligacionRepositorio` | Toma la obligación para actualizar, con bloqueo |
| Molécula | `OrdenCobroRepositorio` | Emite la orden y su QR conciliable |
| Molécula | `ConciliacionRepositorio` | Cruce contra el extracto bancario |
| Organismo | `CU21CobrarAporte` | Transacción: pago, obligación, movimientos y asiento |
| Página | `POST /aportes` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `aporte.confirmado` | Reputación, tablero del grupo y cancelación de recordatorios | `BILLETERA_OPERAR` |
| `aporte.vencido` | Recargo y, superado el umbral, incumplimiento | — |
| `aporte_compensado` | Aviso a la persona de que su aporte no se aplicó por falta de saldo | — (consume `nucleo_financiero.aporte_no_aplicado`) |

> **Cómo se mueve el dinero (carril I).** `aportes` no escribe el libro (invariante 12): al acreditar emite
> `aportes.aporte_cobrado` (`pagoId`, `obligacionId`, `grupoId`, `participanteId`, `usuarioId`, `monto`, `moneda`, `canal`) por outbox y
> `nucleo-financiero` lo CONSUME (`ConsumoDeAportes`). Con canal `BILLETERA_MOVIL` debita la billetera del participante, acredita la
> del grupo (`APORTE_A_GRUPO`) y asienta DEBE 2.1.01 / HABER 2.1.02 en la misma transacción; el consumidor es idempotente por evento y
> por pago, y un rechazo (sin cuenta, sin saldo, moneda sin cuenta en el plan) no escribe nada ni marca el evento. Con saldo insuficiente (`AP-CU12-01`) el rechazo es
> DEFINITIVO: nucleo publica `nucleo_financiero.aporte_no_aplicado` y `aportes` compensa el pago (flujo alternativo 3b, `CU21CompensarAporte`).
> Siguen pendientes, sin inventarse: los canales distintos de `BILLETERA_MOVIL` (entran por la conciliación y su propio asiento) y los
> límites por concepto de [[CU-40 Evaluar límites antes de una operación]] sobre el aporte (decisión de Riesgos).

## Interfaz

- **App:** *Mi aporte*: monto, fecha límite y un botón; con saldo es instantáneo.
- **Backoffice:** Estado de cobranza por grupo y período, con la brecha para completar la bolsa.

## Restricciones aplicables

`R-GRP-03` · `R-BIL-01` · `R-BIL-06` · `R-AUD-01` · `R-AUD-05` · `R-UIF-02`

## Evidencia que deja

[[obligacion_aporte]] · [[orden_cobro]] · [[pago]] · [[conciliacion]] ·
[[transaccion_billetera]] · [[asiento_contable]]

## Criterios de aceptación

```gherkin
Dado un aporte de Bs 500 pagado con saldo que nucleo-financiero no pudo aplicar por falta de saldo
Cuando aportes consume el aviso aporte_no_aplicado
Entonces el pago queda REVERSADO y la obligación vuelve a PENDIENTE con Bs 0 pagados

Dada una obligación de Bs 500 pendiente
Cuando el participante paga con saldo
Entonces monto_pagado es 500 y estado es PAGADO
Y la cuenta del grupo aumentó Bs 500

Dadas dos obligaciones con la misma clave de idempotencia
Cuando se acredita cada pago
Entonces cada obligación recibe un pago distinto

Dado un pago no conciliado con el extracto
Cuando se intenta cerrar el día
Entonces el cierre_diario no puede marcarse cuadrado

Dado un aporte vencido con política de mora
Cuando corre el proceso diario
Entonces se crea una obligación de tipo RECARGO_MORA con obligacion_origen_id

Dado un período abierto con tres cupos ocupados y un aporte de Bs 500
Cuando se generan las obligaciones del período
Entonces existe una obligación APORTE_PERIODICO por cupo con su titular, el turno, Bs 500 y estado PENDIENTE
Y el calendario las enumera sin duplicar

Dado un período cuyo vencimiento contractual cae en feriado
Cuando se generan las obligaciones del período
Entonces fecha_vencimiento es el siguiente día hábil y la fecha contractual queda explícita
Y el evento avisa al grupo del corrimiento

Dadas las obligaciones de un período ya generadas
Cuando se generan otra vez, también a la vez
Entonces no se crea ninguna obligación nueva ni un segundo evento

Dado un pago a las 21:00 de La Paz del último día de gracia, que en UTC ya es el día siguiente
Cuando se acredita el pago parcial
Entonces la obligación no está vencida
Y a la medianoche de La Paz sí lo está

Dadas las obligaciones generadas de un período con pagos acreditados y un extracto del banco
Cuando se concilia el lote
Entonces lo conciliado coincide con el recaudo confirmado del período
Y las diferencias quedan con su causa y su responsable, sin crear ni mover ningún pago
```

## Ver también

[[CU-12 Transferir saldo entre billeteras]] · [[CU-22 Liquidar y entregar el fondo]] · [[CU-51 Ejecutar el cierre diario]] · [[CU-59 Mantener el calendario de días no hábiles]] · [[CU-81 Programar recordatorios de aporte]] · [[CU-82 Procesar una respuesta entrante]] · [[CU-99 Dar de alta un proveedor de pago y enrutar el cobro]]
