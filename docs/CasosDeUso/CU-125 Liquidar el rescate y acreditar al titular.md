---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-125
criticidad: alta
actores: [Sistema, Usuario, Aliado]
normas: [Trazabilidad del dinero, Conciliación, Protección del inversor]
---

# CU-125 — Liquidar el rescate y acreditar al titular

> **Objetivo.** Del «el aliado dijo que sí» al dinero acreditado: nada de lo que informa
> el aliado se acepta porque venga firmado. El interés y la retención se **recalculan**
> y se **concilian**, el comprobante se descompone **sin residuo** y la plata se
> acredita **una sola vez**.

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Ver [[CU-120 Consultar productos de inversión y sus condiciones]]. La retención, la
> penalización y la comisión por resultado que se aplican acá son las de las
> condiciones sintéticas aceptadas; no son una tarifa comercial ni un criterio fiscal.

## Actores y disparador

- **Actor principal:** el sistema, tras el pedido del rescate; el titular puede
  sincronizarlo (`BILLETERA_OPERAR`).
- **Terceros externos:** el aliado (confirma, rechaza o deja pendiente) y el libro de
  `nucleo-financiero` (acredita), ambos por puerto y con falla cerrada.
- **Disparador:** termina el pedido de [[CU-124 Solicitar el rescate de una inversión]] o llega
  `POST /inversiones/rescates/{rescateId}/sincronizacion`.

## Precondiciones

1. El rescate existe y es de la persona de la sesión (si no: `AP-CU124-07`).
2. El rescate está en curso (`SOLICITADO`, `PENDIENTE` o `INCIERTO`) o ya `POR_ACREDITAR`
   con el crédito pendiente.
3. La cuenta que se acredita es **la de la orden que creó la posición**.

## Flujo principal

1. Si el rescate está en curso, se **consulta** al aliado por la referencia: si la
   conoce, se resuelve con lo que dice; si no la conoce, nunca la recibió y se reenvía
   con la misma referencia; si no responde, queda `INCIERTO`.
2. El estado informado se resuelve así: **confirmado** → se liquida; **rechazado** →
   `RECHAZADO` (las cuotas vuelven a estar libres) y evento `inversiones.rescate_rechazado`;
   **pendiente** → `PENDIENTE` con su fecha valor y su fecha de liquidación.
3. **Liquidar** (una transacción; bloquea el rescate y la posición):
   - **DPF.** El interés propio = principal × tasa nominal × días / base de días,
     redondeado a centavos (half-even); los días son el plazo, o —si es anticipado— los
     que informa el aliado, y el interés se reduce por la penalización del contrato. La
     retención propia = interés × tasa de retención. Se exige que el importe que
     informa el aliado sea **principal + interés − retención** (si no, **AP-CU125-02**) y
     se registra la **conciliación** del interés pagado contra el devengo propio
     (`R-INV-08`): si el interés o la retención difieren, queda como **discrepancia**
     y **AP-CU125-01**: nada se acredita y el depósito sigue abierto. Si coinciden, se
     cierra la posición.
   - **Fondo.** El producido = cuotas × valor de cuota informado (centavos, half-even);
     debe ser igual al importe del aliado y, si ya tenemos publicado el valor de esa
     fecha, igual a ese valor (si no, **AP-CU125-02**). El costo base liberado se
     prorratea por cuotas y el **último rescate libera el costo restante entero**: el
     residuo de redondeo se asigna y no se pierde. Se descuentan las cuotas y, si no queda
     ninguna, se cierra la posición.
   - **Comisión por resultado** (si el producto la tiene). La comisión solo grava lo que
     supera la marca máxima previa: base elegible = (valor de cuota − marca máxima
     previa) × cuotas, si el valor la supera; comisión = base × tasa, a centavos, y
     **nunca mayor que la ganancia**; la marca nueva nunca baja (`R-INV-07`). Cada
     suscripción es su propio lote, con la marca que parte de su valor de entrada: no se
     cobra por ganancias anteriores a la entrada de la persona. Si el producido es menor
     al costo, la diferencia es **pérdida realizada**, sin interés ni comisión.
   - Se emite el **comprobante de liquidación**: `neto = principal + interés − impuesto −
     comisión` y `costo base = principal + pérdida realizada` (`R-INV-03`), inmutable
     (`R-AUD-01`). La posición cerrada no tiene cuotas ni queda sin fecha de cierre
     (`R-INV-04`).
   - Se crea la instrucción `ACREDITAR` pendiente, el rescate pasa a `POR_ACREDITAR` y se
     escribe `inversiones.rescate_confirmado`.
4. **Fuera de la transacción** se pide al libro el crédito con una **clave determinista**
   (repetirlo no acredita dos veces) y el rescate pasa a `LIQUIDADO`, con evento
   `inversiones.rescate_liquidado`. Si el libro no responde, el rescate queda
   `POR_ACREDITAR`: liquidado, descontado de la posición y visible, pero todavía no
   disponible; se reintenta con la misma clave.
