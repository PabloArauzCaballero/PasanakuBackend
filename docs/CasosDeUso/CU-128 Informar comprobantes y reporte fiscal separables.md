---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-128
criticidad: alta
actores: [Usuario, Sistema]
normas: [Trazabilidad del dinero, Conciliación]
---

# CU-128 — Informar comprobantes y reporte fiscal separables

> **Objetivo.** Que cada movimiento de inversión se pueda informar con **principal, interés,
> impuesto y comisión por separado**, y que ese informe se pueda **reconciliar con el libro**: cada
> línea se cruza con la instrucción que movió la plata y queda conciliada, pendiente o con
> diferencia. Una exportación (CSV) trae las mismas cifras.

> [!warning] Parámetros SINTÉTICOS, no una declaración tributaria (`DR-INV-03`)
> El tratamiento fiscal de los intereses y de la comisión (alícuotas, retención, rescate anticipado)
> **lo define fiscal** y todavía no lo hizo. La retención es una tasa sintética de las condiciones de
> cada producto. El reporte lo declara en su encabezado (`origenDatos: SINTETICO`,
> `aptoProduccion: false`, `notaFiscal`) y no se presenta como base de ninguna declaración.

## Actores y disparador

- **Actor principal:** el titular (`BILLETERA_VER`), sobre sus propios comprobantes.
- **Disparador:** `GET /inversiones/posiciones/{posicionId}/comprobantes` (por posición),
  `GET /inversiones/reportes/fiscal?desde=&hasta=` y
  `GET /inversiones/reportes/fiscal/exportacion?desde=&hasta=` (CSV).

## Precondiciones

1. Sesión del titular: el reporte nunca trae comprobantes de otra persona.
2. Un periodo de 1 a 366 días.

## Flujo principal

1. Se leen los comprobantes del titular emitidos en el periodo (día de La Paz, inclusive en ambos
   extremos). Cada uno es inmutable (`R-AUD-01`) y descompone sin residuo:
   `neto = principal + interés − impuesto − comisión` (`R-INV-03`).
2. Cada línea se **cruza con el libro**: la suscripción con la instrucción `DEBITAR` de su orden
   (por el principal), la liquidación con la instrucción `ACREDITAR` de su rescate (por el neto).
   - instrucción aplicada y monto igual → `CONCILIADA`;
   - instrucción todavía pendiente (el libro no respondió) → `PENDIENTE_EN_LIBRO`;
   - aplicada con otro monto → `DIFERENCIA`; sin instrucción → `SIN_INSTRUCCION`.
3. Se totalizan por concepto (principal suscrito y devuelto, interés, impuesto, comisión, pérdida
   realizada y neto acreditado) y se arma la **conciliación con el libro**: lo debitado y lo
   acreditado según los comprobantes contra lo aplicado en el libro, las líneas pendientes o con
   diferencia y las distribuciones de interés con discrepancia ([[CU-126 Distribuir los intereses al titular y conciliarlos]]).
   `cuadra` solo es verdadero si no hay nada pendiente, ni diferencia, ni discrepancia.
4. La exportación es el mismo reporte en CSV: nota fiscal y periodo como comentarios, encabezado,
   una fila por línea y una fila `TOTAL`. Los textos del catálogo no arrancan con un carácter que una
   planilla tome por fórmula.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | Comprobantes de otra persona | No aparecen |
| 2a | El crédito todavía no llegó al libro | `PENDIENTE_EN_LIBRO`; el reporte no cuadra |
| 3a | Una distribución de interés quedó en discrepancia | El reporte no cuadra y la cuenta |
| 3b | Producto sintético | El reporte se declara `SINTETICO` y no apto para producción |
| — | Fecha final anterior a la inicial, o más de 366 días | `AP-CU128-01` |

## Postcondiciones

- Cada cifra del informe sale del comprobante inmutable; nada se recalcula aparte.
- El CSV y el reporte en pantalla traen las mismas cifras.
- Consultar no escribe nada.

## Contrato · `openapi/inversiones.yaml`

