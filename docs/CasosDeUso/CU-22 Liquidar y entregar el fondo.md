---
tags:
  - caso-uso
  - modulo/04-entregas-de-fondo
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
codigo: CU-22
criticidad: alta
actores: [Sistema, Organizador, Beneficiario]
normas: [ASFI transparencia, SIN facturación, contabilidad]
---

# CU-22 — Liquidar y entregar el fondo

> **Objetivo.** Que el pozo llegue **completo**, a la persona correcta y **una sola vez**, sin que ninguna persona
> decida el importe: el pozo lo afirma `aportes` y el principal no se descuenta.

> **Vigencia ([[ADR-055 Sin aprobaciones en el circuito|ADR-055]] E8 y [[ADR-056 Tu garantía vuelve y la puntualidad vale plata|ADR-056]], 2026-11-21).**
> La entrega nace **solo** del fondeo del pozo completo (`CU22EntregarPozoCompleto`). Se retiraron la liquidación
> con bruto, recaudado y deducciones que mandaba el cliente (F1.L2.M09) y el cierre manual con un monto libre
> (F1.L2.M08): `POST /entregas` y `POST /entregas/{id}/ejecucion` responden **410**. La comisión no sale del pozo:
> se cobra en el mismo QR de la cuota como línea aparte (DECISIONES, tanda 7). No hay comisión del organizador ni
> pasa dinero por su cuenta (RN-18, regla 91.5).
>
> **Transitorio.** Hasta que el fondeo y la emisión los dispare el sistema (F3.L2.M01 y F3.L2.M02), la autorización
> y la orden de desembolso ([[CU-28 Emitir la orden de desembolso y ejecutar el intento]]) siguen siendo actos de
> Tesorería; F2.L2.M06 los retira junto con la segregación de la entrega (`ck_entrega_segregacion`).

## Actores y disparador

- **Actor principal:** el sistema.
- **Disparador:** el turno del período tiene su pozo cobrado (hoy, el pedido de fondeo; en F3, el evento del turno
  cobrado).

## Precondiciones

1. Existe [[turno]] del período sin entrega previa (`R-GRP-01`).
2. `aportes` responde lo confirmado del período; un pendiente no es caja.
3. El beneficiario tiene cuenta verificada y fuera de enfriamiento al momento del desembolso (CU-28).

## Flujo principal

1. Se pregunta a `aportes` el pozo, lo confirmado y lo cubierto del período, fuera de la transacción.
2. En una transacción local se crea [[entrega_fondo]] con el **pozo completo** como principal y sin deducciones, y
   su [[fondeo_entrega]], que cuadra al centavo (`R-DES-03`).
3. Si el pozo está completo, la entrega queda `PROGRAMADA`; si falta caja, queda `BLOQUEADA_POR_FONDO_INCOMPLETO`
   con la deuda conservada y una incidencia, a la espera del fondo del grupo (modelo A, F3.L2.M05).
4. El desembolso a la cuenta verificada lo emite y lo acredita [[CU-28 Emitir la orden de desembolso y ejecutar el intento]];
   la entrega queda `ENTREGADA` solo cuando el desembolso se acredita.

## Flujos alternativos

| # | Situación | Comportamiento |
| --- | --- | --- |
| 1a | `aportes` no responde | No se escribe nada (`AP-CU22-06`); el reintento es seguro |
| 1b | El período no es del grupo | Se rechaza (`AP-CU22-07`) |
| 2a | Se repite el fondeo con la misma clave | Devuelve la misma entrega y el mismo fondeo |
| 2b | El turno ya tiene una entrega anterior al fondeo | Se rechaza (`AP-CU22-09`); la base además impide una segunda entrega (`R-GRP-01`) |

## Postcondiciones

- Una sola [[entrega_fondo]] por turno, con el pozo completo como principal.
- Un [[fondeo_entrega]] que cuadra, y la deuda conservada si faltó caja.

## Contrato · `openapi/entregas.yaml`