5. Se responde el rescate con el neto a acreditar.

Ejemplo de cálculo de la comisión (sintético, del plan; **no es una tarifa comercial**):
100 cuotas, marca máxima 110, valor 112 y tasa 10 % dan base elegible 200,00, comisión
20,00 y cuota neta 111,80.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El aliado no responde | `INCIERTO`; se reintenta al sincronizar |
| 2a | El aliado rechaza | `RECHAZADO` con su motivo; las cuotas vuelven a estar libres |
| 2b | El aliado deja el rescate pendiente | `PENDIENTE`; después liquida, descuenta las cuotas y acredita una sola vez |
| 3a | El interés o la retención del DPF **no coinciden** con el cálculo propio | `AP-CU125-01`: discrepancia registrada, nada se acredita y el depósito sigue abierto |
| 3b | El importe informado no es principal + interés − retención (DPF) | `AP-CU125-02`; no se acredita |
| 3c | Un rescate de cuotas cuyo importe no es cuotas × valor, o cuyo valor difiere del ya publicado para esa fecha | `AP-CU125-02`; no se acredita y la posición no cambia |
| 3d | Rescate con pérdida | Sin comisión; la diferencia es pérdida realizada |
| 3e | Valor que cae y se recupera sin pasar la marca | No hay comisión por lo recuperado; lo rescatado después paga solo sobre lo que supera la marca del lote |
| 4a | El libro está caído al acreditar | `POR_ACREDITAR`; al volver, se acredita una vez |
| — | Una orden de suscripción rechazada | Se compensa la retención con una operación nueva (liberar), sin editar nada del historial |

## Postcondiciones

- El neto acreditado es exactamente el del comprobante, y el comprobante separa
  principal, interés, impuesto, comisión y pérdida sin residuo.
- El efecto neto sobre el mayor coincide con el del comprobante.
- Si hubo discrepancia con el aliado, no se movió plata y queda a la vista.
- El crédito ocurre una sola vez aunque se sincronice varias veces.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU125 = z.object({
  claveIdempotencia: z.string().uuid(), // exigida por el contrato; el crédito usa una clave determinista del rescate
  rescateId: z.string().uuid(),         // en la ruta
}).strict()

export const SalidaCU125 = z.object({
  rescateId: z.string().uuid(),
  posicionId: z.string().uuid(),
  tipo: z.enum(['PARCIAL', 'TOTAL', 'VENCIMIENTO', 'ANTICIPADO']),
  estado: z.enum(['SOLICITADO', 'PENDIENTE', 'INCIERTO', 'POR_ACREDITAR', 'LIQUIDADO', 'RECHAZADO']),
  netoAcreditar: MontoSchema.optional(),
  motivoRechazo: z.string().optional(),
  mensaje: z.string(),
}).strict()

