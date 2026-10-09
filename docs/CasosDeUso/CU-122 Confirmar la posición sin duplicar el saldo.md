---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-122
criticidad: alta
actores: [Sistema, Usuario, Aliado]
normas: [Trazabilidad del dinero, Idempotencia de dinero]
---

# CU-122 — Confirmar la posición sin duplicar el saldo

> **Objetivo.** Que cuando el aliado confirma, aparezca la posición (depósito o cuotas)
> y el importe deje de estar disponible **una sola vez**. Si algo se corta en el medio,
> lo peor que puede pasar es «retenido y sin posición todavía» —visible y
> reintentable—, **nunca** «posición sin débito».

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Ver [[CU-120 Consultar productos de inversión y sus condiciones]]. El aliado de este
> caso es hoy un simulador (`herramientas/aliado_simulado`).

## Actores y disparador

- **Actor principal:** el sistema, que avanza la saga de la orden.
- **Actor secundario:** el titular, que ve su orden (`BILLETERA_VER`) o pide
  sincronizarla (`BILLETERA_OPERAR`). La orden de **otra persona no existe** para él.
- **Tercero externo:** el aliado, por el puerto `AliadoDeInversion`. La referencia de
  toda operación es el **id de nuestra orden**, estable en los reintentos.
- **Disparador:** se crea la orden ([[CU-121 Aceptar las condiciones y ordenar una inversión]])
  o llega `POST /inversiones/ordenes/{ordenId}/sincronizacion`.

## Precondiciones

1. Existe una orden propia (sesión == titular de la orden; si no, **AP-CU122-01**).
2. La orden está en un estado abierto (`CREADA`, `RETENIDA`, `ENVIADA`, `INCIERTA`) o ya
   `CONFIRMADA` con el débito pendiente.
3. Cada efecto sobre el libro ya existe como **intención persistida** (`instruccion_libro`)
   antes de pedirlo.

## Flujo principal

La saga avanza un paso idempotente a la vez (hasta seis por pasada, y se corta sola si
no hay progreso: el estado queda visible en vez de insistir en bucle).

1. `CREADA` → se aplica la instrucción `RETENER` en el libro (fuera de transacción) y
   la orden pasa a `RETENIDA`; evento `inversiones.saldo_reservado`.
2. `RETENIDA` → pasa a `ENVIADA` con una transición condicionada al estado (solo un
   actor la envía) y se le suscribe al aliado con la referencia estable.
3. Respuesta del aliado:
   - **confirmó** → paso 5;
   - **rechazó de forma definitiva** (sin haber registrado nada) → paso 7;
   - **pendiente** → la orden queda `ENVIADA`;
   - **no se sabe** (timeout, corte, respuesta sin firma válida) → `INCIERTA`. El saldo
     **sigue retenido**; jamás se interpreta un «no sé» como rechazo.
4. `ENVIADA` o `INCIERTA` se resuelven **consultando** al aliado por la referencia: si
   la conoce, se sigue con lo que dice; si no la conoce, nunca la recibió y se reenvía
   con la misma referencia; si no responde, queda `INCIERTA`. No se libera el saldo
   mientras no se sepa qué pasó.
5. **Confirmación.** Se exige que lo confirmado coincida con lo pedido: estado
   confirmado, monto igual al de la orden y posición externa informada; en un fondo,
   valor de cuota positivo y cuotas = monto / valor de cuota (seis decimales, hacia
   abajo); en un DPF, fecha de vencimiento informada. Si no coincide: **AP-CU122-02**,
   no se registra la posición y el importe sigue retenido. Si coincide, en **una
   transacción**: se crea la posición (DPF con vencimiento y sin cuotas; fondo con
   cuotas, valor de entrada y marca máxima inicial igual al valor de entrada), el
   comprobante de `SUSCRIPCION` (todo es principal), la instrucción `DEBITAR`
   pendiente, la orden pasa a `CONFIRMADA` y se escribe `inversiones.posicion_constituida`.
6. `CONFIRMADA` → se aplica `DEBITAR`: el libro convierte la retención en el débito
   definitivo (sale del disponible y del retenido en un solo movimiento). Si el libro no
   responde, la instrucción queda pendiente y se reintenta con la **misma clave**; el
   importe sigue sin estar disponible y no se acredita nada dos veces.
7. **Rechazo.** La orden pasa a `RECHAZADA`; si había una retención aplicada, se crea la
   instrucción `LIBERAR` y se aplica una sola vez (una operación nueva, no una edición
   del historial); evento `inversiones.orden_rechazada`.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 3a | El aliado confirma un importe **distinto** del pedido | `AP-CU122-02`: no hay posición y el importe sigue retenido |
| 3b | La respuesta se pierde (el aliado la registró y a nosotros nos llega un corte) | `INCIERTA`; el saldo sigue retenido; se **consulta** y no se reenvía a ciegas |
| 3c | El aliado está caído antes de recibirla | `INCIERTA`; al volver, la consulta dice que no la conoce y recién ahí se reenvía con la misma referencia |
| 6a | El libro está caído al debitar | La posición existe pero el importe sigue retenido (no hay doble disponibilidad); al volver el libro se debita una vez |
| 7a | El aliado rechaza | `RECHAZADA` y lo retenido se libera exactamente una vez |
| — | Se sincroniza dos veces la misma orden | No duplica posición, comprobante ni débito |
| — | Una orden ajena | `AP-CU122-01`: ni se ve ni se sincroniza |

## Postcondiciones