```ts
export const EntradaCU22 = z.object({
  claveIdempotencia: z.string().uuid(),
  grupoId: z.string().uuid(),
  periodoId: z.string().uuid(),
  turnoId: z.string().uuid(),
  cupoId: z.string().uuid(),
  beneficiarioId: z.string().uuid(),
  metodoDesembolso: z.enum(['BILLETERA_MOVIL', 'COMPENSACION_INTERNA', 'QR_ENVIO', 'TRANSFERENCIA_BANCARIA']),
  fechaProgramada: z.string().date(),
}).strict()

export const SalidaCU22 = z.object({
  entregaId: z.string().uuid(),
  fondeoId: z.string().uuid(),
  estado: z.enum(['FONDEADO', 'CON_PENDIENTE']),
  pozo: MontoSchema,
  confirmado: MontoSchema,
  pendiente: MontoSchema,
  esNuevo: z.boolean(),
}).strict()

export const ErroresCU22 = {
  BOLSA_INCOMPLETA: 'AP-CU22-01',        // retirado con la liquidacion vieja
  VALIDACION_BLOQUEANTE: 'AP-CU22-02',
  ENTREGA_DUPLICADA: 'AP-CU22-03',
  SEGREGACION_INCUMPLIDA: 'AP-CU22-04',  // retirado con la ejecucion manual
  NETO_NEGATIVO: 'AP-CU22-05',           // retirado: el principal no se descuenta
  RECAUDO_NO_DISPONIBLE: 'AP-CU22-06',
  PEDIDO_NO_COINCIDE: 'AP-CU22-07',
  RESPALDO_NO_DISPONIBLE: 'AP-CU22-08',  // se retira con el respaldo empresarial (F2.L2.M01)
  FONDEO_EN_CONFLICTO: 'AP-CU22-09',
  LIQUIDACION_RETIRADA: 'AP-CU22-10',
  EJECUCION_RETIRADA: 'AP-CU22-11',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `BOLSA_INCOMPLETA` | Retirado: era de la liquidación con importes del pedido |
| `VALIDACION_BLOQUEANTE` | Al autorizar, una regla de entrega bloqueante sigue sin resolver |
| `ENTREGA_DUPLICADA` | La entrega no existe o ya no admite la autorización |
| `SEGREGACION_INCUMPLIDA` | Retirado: era de la ejecución manual |
| `NETO_NEGATIVO` | Retirado: el principal no se descuenta |
| `RECAUDO_NO_DISPONIBLE` | `aportes` no responde: no se fondea nada |
| `PEDIDO_NO_COINCIDE` | El período no es del grupo |
| `RESPALDO_NO_DISPONIBLE` | `garantia` no responde al pedir el respaldo; se retira con el respaldo empresarial (F2.L2.M01) |
| `FONDEO_EN_CONFLICTO` | Otro pedido movió el fondeo, o el turno ya tiene una entrega anterior |
| `LIQUIDACION_RETIRADA` | `POST /entregas` (410): la liquidación con importes del pedido ya no existe |
| `EJECUCION_RETIRADA` | `POST /entregas/{id}/ejecucion` (410): la entrega no se cierra a mano |

Un código retirado queda retirado: no se reusa.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `FondeoDelPozo` | Cuánto es caja confirmada, cuánto falta y cuánto queda pendiente; puro |
| Molécula | `RecaudoPorHttp` | Pregunta a `aportes`, con timeout y circuito |
| Molécula | `FondeoRepositorio`, `EntregaRepositorio` | Alta del fondeo y de la entrega, transiciones con versión |
| Organismo | `RegistroDelFondeo` | La transacción local: entrega + fondeo + eventos por outbox |
| Organismo | `CU22EntregarPozoCompleto` | Pregunta afuera, escribe adentro |
| Página | `POST /entregas/pozo-completo` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `entregas.pozo_fondeado` | La orden de desembolso (hoy manual, F3: automática) | Hoy `ENTREGA_EJECUTAR` (transitorio) |
| `entregas.entrega_autorizada` | Habilita la orden de desembolso (transitorio, F2.L2.M06) | `ENTREGA_AUTORIZAR` |

## Interfaz

- **App:** *Cobrar mi turno*: el pozo completo, la cuenta de destino y el estado del desembolso.

## Restricciones aplicables

`R-GRP-01` · `R-DES-03` · `R-DES-04` · `R-DES-05`

- `R-GRP-01` · **una entrega por turno**: dos entregas del mismo turno es pagar dos veces el mismo premio.
- `R-DES-03` · el fondeo del pozo **cuadra al centavo**: confirmado + cubierto + faltante = pozo, y el pendiente es
  la deuda conservada.
- `R-DES-04` y `R-DES-05` · una oferta y una cesión vivas por turno. El mercado de turnos se retiró (D-L2-1): las
  tablas y sus restricciones siguen hasta la contracción del esquema, para las cesiones ya liquidadas.

## Evidencia que deja

[[entrega_fondo]] · [[fondeo_entrega]] · [[incidencia_entrega]] · [[historial_estado_entrega]] · el evento por outbox

Tablas del módulo que este caso conserva sin escribirlas en el flujo nuevo: [[validacion_pre_entrega]] y
[[regla_entrega]] (las lee la autorización transitoria), [[confirmacion_recepcion]] (la reemplaza la acreditación
del desembolso), y [[oferta_turno]] y [[cesion_derecho]] (mercado retirado, D-L2-1; se contraen después).

## Criterios de aceptación

```gherkin
Dado un pozo de Bs 6.000 con todos los aportes confirmados
Cuando se fondea el turno
Entonces nace una sola entrega PROGRAMADA con el pozo completo como principal y sin deducciones

Dado un turno ya fondeado
Cuando se repite el fondeo
Entonces devuelve la MISMA entrega y no crea otra
Y la base sigue rechazando una segunda entrega del turno (R-GRP-01)

Dado un pozo de Bs 6.000 con Bs 5.000 confirmados y nadie que cubra el faltante
Cuando se fondea el turno
Entonces la entrega queda BLOQUEADA con el pozo completo como principal y Bs 1.000 de deuda conservada
```

## Ver también

[[CU-13 Retener y liberar saldo]] · [[CU-18 Registrar y verificar una cuenta bancaria de destino]] · [[CU-20 Crear grupo y congelar tarifario]] · [[CU-21 Cobrar el aporte del período]] · [[CU-23 Cubrir un incumplimiento con el fondo]] · [[CU-28 Emitir la orden de desembolso y ejecutar el intento]] · [[CU-31 Devengar y cobrar la comisión]] · [[CU-32 Emitir factura electrónica]] · [[CU-62 Permutar turnos entre participantes]]