export const ErroresCU125 = {
  DISCREPANCIA_CON_EL_EMISOR: 'AP-CU125-01',
  LIQUIDACION_NO_COINCIDE_CON_EL_VALOR: 'AP-CU125-02',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `DISCREPANCIA_CON_EL_EMISOR` | El interés o la retención que informa el aliado no coinciden con el cálculo propio: queda en revisión y no se acredita |
| `LIQUIDACION_NO_COINCIDE_CON_EL_VALOR` | El importe informado no coincide con principal + interés − retención (DPF) o con cuotas × valor de cuota (fondo) |

El comprobante de liquidación se lista con `GET /inversiones/posiciones/{posicionId}/comprobantes`
(ver [[CU-123 Devengar y valorar una posición de inversión]]); el contrato lo etiqueta `CU-128` y el cálculo de
comisión `CU-127`. El código `AP-CU127-01` que el encabezado del contrato declara
(`COMISION_NO_CONFIGURADA`) no lo emite ningún camino hoy.

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `DevengoDeDpf` | Interés simple y retención, con un único redondeo |
| Átomo | `ComisionDeExito` | Comisión por resultado con marca máxima previa; la marca no baja |
| Átomo | `ValoracionDeCuotas.costoBaseLiberado` | Costo prorrateado; el último rescate asigna el residuo |
| Átomo | `Descomposicion` | Principal, interés, impuesto, comisión y pérdida; verifica sus dos identidades al construir |
| Molécula | `CalculoDeLiquidacion` | Recalcula contra lo que dice el aliado y registra conciliación y comisión |
| Molécula | `ComprobanteRepositorio` | Comprobante inmutable, conciliación y comisión de éxito |
| Molécula | `AplicadorDeInstrucciones` | Acredita en el libro con clave determinista |
| Organismo | `CU125LiquidarRescate` | Consulta, liquida en una transacción y acredita después |
| Página | `POST /inversiones/rescates/{rescateId}/sincronizacion` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.rescate_rechazado` · `inversiones.rescate_confirmado` · `inversiones.rescate_liquidado` | La contabilización de la custodia de inversión en el núcleo (pendiente: ver decisiones abiertas) | `BILLETERA_OPERAR` |

## Interfaz

- **App:** detalle del rescate: estado real (`PENDIENTE`, `POR_ACREDITAR`, `LIQUIDADO`),
  comprobante con principal, interés, impuesto, comisión y neto separados. Pendiente de
  especificar en el frontend.
- **Backoffice:** vista de rescates en discrepancia con el aliado (pendiente de
  especificar; hoy queda registrada la conciliación).

## Restricciones aplicables

`R-INV-03` · `R-INV-04` · `R-INV-07` · `R-INV-08` · `R-AUD-01`

## Evidencia que deja

`rescate_inversion` · `comprobante_inversion` (append-only) · `conciliacion_interes` ·
`comision_exito` · `posicion_inversion` · `instruccion_libro` · `evento_dominio`
(outbox).

## Criterios de aceptación

```gherkin
Dado el interes pagado por el emisor
Cuando se acredita
Entonces coincide con titular, periodo y retencion y se concilia contra el devengo propio

Dado un interes que NO coincide con el calculo propio
Cuando el aliado confirma el rescate
Entonces AP-CU125-01: discrepancia registrada, nada se acredita y el deposito sigue abierto

Dado un importe que no es principal + interes - retencion
Cuando el aliado confirma el rescate
Entonces AP-CU125-02 y no se acredita

Dado un rescate de cuotas cuyo importe no es cuotas x valor
Cuando el aliado confirma el rescate
Entonces AP-CU125-02: no se acredita y la posicion no cambia

Dado un valor del aliado distinto del que ya tenemos publicado para esa fecha
Cuando el aliado confirma el rescate
Entonces AP-CU125-02

Dado un rescate demorado
Cuando el aliado confirma mas tarde
Entonces estuvo PENDIENTE hasta confirmar, despues liquida, descuenta las cuotas y acredita una sola vez

Dado el libro caido AL ACREDITAR
Cuando se liquida el rescate
Entonces el rescate queda POR_ACREDITAR (liquidado y descontado, pero no disponible); al volver, se acredita una vez

Dado un rescate liquidado con tratamiento definido
Cuando se emite el comprobante
Entonces principal, interes, impuesto y comision son separables y cuadran con el mayor

Dado el ejemplo sintetico del plan: 100 cuotas, marca 110, valor 112 y 10 %
Cuando se rescata
Entonces la comision es Bs 20,00 y la cuota neta 111,80 (no es una tarifa comercial)

Dado un rescate con perdida
Cuando se liquida
Entonces no hay comision porque se cobra solo beneficio elegible, y la diferencia es perdida realizada

Dado un valor que cae y luego se recupera
Cuando se rescata en dos tandas
Entonces lo rescatado con perdida no paga exito y lo rescatado despues solo paga sobre lo que supera la marca del lote

Dadas dos suscripciones con valores de entrada distintos
Cuando se rescata
Entonces cada lote paga exito solo sobre lo que supera SU valor de entrada
```

Además: la prueba de cuadre del redondeo (tres rescates de una posición de 100,00 en 3
cuotas liberan 33,33 + 33,34 + 33,33; el residuo se asigna y no se pierde), la de
compensación (una orden rechazada deshace la retención con una operación nueva) y los
rechazos de la base por `R-INV-03` y `R-AUD-01` (comprobante que no descompone sin
residuo; ni `UPDATE` ni `DELETE`), `R-INV-04` (cerrar una posición sin fecha de cierre o
con cuotas) y `R-INV-07` / `R-INV-08` (comisión sobre una base inexistente;
conciliación marcada conciliada con cifras distintas).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-03 | Tratamiento fiscal de intereses y de la comisión: alícuotas, retención, rescate anticipado. Hoy la «retención» del DPF es una tasa sintética de las condiciones y no un criterio fiscal | Fiscal |
| DR-INV-04 | Existencia, base contractual y fórmula de la comisión de éxito: marca máxima, cristalización. La fórmula de este caso es una propuesta técnica con datos sintéticos | Legal + fiscal + aliado |
| DR-INV-07 | Permiso propio de inversión (`INVERSION_OPERAR`) en el catálogo de roles; hoy se reutilizan `BILLETERA_OPERAR`, `BILLETERA_VER` y `ADMIN_PLATAFORMA` | Seguridad + producto |

El crédito del rescate depende de una operación del núcleo (`acreditarRescate`) que **hoy
no existe en el productor**: este caso solo corre contra el doble del libro. Ver
`evidencia/carril-D/contrato-requerido-nucleo.md`.

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-122 Confirmar la posición sin duplicar el saldo]] · [[CU-123 Devengar y valorar una posición de inversión]] · [[CU-124 Solicitar el rescate de una inversión]]