- Efectivo + posición = saldo inicial: lo invertido ya no cuenta como disponible y no
  hay doble disponibilidad.
- Hay a lo sumo una posición por orden, un comprobante de suscripción y un débito.
- Un estado intermedio (`RETENIDA`, `ENVIADA`, `INCIERTA`) es visible para la persona
  con un mensaje claro.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU122 = z.object({
  claveIdempotencia: z.string().uuid(), // exigida por el contrato; la saga es idempotente por orden y estado
  ordenId: z.string().uuid(),           // en la ruta
}).strict()

export const SalidaCU122 = z.object({
  ordenId: z.string().uuid(),
  estado: z.enum(['CREADA', 'RETENIDA', 'ENVIADA', 'INCIERTA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA']),
  posicionId: z.string().uuid().optional(),
  fechaValor: z.string().optional(),
  motivoRechazo: z.string().optional(),
  mensaje: z.string(),
}).strict()

export const ErroresCU122 = {
  ORDEN_INEXISTENTE: 'AP-CU122-01',
  CONFIRMACION_NO_COINCIDE: 'AP-CU122-02',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `ORDEN_INEXISTENTE` | La orden no existe o es de otra persona |
| `CONFIRMACION_NO_COINCIDE` | Lo que confirma el aliado (monto, cuotas, posición externa, vencimiento) no coincide con la orden: no se registra la posición |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `ConfirmacionDeSuscripcion` | Exige que la confirmación del aliado coincida con lo pedido |
| Átomo | `Descomposicion.suscripcion` | Todo el importe es principal; sin interés, impuesto ni comisión |
| Molécula | `PosicionRepositorio` | Alta de la posición con las columnas propias de su tipo |
| Molécula | `ComprobanteRepositorio` | Comprobante inmutable de la suscripción |
| Molécula | `InstruccionRepositorio` · `AplicadorDeInstrucciones` | Intención persistida y aplicación idempotente en el libro |
| Molécula | `AliadoSimuladoHttp` | Suscribir y consultar por referencia, con firma verificada |
| Organismo | `CU122ConfirmarPosicion` | Avanza la saga un paso idempotente a la vez |
| Página | `GET /inversiones/ordenes/{ordenId}` · `POST /inversiones/ordenes/{ordenId}/sincronizacion` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.saldo_reservado` · `inversiones.posicion_constituida` · `inversiones.orden_rechazada` · `inversiones.instruccion_aplicada` | Nada todavía | `BILLETERA_VER` para ver · `BILLETERA_OPERAR` para sincronizar |

`AplicadorDeInstrucciones.reintentarPendientes` existe para reintentar las instrucciones
pendientes, pero **ningún trabajo programado lo invoca todavía**: hoy lo ejercita la
sincronización de cada orden y las pruebas. Cablear ese trabajo (con su bloqueo) queda
pendiente.

## Interfaz

- **App:** detalle de la orden con su estado real y su mensaje; botón «Actualizar
  estado» que sincroniza. Pendiente de especificar en el frontend.
- **Backoffice:** ninguna acción manual en este caso.

## Restricciones aplicables

`R-INV-04` · `R-INV-06`

## Evidencia que deja

`orden_inversion` · `posicion_inversion` · `comprobante_inversion` · `instruccion_libro`
· `evento_dominio` (outbox). El movimiento de débito vive en el libro del núcleo.

## Criterios de aceptación

```gherkin
Dada la confirmacion del aliado
Cuando se procesa
Entonces nace la posicion, se elimina la disponibilidad del importe y efectivo + posicion = saldo inicial

Dado un aliado que confirma un importe DISTINTO
Cuando se procesa la confirmacion
Entonces AP-CU122-02: no hay posicion y el importe sigue retenido

Dado un aliado que rechaza
Cuando se procesa la orden
Entonces la orden queda RECHAZADA y lo retenido se libera exactamente una vez

Dada la respuesta PERDIDA (el aliado la registro y a nosotros nos llega un corte)
Cuando se procesa la orden
Entonces INCIERTA, el saldo sigue retenido, se CONSULTA y no se reenvia a ciegas

Dado el aliado CAIDO antes de recibirla
Cuando se procesa la orden
Entonces INCIERTA; al volver, la consulta dice que no la conoce y recien ahi se reenvia con la misma referencia

Dado el libro caido AL DEBITAR
Cuando se procesa la confirmacion
Entonces la posicion existe pero el importe sigue retenido (no hay doble disponibilidad); al volver el libro, se debita una vez

Dada una orden de OTRA persona
Cuando intenta verla o sincronizarla
Entonces AP-CU122-01: no existe para ella
```

Además, la prueba de reintento (sincronizar dos veces no duplica posición, comprobante
ni débito) y los rechazos de la base por `R-INV-04` (una posición incoherente con su
tipo) y `R-INV-06` (una instrucción al libro aplicada sin referencia, o con una clave de
idempotencia repetida).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-06 | Titularidad y custodia de las posiciones: a nombre de quién queda registrada la posición ante el aliado. El texto que la persona acepta hoy lo dice como **sintético** | Legal + proveedor |

El débito definitivo depende de una operación del núcleo que **hoy no existe en el
productor**: `cerrarRetencion(EJECUTADA)` no alcanza, porque el saldo se deriva de los
movimientos menos las retenciones vigentes. Ver
`evidencia/carril-D/contrato-requerido-nucleo.md`.

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-123 Devengar y valorar una posición de inversión]] · [[CU-124 Solicitar el rescate de una inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]]
