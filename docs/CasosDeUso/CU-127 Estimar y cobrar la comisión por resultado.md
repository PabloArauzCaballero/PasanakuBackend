---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
codigo: CU-127
criticidad: alta
actores: [Usuario, Sistema]
normas: [Protección del inversor, Trazabilidad del dinero]
---

# CU-127 — Estimar y cobrar la comisión por resultado

> **Objetivo.** Que la comisión por resultado de un fondo se pueda **ver antes de rescatar** y se
> cobre **solo sobre el beneficio elegible**: lo que supera la marca máxima previa del lote, neta
> de comisión. Una pérdida no se cobra, una recuperación sin pasar la marca no se cobra, y el
> mismo tramo de ganancia no se cobra dos veces.

> [!warning] Parámetros SINTÉTICOS (`DR-INV-04`)
> La existencia, la base contractual y la fórmula de esta comisión **no tienen fuente aprobada**
> (legal, fiscal, aliado). La tasa sale de las condiciones de cada producto y mientras no tenga
> fuente es sintética; el ejemplo del plan (100 cuotas, marca 110, valor 112, 10 % → Bs 20,00; cuota
> neta 111,80) es un cálculo, **no una tarifa comercial**. Si el producto del aliado no admite este
> cargo, la función permanece inhabilitada: sin tasa configurada no hay comisión.

## Actores y disparador

- **Actor principal:** el titular de la posición (`BILLETERA_VER`) para estimar; el sistema para
  cobrar, dentro de la liquidación de [[CU-125 Liquidar el rescate y acreditar al titular]].
- **Disparador:** `GET /inversiones/posiciones/{posicionId}/comision`.

## Precondiciones

1. La posición es de la persona de la sesión y es de un **fondo** cuyas condiciones declaran
   `tasaComisionExito`.
2. Quedan cuotas abiertas y hay un valor de cuota publicado.

## Flujo principal

1. **Lote.** Cada suscripción es su propia posición, con su propia marca máxima: la que parte de su
   **valor de entrada**. No se cobra por ganancias anteriores a la entrada de la persona, y una
   pérdida en un lote no se compensa ni se cobra en otro.
2. **Estimar.** Con el último valor de cuota publicado: base elegible = (valor − marca previa) ×
   cuotas, solo si el valor supera la marca; comisión = base × tasa, a centavos half-even; cuota neta
   = (valor × cuotas − comisión) / cuotas, a seis decimales; marca nueva = el mayor entre la previa
   y la cuota neta.
3. **Cobrar** (en [[CU-125 Liquidar el rescate y acreditar al titular]]): con el valor que confirma el aliado, la
   misma cuenta, **nunca mayor que la ganancia** del rescate; se guarda en `comision_exito`
   (append-only) y la **marca máxima de la posición sube** a la marca nueva.
4. **Rescate parcial.** Las cuotas que quedan parten de la marca nueva: el siguiente rescate cobra
   solo lo que supere esa marca. Sin este paso, rescatar en dos tandas cobraría dos veces el mismo
   tramo de ganancia.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | Producto sin comisión (todo DPF, o fondo sin tasa) | `AP-CU127-01`: no hay nada que estimar |
| 2a | El valor de cuota está por debajo o en la marca | Sin base elegible ni comisión; la marca no cambia |
| 2b | Todavía no hay valor de cuota publicado | `AP-CU127-02`: no se inventa una estimación |
| 2c | La posición ya no tiene cuotas | `AP-CU127-03` |
| 3a | Rescate con pérdida | Sin comisión; la diferencia es pérdida realizada |
| 3b | Pérdida previa en un lote y suscripción nueva más alta | Cada lote paga solo sobre su propia marca |
| 4a | Rescate parcial con beneficio | La marca sube; el resto no paga de nuevo el mismo tramo |
| — | La posición es de otra persona | `AP-CU123-01`, como si no existiera |

## Postcondiciones

- La marca máxima de una posición **nunca baja** (`R-INV-07`): la base lo rechaza.
- La comisión cobrada nunca supera el beneficio ni la base elegible.
- Estimar no escribe nada.

## Contrato · `openapi/inversiones.yaml`

