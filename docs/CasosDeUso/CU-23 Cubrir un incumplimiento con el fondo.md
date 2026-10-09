---
tags:
  - caso-uso
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-23
criticidad: alta
actores: [Sistema, Comité del grupo]
normas: [Contabilidad, debido proceso, ASFI riesgo]
---

# CU-23 — Cubrir un incumplimiento con el fondo de garantía

> **Objetivo.** Que el grupo no se detenga cuando alguien no aporta, y que esa
> cobertura **no sea un regalo**: genera deuda exigible, subrogación y expediente.

## Actores y disparador

- **Actor principal:** el sistema, al detectar bolsa incompleta antes de una entrega.
- **Actor secundario:** comité o acuerdo del grupo, cuando la política lo exige.

## Precondiciones

1. Existe [[obligacion_aporte]] vencida y sin pagar, con plazo de gracia superado.
2. El [[fondo_garantia]] tiene saldo suficiente y la [[politica_cobertura]] lo permite.
3. Existe [[registro_incumplimiento]] abierto con su evidencia.

## Flujo principal

1. Se evalúa la política: monto máximo por evento, tope por participante, número de
   coberturas previas.
2. **En la misma transacción**:
   - se crea [[cobertura_incumplimiento]] por el importe faltante;
   - se crea [[movimiento_fondo]] (append-only) que debita el fondo;
   - se crea la [[transaccion_billetera]] `tipo='COBERTURA_GARANTIA'`: débito a la
     cuenta del fondo, crédito a la cuenta del grupo;
   - se crea [[deuda_participante]] por el mismo importe, con su
     [[subrogacion]] a favor del fondo;
   - se registra el [[asiento_contable]].
3. La entrega del turno continúa normalmente ([[CU-22 Liquidar y entregar el fondo]]).
4. Se inicia la [[gestion_cobranza]] según la [[estrategia_cobranza]] vigente.
5. Cuando el deudor cobre su propio turno, la deuda se descuenta como
   `REPOSICION_FONDO_GARANTIA` en su entrega.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El fondo no alcanza | No hay cobertura: la entrega queda `BLOQUEADA_POR_FONDO_INCOMPLETO` y se activa el [[plan_contingencia]] |
| 1b | La política exige acuerdo del grupo | Se crea [[acuerdo]] y se vota antes de cubrir |
| 2a | El participante presenta descargo | [[descargo_participante]] dentro del plazo; la sanción no queda firme hasta resolverlo |
| 5a | El deudor abandona el grupo | La deuda sigue viva: [[abono_recuperacion]], [[acuerdo_quita]] o [[castigo_deuda]] con autorización |

## Postcondiciones

- La bolsa quedó completa sin que nadie pusiera plata de su bolsillo fuera del fondo.
- Existe deuda exigible con expediente, no una pérdida silenciosa.

## Contrato · `openapi/garantia.yaml`

