---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
codigo: CU-123
criticidad: media
actores: [Usuario, Operaciones, Sistema]
normas: [Trazabilidad del dinero, Prohibido inferir parámetros financieros]
---

# CU-123 — Devengar y valorar una posición de inversión

> **Objetivo.** Mostrar qué vale una posición **hoy**, con la fecha del dato y **sin
> promesa**: un DPF muestra por separado lo devengado (que todavía no es plata en mano)
> de lo pagado; un fondo muestra lo que valen sus cuotas, que puede ser menos de lo
> invertido.

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Ver [[CU-120 Consultar productos de inversión y sus condiciones]]. Las tasas, la base
> de días y los valores de cuota de este caso son sintéticos mientras no tengan fuente
> aprobada.

## Actores y disparador

- **Actor principal:** el usuario titular, que mira sus posiciones (`BILLETERA_VER`).
- **Actor de plataforma:** Operaciones, que corre el devengo diario y sincroniza los
  valores de cuota (`ADMIN_PLATAFORMA`). Hoy no hay trabajo programado: se disparan por
  API.
- **Disparador:** `GET /inversiones/posiciones[/{posicionId}]`,
  `GET /inversiones/posiciones/{posicionId}/comprobantes`,
  `POST /inversiones/devengos` o `POST /inversiones/valores-cuota/sincronizacion`.

## Precondiciones

1. Existe la posición y es del titular de la sesión (si no: **AP-CU123-01**).
2. Para devengar: hay DPF abiertos. Para valorar: el aliado publicó al menos un valor de
   cuota del fondo.
3. El cálculo se hace con las condiciones **de la versión que la persona aceptó**, no con
   las vigentes hoy.

## Flujo principal

**Ver una posición (o listarlas)**

1. Se resuelve la posición propia y la versión de condiciones con la que se constituyó.
2. **DPF.** Se muestran: tasa nominal anual, base de días, días devengados (entre la
   constitución y hoy, con tope en el plazo), **interés devengado** (último acumulado
   hasta hoy), **interés pagado** (suma de los intereses de sus comprobantes de
   liquidación), **impuesto estimado** (retención sobre lo devengado y no pagado, solo
   mientras la posición está abierta), impuesto pagado y vencimiento.
3. **Fondo.** Se muestran: cuotas, **último valor de cuota publicado con su fecha**, su
   antigüedad en días y la marca `desactualizado` cuando esa antigüedad supera la
   tolerancia (`aportaya.inversiones.valor-cuota.tolerancia-dias`, hoy 3: es un criterio
   técnico, no comercial); valor bruto = cuotas × valor (centavos, half-even);
   **variación** contra el costo base vigente (**puede ser negativa**); la comisión de
   éxito devengada, si el producto la tiene, **aparte** y descontada del neto estimado
   (la regla de cálculo está en [[CU-125 Liquidar el rescate y acreditar al titular]]); y el valor
   neto estimado. Si todavía no hay ningún valor publicado, **no se inventa una
   valoración**.
4. Toda vista lleva `origenDatos` y la advertencia: invertir puede generar pérdidas, el
   rendimiento no está garantizado y la garantía del pozo de un grupo no alcanza a esta
   inversión.
5. **Comprobantes.** Cada comprobante separa principal, interés, impuesto y comisión
   (más pérdida realizada y neto). El contrato lo etiqueta `CU-128`; esta especificación
   lo incluye acá porque lo resuelve `CU123Posiciones`.

**Devengo diario de los DPF (Operaciones)**

1. Para cada DPF abierto cuya fecha de devengo cae entre su constitución y su
   vencimiento, se calcula el **acumulado** = principal × tasa nominal × días reales /
   base de días, redondeado **una vez** a centavos (half-even) y con tope en el plazo.
2. El devengo del día es la **diferencia de dos acumulados ya redondeados**: la suma de
   todos los días es exactamente el interés del plazo, sin un centavo perdido.
3. Se inserta (único por posición y fecha, append-only). Si ya estaba, se cuenta como
   «ya devengada». Correr después de varios días sin correr cubre la diferencia.

**Sincronizar valores de cuota (Operaciones)**

1. Por cada fondo activo se pide al aliado su último valor publicado.
2. Se guarda un valor por producto y fecha; **un valor ya publicado no se reescribe**.
3. Si el aliado publica un valor **distinto** para una fecha ya guardada, se rechaza con
   **AP-CU123-02** y queda el primero.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | La posición es de otra persona o no existe | `AP-CU123-01`: no existe para ella, ni en el listado ni en sus comprobantes |
| 3a | El valor de la cuota cae | La variación sale **negativa**, con fecha y valor neto, sin promesa |
| 3b | El último valor es viejo | Hasta 3 días no avisa; a los 4 se marca `desactualizado` |
| 3c | Un fondo sin ningún valor publicado | No hay valoración; no se inventa |
| 3d | El valor supera la marca máxima | La comisión de éxito devengada se muestra aparte y se descuenta del neto estimado |
| 6a | El devengo se corre dos veces el mismo día | No duplica nada (único por posición y fecha) |
| 6b | Pasaron días sin correr el devengo | El devengo del día cubre la diferencia |
| 6c | La fecha es anterior a la constitución o posterior al vencimiento | No se devenga |
| 8a | El aliado está caído al sincronizar valores | `AP-CU123-02`; la valoración existente sigue visible |
| 8b | El aliado publica otro valor para una fecha ya guardada | `AP-CU123-02` y se conserva el primero |

## Postcondiciones

- El devengo diario de un DPF suma exactamente su interés del plazo; el acumulado final
  es el mismo número.
