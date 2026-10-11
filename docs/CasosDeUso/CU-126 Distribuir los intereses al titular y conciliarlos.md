---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-126
criticidad: alta
actores: [Sistema, Usuario, Aliado]
normas: [Trazabilidad del dinero, Conciliación]
---

# CU-126 — Distribuir los intereses al titular y conciliarlos

> **Objetivo.** El interés que paga el emisor de un DPF se reparte a **un titular** (el de la
> posición), por **un período** (de la constitución al corte) y con **su retención** (la del
> comprobante), y se concilia contra el **devengo propio**. Si no coincide no se corrige en
> silencio: queda una discrepancia con su motivo, a la vista.

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Ver [[CU-120 Consultar productos de inversión y sus condiciones]]. La retención y la
> penalización son las de las condiciones sintéticas aceptadas; no son un criterio fiscal
> (`DR-INV-03`).

## Actores y disparador

- **Actor principal:** el sistema, dentro de la liquidación de [[CU-125 Liquidar el rescate y acreditar al titular]].
  El titular consulta lo repartido con `GET /inversiones/posiciones/{posicionId}/distribuciones`
  (`BILLETERA_VER`).
- **Disparador:** un DPF queda liquidado (vencimiento o cancelación anticipada).

## Precondiciones

1. El aliado confirmó la liquidación y el interés y la retención que informa **coinciden** con el
   cálculo propio (si no, [[CU-125 Liquidar el rescate y acreditar al titular]] corta con `AP-CU125-01` y no hay nada
   que repartir).
2. El comprobante de liquidación ya está emitido en la misma transacción.

## Flujo principal

1. Se toma el **devengo propio** acumulado hasta el corte: el último acumulado de `devengo_dpf` con
   fecha menor o igual al corte (constitución + días del período). Si el DPF se canceló antes, los
   días son los que informa el aliado.
2. Se calcula lo **esperado**: el devengo menos la **penalización** del contrato (solo en la
   cancelación anticipada: `devengo × (1 − penalización)`, a centavos, half-even; la penalización
   es la diferencia).
3. Se compara lo esperado con el **interés pagado** (el del comprobante):
   - iguales → `CONCILIADA`;
   - distintos → `DISCREPANCIA`, con motivo `SIN_DEVENGO_AL_CORTE` (no hay devengo hasta el corte:
     el trabajo diario no corrió) o `INTERES_DISTINTO_DEL_DEVENGO`.
4. Se registra la distribución (append-only, única por rescate): titular, período, devengado,
   penalización, esperado, pagado, retención e interés neto (`pagado − retención`). La base exige que
   el titular, el inicio del período, el interés y la retención sean los de la posición y del
   comprobante (`R-INV-09`).
5. Se emite `inversiones.interes_distribuido` o `inversiones.distribucion_con_discrepancia`.

La discrepancia contra el devengo **no frena el crédito**: el pago del emisor ya pasó el control más
fuerte (CU-125). Lo que dice es que el devengo propio está atrasado, y eso se corrige corriendo el
devengo, no negándole al titular lo que el emisor ya pagó.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 3a | No corrió el devengo hasta el corte | `DISCREPANCIA` con `SIN_DEVENGO_AL_CORTE`; el titular cobra igual |
| 3b | Cancelación anticipada | La penalización es la parte del devengo que no se paga; concilia si coincide |
| 3c | El emisor informa otro interés | `AP-CU125-01`: no se acredita ni se reparte nada |
| 4a | El rescate se sincroniza otra vez | Una sola distribución (`UNIQUE` por rescate) |
| — | La posición es de otra persona | Para quien pregunta no existe: `AP-CU123-01` |

## Postcondiciones

- Lo repartido es lo que separó el comprobante: el mismo interés, la misma retención.
- Principal devuelto + interés neto repartido es exactamente lo que acredita el libro.
- Toda discrepancia queda visible en `GET .../distribuciones` y cuenta en el reporte de
  [[CU-128 Informar comprobantes y reporte fiscal separables]].

## Contrato · `openapi/inversiones.yaml`

```ts
export const SalidaCU126 = z.object({
  distribuciones: z.array(z.object({
    distribucionId: z.string().uuid(),
    rescateId: z.string().uuid(),
    periodoDesde: z.string().date(),
    periodoHasta: z.string().date(),
    interesDevengado: MontoSchema,
    penalizacion: MontoSchema,
    interesEsperado: MontoSchema,
    interesPagado: MontoSchema,
    retencion: MontoSchema,
    interesNeto: MontoSchema,
    estado: z.enum(['CONCILIADA', 'DISCREPANCIA']),
    motivoDiscrepancia: z.string().optional(),
    distribuidaEn: z.string().datetime(),
  }).strict()),
}).strict()
```

No tiene códigos propios: pedir las distribuciones de una posición ajena es `AP-CU123-01` (la posición
no existe para quien pregunta).

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `DevengoDeDpf` | El interés simple con un único redondeo |
| Molécula | `DistribucionRepositorio` | La distribución, append-only y única por rescate |
| Organismo | `CU126DistribuirIntereses` | Concilia contra el devengo y registra; lista lo repartido |
| Página | `GET /inversiones/posiciones/{posicionId}/distribuciones` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.interes_distribuido` · `inversiones.distribucion_con_discrepancia` | Revisión de operaciones cuando hay discrepancia | `BILLETERA_VER` |

## Interfaz

- **App:** detalle de la posición cerrada: interés pagado, retención y neto, con el período.
  Pendiente de especificar en el frontend.
- **Backoffice:** lista de distribuciones en discrepancia (pendiente de especificar).

## Restricciones aplicables

`R-INV-09` · `R-AUD-01`

## Evidencia que deja

`distribucion_interes` (append-only) · `comprobante_inversion` · `devengo_dpf` · `evento_dominio` (outbox).

## Criterios de aceptación

```gherkin
Dado el interes pagado por el emisor y un devengo diario completo
Cuando se acredita
Entonces se reparte al titular de la posicion, por su periodo y con su retencion, y concilia contra el devengo

Dado un devengo propio que no corrio hasta el corte
Cuando se acredita el interes
Entonces queda una discrepancia con su motivo a la vista y el titular cobra igual lo que pago el emisor

Dado un DPF cancelado antes del vencimiento
Cuando se acredita el interes
Entonces la penalizacion es la parte del devengo que no se paga y la distribucion concilia

Dado un interes que el emisor informa distinto del calculo propio
Cuando se confirma el rescate
Entonces no se reparte nada y queda la discrepancia con el emisor
```

Además: la no duplicación al sincronizar dos veces el rescate, el cuadre contra el libro, el rechazo de
la posición ajena y los rechazos de la base por `R-INV-09` (otro titular, otro período, otra retención,
conciliada sin cuadrar) y `R-AUD-01` (ni `UPDATE` ni `DELETE`).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-03 | Tratamiento fiscal del interés y de su retención; la «retención» es una tasa sintética | Fiscal |
| DR-INV-01 | Si el emisor paga intereses periódicos antes del vencimiento: hoy solo se reparte el de la liquidación | Aliado + producto |

El ADR del carril D mencionaba `movimiento_posicion`: **no se crea**. El saldo de una posición se deriva
del comprobante (append-only), que ya registra la constitución y cada liquidación; un segundo libro de
movimientos de la posición diverge del primero sin aportar nada.

## Ver también

[[CU-123 Devengar y valorar una posición de inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]] · [[CU-128 Informar comprobantes y reporte fiscal separables]]