```ts
export const EntradaCU23 = z.object({
  claveIdempotencia: z.string().uuid(),
  obligacionId: z.string().uuid(),
  acuerdoId: z.string().uuid().optional(),
}).strict()

export const SalidaCU23 = z.object({
  coberturaId: z.string().uuid(),
  montoCubierto: MontoSchema,
  deudaId: z.string().uuid(),
  saldoFondoDespues: MontoSchema,
}).strict()

export const ErroresCU23 = {
  FONDO_INSUFICIENTE: 'AP-CU23-01',
  TOPE_POR_PARTICIPANTE: 'AP-CU23-02',
  REQUIERE_ACUERDO: 'AP-CU23-03',
  OBLIGACION_NO_VENCIDA: 'AP-CU23-04',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `FONDO_INSUFICIENTE` | El fondo no alcanza para cubrir |
| `TOPE_POR_PARTICIPANTE` | Superó su tope de coberturas |
| `REQUIERE_ACUERDO` | El monto exige aprobación del grupo |
| `OBLIGACION_NO_VENCIDA` | Todavía está en plazo de gracia |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `evaluarPoliticaCobertura` | Aplica topes y devuelve cuánto se puede cubrir; puro |
| Molécula | `FondoGarantiaRepositorio` | Movimiento del fondo, append-only |
| Molécula | `DeudaRepositorio` | Deuda del participante y su subrogación |
| Organismo | `CU23CubrirIncumplimiento` | Transacción: cobertura, deuda, subrogación y asiento |
| Página | `POST /fondo/coberturas` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `incumplimiento.cubierto` | Gestión de cobranza y evento de reputación | `GRUPO_ADMINISTRAR` |
| `fondo.consumido` | Aviso al grupo del saldo restante del fondo | — |

## Interfaz

- **App:** El grupo ve que la bolsa se completó con el fondo y quién la debe reponer.
- **Backoffice:** Panel del fondo por grupo: saldo, cubierto, recuperado y en gestión.

## Restricciones aplicables

`R-BIL-01` · `R-AUD-01` · `R-AUD-05` · `R-GRP-02` · `R-GAR-08` · `R-GAR-09` ·
`R-GAR-10` · `R-GAR-11` · `R-GAR-12`

Las cinco últimas las hace cumplir la base en el camino del **respaldo empresarial** del
servicio `garantia` (`CU23ReservarRespaldo` y `CU23CubrirConRespaldo`: la empresa aparta
por ciclo una reserva de una capacidad finita y la usa para cubrir lo que no llegó al
corte del pozo), no en la cobertura con el fondo mutual descrita arriba:

- `R-GAR-08` · la capacidad empresarial comprometida **nunca supera su tope**. Dos grupos
  que activan a la vez compiten por la misma fila de capacidad: se bloquea primero y la
  base lo vuelve a exigir. Sin capacidad cargada se deniega; el tope es un dato.
- `R-GAR-09` · la reserva se mueve solo con **movimientos inmutables** y nunca se usa más
  de lo reservado (aplicado + liberado ≤ reservado; recuperado ≤ aplicado). Los
  contadores de la reserva y de la capacidad son caché del libro de movimientos que
  mantiene la base, y una reserva liberada no admite ampliación ni aplicación.
- `R-GAR-10` · **una cobertura viva por turno** (la reversada deja de contar) y su
  faltante cuadra con el pozo: faltante = pozo − confirmado − cubierto por el fondo mutual.
- `R-GAR-11` · las líneas de una cobertura **suman exactamente su faltante** (se verifica
  al confirmar) y cada línea recupera a lo sumo lo que cubrió: nadie se recupera dos
  veces por el mismo pago.
- `R-GAR-12` · el turno de una cobertura **es del grupo y del periodo que declara**: la
  base lo comprueba con una consulta de alcance mínimo a `grupos` y rechaza un turno de
  otro grupo.

Esos caminos devuelven además `AP-CU23-05`, `AP-CU23-06`, `AP-CU23-07`, `AP-CU23-09`,
`AP-CU23-11`, `AP-CU23-12` y `AP-CU23-13`, declarados en `openapi/garantia.yaml`; el
contrato de arriba todavía no los lista.

## Evidencia que deja

[[registro_incumplimiento]] · [[cobertura_incumplimiento]] · [[movimiento_fondo]] ·
[[deuda_participante]] · [[subrogacion]] · [[transaccion_billetera]] ·
[[asiento_contable]] · [[gestion_cobranza]] ·
[[capacidad_respaldo]] · [[reserva_respaldo]] · [[movimiento_reserva]] · [[cobertura_respaldo]] · [[cobertura_respaldo_linea]] · [[recuperacion_respaldo]]

## Criterios de aceptación

```gherkin
Dada una obligación vencida de Bs 500 y fondo con saldo suficiente
Cuando se ejecuta la cobertura
Entonces existe cobertura_incumplimiento por 500
Y existe deuda_participante por 500 con subrogación al fondo
Y la cuenta del grupo aumentó 500

Dado un fondo sin saldo suficiente
Cuando se evalúa la cobertura
Entonces la entrega queda BLOQUEADA_POR_FONDO_INCOMPLETO

Dado un deudor que cobra su turno
Cuando se liquida su entrega
Entonces existe una deducción REPOSICION_FONDO_GARANTIA por el saldo de su deuda
```

## Ver también

[[CU-22 Liquidar y entregar el fondo]] · [[CU-25 Declarar el incumplimiento con descargo y evidencia]] · [[CU-26 Ejecutar el aval y subrogar la deuda]] · [[CU-29 Devolver los aportes del fondo de garantía]] · [[CU-54 Registrar un evento de riesgo operativo]] · [[CU-66 Reemplazar a un participante moroso]]