- Un valor de cuota publicado no cambia.
- Nada se presenta como rentabilidad segura: lo devengado no es lo pagado.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU123 = z.object({
  claveIdempotencia: z.string().uuid(), // en las operaciones con efecto (devengo y sincronización de valores)
  fecha: z.string().date(),             // solo en el devengo
}).strict()

export const SalidaCU123 = z.object({
  posicionId: z.string().uuid(),
  tipo: z.enum(['DPF', 'FONDO']),
  productoCodigo: z.string(),
  estado: z.enum(['ABIERTA', 'CERRADA']),
  principal: MontoSchema,
  costoBaseVigente: MontoSchema,
  fechaConstitucion: z.string().date(),
  fechaVencimiento: z.string().date().optional(),
  valoracion: z.object({
    cuotas: z.string(),
    valorCuota: z.string(),
    fechaValor: z.string().date(),
    antiguedadDias: z.number().int(),
    desactualizado: z.boolean(),
    valorBruto: MontoSchema,
    variacion: MontoSchema,
    comisionDeExitoDevengada: MontoSchema,
    valorNetoEstimado: MontoSchema,
  }).optional(),
  dpf: z.object({
    interesDevengado: MontoSchema,
    interesPagado: MontoSchema,
    impuestoEstimado: MontoSchema,
    impuestoPagado: MontoSchema,
    vencimiento: z.string().date(),
  }).optional(),
  origenDatos: z.enum(['SINTETICO', 'VERIFICADO']),
  advertencia: z.string(),
}).strict()

export const ErroresCU123 = {
  POSICION_INEXISTENTE: 'AP-CU123-01',
  VALOR_CUOTA_NO_DISPONIBLE: 'AP-CU123-02',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `POSICION_INEXISTENTE` | La posición no existe o es de otra persona |
| `VALOR_CUOTA_NO_DISPONIBLE` | El aliado no respondió al sincronizar valores, o publicó dos valores distintos para la misma fecha |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `DevengoDeDpf` | Interés simple por días reales sobre la base contractual; redondeo único |
| Átomo | `ValoracionDeCuotas` | Cuotas × valor, variación y antigüedad del dato; sin promesa |
| Átomo | `ComisionDeExito` | Comisión por resultado con marca máxima previa (ver CU-125) |
| Molécula | `PosicionRepositorio` | Posiciones y devengos (append-only, único por posición y fecha) |
| Molécula | `CatalogoRepositorio` | Condiciones aceptadas y valores de cuota publicados |
| Molécula | `ComprobanteRepositorio` | Comprobantes y costo base vigente |
| Organismo | `CU123Devengo` | Corre el devengo diario en una transacción |
| Organismo | `CU123Posiciones` | Valora, lista comprobantes y sincroniza valores de cuota |
| Página | `GET /inversiones/posiciones` · `GET /inversiones/posiciones/{posicionId}` · `GET /inversiones/posiciones/{posicionId}/comprobantes` · `POST /inversiones/devengos` · `POST /inversiones/valores-cuota/sincronizacion` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| Ninguno propio | Nada | `BILLETERA_VER` para ver · `ADMIN_PLATAFORMA` para devengar y sincronizar valores |

Pendiente: programar el devengo diario y la sincronización de valores como trabajos con
bloqueo (hoy se disparan por API).

## Interfaz

- **App:** detalle de la posición con devengado/pagado (DPF) o valor, variación y
  marca de dato desactualizado (fondo), siempre con la advertencia. Pendiente de
  especificar en el frontend.
- **Backoffice:** acciones de Operaciones «Correr devengo» y «Sincronizar valores de
  cuota».

## Restricciones aplicables

`R-INV-08`

## Evidencia que deja

`devengo_dpf` (append-only) · `valor_cuota` (append-only) · `comprobante_inversion`.

## Criterios de aceptación

```gherkin
Dados principal, tasa y base contractual
Cuando devenga
Entonces distingue devengado, pagado, impuesto estimado y vencimiento

Dado un devengo que no corrio durante varios dias
Cuando corre de nuevo
Entonces el devengo del dia cubre la diferencia y no se pierde un centavo

Dado un DPF
Cuando se corre el devengo antes de constituir o despues del vencimiento
Entonces no se devenga nada

Dadas cuotas y valor publicado
Cuando cae el valor
Entonces muestra fecha, variacion NEGATIVA y valor neto, sin promesa

Dado un valor por encima de la marca
Cuando se valora la posicion
Entonces la comision de exito devengada se muestra aparte y se descuenta del neto estimado

Dado un valor de cuota publicado hace 3 o 4 dias
Cuando se valora la posicion
Entonces a los 3 dias no avisa y a los 4 se marca desactualizado

Dado un fondo sin ningun valor publicado
Cuando se valora la posicion
Entonces no se inventa una valoracion

Dado un valor de cuota ya publicado para una fecha
Cuando el aliado publica otro distinto para esa fecha
Entonces se rechaza con AP-CU123-02 y queda el primero

Dado el aliado caido
Cuando se sincronizan los valores de cuota
Entonces falla con AP-CU123-02 y la valoracion existente sigue visible

Dada la posicion de OTRA persona
Cuando esta intenta verla, listarla o pedir sus comprobantes
Entonces AP-CU123-01: no existe para ella
```

Además, las pruebas de cuadre (los 180 devengos diarios de un DPF suman exactamente
197,26 con una cuenta hecha a mano y el acumulado final es el mismo), de reintento
(correr el devengo dos veces el mismo día no duplica nada) y el rechazo de la base por
`R-INV-08` (un devengo diario mayor que el interés acumulado).

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-122 Confirmar la posición sin duplicar el saldo]] · [[CU-124 Solicitar el rescate de una inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]]