```ts
export const SalidaCU127 = z.object({
  posicionId: z.string().uuid(),
  cuotas: z.string().regex(/^\d+\.\d{6}$/),
  valorCuota: z.string().regex(/^\d+\.\d{6}$/),
  fechaValor: z.string().date(),
  marcaMaximaPrevia: z.string().regex(/^\d+\.\d{6}$/),
  tasa: z.string().regex(/^\d+\.\d{6}$/),
  baseElegible: MontoSchema,
  comisionEstimada: MontoSchema,
  cuotaNetaEstimada: z.string().regex(/^\d+\.\d{6}$/),
  marcaMaximaNueva: z.string().regex(/^\d+\.\d{6}$/),
  comisionCobradaAntes: MontoSchema,
  origenDatos: z.enum(['SINTETICO', 'VERIFICADO']),
  advertencia: z.string(),
}).strict()

export const ErroresCU127 = {
  COMISION_NO_CONFIGURADA: 'AP-CU127-01',
  VALOR_CUOTA_NO_DISPONIBLE: 'AP-CU127-02',
  SIN_CUOTAS_ABIERTAS: 'AP-CU127-03',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `COMISION_NO_CONFIGURADA` | El producto no declara comisión por resultado (DPF, o fondo sin tasa) |
| `VALOR_CUOTA_NO_DISPONIBLE` | No hay ningún valor de cuota publicado |
| `SIN_CUOTAS_ABIERTAS` | La posición ya no tiene cuotas |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `ComisionDeExito` | Comisión con marca máxima previa; la marca no baja |
| Molécula | `CalculoDeLiquidacion` | Cobra al liquidar y devuelve la marca nueva |
| Molécula | `PosicionRepositorio.descontarCuotas` | Guarda la marca con las cuotas que quedan |
| Organismo | `CU127ComisionPorResultado` | Estima con el último valor publicado |
| Página | `GET /inversiones/posiciones/{posicionId}/comision` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| — (cobrar emite lo de CU-125) | — | `BILLETERA_VER` |

## Interfaz

- **App:** en el detalle de un fondo, la comisión estimada **aparte** y descontada del neto
  estimado, con la advertencia de que puede haber pérdidas. Pendiente de especificar.

## Restricciones aplicables

`R-INV-07`

## Evidencia que deja

`comision_exito` (append-only) · `posicion_inversion` (marca máxima) · `valor_cuota`.

## Criterios de aceptación

```gherkin
Dado un DPF que no tiene comision por resultado
Cuando se estima
Entonces AP-CU127-01

Dado el ejemplo sintetico del plan: 100 cuotas, marca 110, valor 112 y 10 %
Cuando se estima
Entonces la base elegible es Bs 200,00, la comision Bs 20,00 y la cuota neta 111,80

Dado un valor de cuota por debajo de la marca previa
Cuando se estima
Entonces no hay beneficio elegible ni comision

Dado un fondo sin ningun valor de cuota publicado
Cuando se estima
Entonces AP-CU127-02 y no se inventa una estimacion

Dada una posicion de fondo ya rescatada por completo
Cuando se estima
Entonces AP-CU127-03: no quedan cuotas

Dado un rescate parcial con beneficio
Cuando se rescata el resto al mismo valor
Entonces la marca ya subio y no se cobra dos veces el mismo tramo

Dadas una perdida previa en un lote y una suscripcion nueva mas alta
Cuando se estima cada una
Entonces cada lote paga solo sobre lo que supera su propia marca
```

Además: la posición de otra persona (`AP-CU123-01`) y el rechazo de la base por `R-INV-07` (bajar la
marca máxima de una posición).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-04 | Existencia, base contractual y fórmula de la comisión; cristalización y tratamiento de entradas y salidas | Legal + fiscal + aliado |
| DR-INV-03 | Tratamiento fiscal de la comisión | Fiscal |

El `AP-CU127-01` que el encabezado del contrato declaraba sin camino que lo emitiera ahora lo emite
este caso de uso.

## Ver también

[[CU-123 Devengar y valorar una posición de inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]] · [[CU-128 Informar comprobantes y reporte fiscal separables]]