```ts
export const SalidaCU128 = z.object({
  desde: z.string().date(),
  hasta: z.string().date(),
  lineas: z.array(z.object({
    comprobanteId: z.string().uuid(),
    posicionId: z.string().uuid(),
    productoCodigo: z.string(),
    tipo: z.enum(['SUSCRIPCION', 'LIQUIDACION']),
    fecha: z.string().date(),
    principal: MontoSchema,
    interes: MontoSchema,
    impuesto: MontoSchema,
    comision: MontoSchema,
    perdidaRealizada: MontoSchema,
    neto: MontoSchema,
    origenDatos: z.enum(['SINTETICO', 'VERIFICADO']),
    estadoEnLibro: z.enum(['CONCILIADA', 'PENDIENTE_EN_LIBRO', 'DIFERENCIA', 'SIN_INSTRUCCION']),
  }).strict()),
  totales: z.object({ /* principalSuscrito, principalDevuelto, interes, impuesto, comision, perdidaRealizada, netoAcreditado */ }).strict(),
  conciliacion: z.object({ /* debitado/acreditado segun comprobantes y en libro, pendientes, diferencias, discrepancias, cuadra */ }).strict(),
  origenDatos: z.enum(['SINTETICO', 'VERIFICADO']),
  aptoProduccion: z.boolean(),
  notaFiscal: z.string(),
}).strict()

export const ErroresCU128 = {
  PERIODO_INVALIDO: 'AP-CU128-01',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `PERIODO_INVALIDO` | La fecha final es anterior a la inicial, o el periodo pasa de 366 días |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `Descomposicion` | Principal, interés, impuesto y comisión; verifica sus identidades |
| Molécula | `ComprobanteRepositorio` | Comprobantes inmutables, por posición o por titular y periodo |
| Organismo | `CU128ReporteFiscal` | Cruza con el libro, totaliza y exporta |
| Página | `GET /inversiones/reportes/fiscal` y `.../exportacion` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| — | — | `BILLETERA_VER` |

## Interfaz

- **App:** pantalla de comprobantes con los cuatro componentes separados y el estado de cada línea
  contra el libro. Pendiente de especificar.
- **Backoffice:** reporte por periodo y exportación (pendiente de especificar).

## Restricciones aplicables

`R-INV-03` · `R-AUD-01`

## Evidencia que deja

`comprobante_inversion` (append-only) · `instruccion_libro` · `distribucion_interes`.

## Criterios de aceptación

```gherkin
Dado un rescate liquidado con tratamiento definido
Cuando se informa el periodo
Entonces principal, interes, impuesto y comision aparecen separados y suman el neto de cada linea

Dados una suscripcion y una liquidacion aplicadas en el libro
Cuando se informa
Entonces todas las lineas quedan CONCILIADAS y el reporte cuadra con el libro

Dado un credito que todavia no llego al libro
Cuando se informa
Entonces la linea queda PENDIENTE_EN_LIBRO y el reporte no cuadra

Dado un periodo con la fecha final anterior a la inicial, o de mas de un anio
Cuando se informa
Entonces AP-CU128-01

Dado el reporte exportado
Cuando se compara con el reporte en pantalla
Entonces el CSV trae las mismas cifras, el encabezado fiscal y la fila de totales

Dados comprobantes de otra persona
Cuando se pide el reporte
Entonces no aparecen

Dada una distribucion de interes con discrepancia en el periodo
Cuando se informa
Entonces el reporte no cuadra y cuenta la discrepancia

Dado un producto sintetico
Cuando se informa
Entonces el reporte se declara SINTETICO, no apto para produccion y sin criterio fiscal real
```

Además: el rechazo de la base por `R-INV-03` (un comprobante que no descompone sin residuo).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-03 | Tratamiento fiscal real (alícuotas, retención, qué se informa y a quién): hasta entonces, parámetros sintéticos declarados | Fiscal |

El contrato etiquetaba este caso `CU-128` sin una especificación propia; el listado de comprobantes de
una posición ([[CU-123 Devengar y valorar una posición de inversión]]) sigue en la misma ruta.

## Ver también

[[CU-123 Devengar y valorar una posición de inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]] · [[CU-126 Distribuir los intereses al titular y conciliarlos]] · [[CU-127 Estimar y cobrar la comisión por resultado]]
