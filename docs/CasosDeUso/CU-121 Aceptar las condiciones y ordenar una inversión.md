---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-121
criticidad: alta
actores: [Usuario, Sistema]
normas: [Consentimiento versionado, Prohibido inferir parámetros financieros]
---

# CU-121 — Aceptar las condiciones y ordenar una inversión

> **Objetivo.** Que la persona acepte **exactamente** las condiciones que vio y ordene
> una inversión con su saldo libre; que el importe quede **reservado antes** de que nada
> salga hacia el aliado; y que el dinero afectado a un pozo **nunca** se invierta.

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Solo se invierte saldo propio y libre. Todo parámetro comercial o fiscal del producto
> (mínimo, costos, tasas, plazos) es sintético mientras no tenga fuente aprobada; ver
> [[CU-120 Consultar productos de inversión y sus condiciones]].

## Actores y disparador

- **Actor principal:** el usuario titular de la billetera (`BILLETERA_OPERAR`).
- **Actores de sistema:** el libro de `nucleo-financiero` (saldo y retención, solo por
  contrato) y el aliado, que entra recién en [[CU-122 Confirmar la posición sin duplicar el saldo]].
- **Disparador:** `POST /inversiones/ordenes` con `Idempotency-Key`.

## Precondiciones

1. Existe un producto `ACTIVO` con versión de condiciones vigente (CU-120) y, en un
   entorno productivo, esa versión es apta para producción.
2. La cuenta de billetera es del titular de la sesión: la identidad sale del token,
   nunca del cuerpo.
3. La moneda es bolivianos. Por ahora no se invierte en otra moneda.
4. El libro informa el saldo con tres datos: disponible, retenido y **comprometido en
   pozos**. Ese tercer dato todavía no lo publica el productor (ver «Decisiones
   abiertas»); mientras tanto el adaptador falla cerrado.

## Flujo principal

1. Se valida la moneda (`BOB`).
2. Se calcula la **huella** SHA-256 del contenido del pedido (producto, versión, hash
   del texto, cuenta, monto y moneda) y se busca una orden previa por titular + clave de
   idempotencia. Misma clave y misma huella: se devuelve esa orden y se sigue desde
   donde quedó. Misma clave y otra huella: **AP-CU121-06**.
3. Se valida el producto: existe, está `ACTIVO` y tiene versión vigente
   (**AP-CU121-01**; en producción tampoco se ofrece un dato sintético).
4. Se valida que la versión y el hash de texto que la persona aceptó **son los
   vigentes**. Si el producto cambió de condiciones entre que las vio y que ordenó:
   **AP-CU121-02**, y se le muestra el texto nuevo.
5. Se valida el monto contra el mínimo del producto (**AP-CU121-03**).
6. Se consulta el saldo al libro. Si el libro no responde: **AP-CU121-07** y **no se
   reserva nada** (no se invierte «por las dudas»). Si la cuenta no es del titular o es
   de otra moneda: **AP-CU121-04**.
7. Se compara el monto con lo **invertible** = disponible − comprometido en pozos.
   Si lo supera: si cabía en el disponible pero no en lo invertible, es un saldo
   afectado a un pozo (**AP-CU121-05**, con el invertible en el detalle); si no,
   saldo insuficiente (**AP-CU121-04**).
8. En **una sola transacción** se guarda el consentimiento inmutable (titular,
   versión, hash del texto, clave, huella), la orden `CREADA`, la instrucción
   `RETENER` pendiente y el evento `inversiones.orden_creada` en el outbox.
9. Se avanza la saga (CU-122): se aplica `RETENER` en el libro, **fuera de toda
   transacción** y con clave de idempotencia determinista, y la orden pasa a `RETENIDA`.
10. Se responde `201` con la orden **en el estado en que quedó** (`CREADA`, `RETENIDA`,
    `ENVIADA`, `INCIERTA`, `CONFIRMADA` o `RECHAZADA`) y un mensaje claro: un estado
    intermedio se muestra, no se esconde.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | La moneda no es `BOB` | `AP-CU121-08`; no se escribe nada |
| 2a | Misma clave y mismo contenido (reintento, o doce pedidos simultáneos) | Se devuelve la orden original: una orden, una posición y un solo débito en el libro (clave única por titular) |
| 2b | Misma clave y **otro** monto o contenido | `AP-CU121-06`; no se crea nada nuevo |
| 3a | Producto inexistente, inactivo o sin versión vigente | `AP-CU121-01` |
| 4a | Las condiciones cambiaron (versión u hash viejos) | `AP-CU121-02`: hay que volver a aceptar con el texto nuevo |
| 5a | Monto bajo el mínimo del producto | `AP-CU121-03`; con el mínimo exacto, pasa |
| 6a | El libro no puede informar el saldo | `AP-CU121-07`; falla cerrada, ninguna reserva |
| 6b | La cuenta es de otra persona | `AP-CU121-04`: objeto propio, no solo rol |
| 7a | Parte del saldo está afectada a un pozo | `AP-CU121-05` y no se crea orden; en el borde exacto de lo invertible, pasa |
| 7b | Dos órdenes **distintas** que juntas superan el saldo | Una se concreta y la otra se rechaza por saldo; la retención atómica del libro es el control final y el libro no queda en negativo |
| 9a | El libro rechaza la retención por saldo insuficiente | La orden queda `RECHAZADA` con motivo `SALDO_INSUFICIENTE` |

## Postcondiciones

- Hay un consentimiento guardado contra la versión exacta y el hash del texto que la
  persona aceptó; no se edita.
- El importe está retenido en el libro: no está disponible, y todavía no se debitó
  definitivamente.
- Ninguna orden se envía al aliado sin retención previa (el orden de la saga es el de
  menor dolor).
- El saldo comprometido en pozos quedó intacto.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU121 = z.object({
  claveIdempotencia: z.string().uuid(), // cabecera Idempotency-Key
  productoCodigo: z.string().max(40),
  versionCondicionesId: z.string().uuid(),
  textoHash: z.string().regex(/^[0-9a-f]{64}$/), // el hash del texto que la persona vio y aceptó
  cuentaBilleteraId: z.string().uuid(),
  monto: MontoSchema, // cadena decimal, nunca number
}).strict()

export const SalidaCU121 = z.object({
  ordenId: z.string().uuid(),
  estado: z.enum(['CREADA', 'RETENIDA', 'ENVIADA', 'INCIERTA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA']),
  productoCodigo: z.string(),
  monto: MontoSchema,
  versionCondicionesId: z.string().uuid(),
  consentimientoId: z.string().uuid(),
  posicionId: z.string().uuid().optional(),
  fechaValor: z.string().optional(),
  motivoRechazo: z.string().optional(),
  mensaje: z.string(),
}).strict()

export const ErroresCU121 = {
  PRODUCTO_NO_DISPONIBLE: 'AP-CU121-01',
  CONDICIONES_DESACTUALIZADAS: 'AP-CU121-02',
  MONTO_BAJO_EL_MINIMO: 'AP-CU121-03',
  SALDO_INSUFICIENTE: 'AP-CU121-04',
  SALDO_AFECTADO_A_POZO: 'AP-CU121-05',
  IDEMPOTENCIA_CONTENIDO_DISTINTO: 'AP-CU121-06',
  NO_SE_PUDO_VERIFICAR_SALDO: 'AP-CU121-07',
  MONEDA_NO_SOPORTADA: 'AP-CU121-08',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `PRODUCTO_NO_DISPONIBLE` | El producto no existe, no está activo, no tiene versión vigente o es sintético en producción |
| `CONDICIONES_DESACTUALIZADAS` | La versión o el hash aceptados ya no son los vigentes |
| `MONTO_BAJO_EL_MINIMO` | El monto es menor al mínimo de la versión vigente |
| `SALDO_INSUFICIENTE` | No alcanza el saldo disponible, o la cuenta no es del titular o es de otra moneda |
| `SALDO_AFECTADO_A_POZO` | El monto cabe en el disponible pero no en lo invertible: parte está comprometida en un pozo |
| `IDEMPOTENCIA_CONTENIDO_DISTINTO` | La clave ya se usó con otra orden distinta |
| `NO_SE_PUDO_VERIFICAR_SALDO` | El libro no respondió: no se reservó nada |
| `MONEDA_NO_SOPORTADA` | La moneda no es bolivianos |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `SaldoDelTitular.invertible` | Disponible menos comprometido en pozos, nunca negativo |
| Átomo | huella del pedido | SHA-256 del contenido canónico del pedido: distingue reintento de otro pedido |
| Molécula | `OrdenRepositorio` | Consentimiento inmutable y orden; la clave es única por titular |
| Molécula | `InstruccionRepositorio` | La intención de cada efecto en el libro, persistida antes de pedirlo |
| Molécula | `LibroDelTitular` (puerto) · `LibroPorHttp` | Saldo, retener y liberar contra `nucleo-financiero`, con timeout y falla cerrada |
| Organismo | `CU121OrdenarInversion` | Valida, guarda y arranca la saga |
| Página | `POST /inversiones/ordenes` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.orden_creada` | La saga de CU-122: retener, enviar y confirmar | `BILLETERA_OPERAR` |

El permiso reutiliza los de la billetera porque invertir es mover saldo del titular;
un código propio (`INVERSION_OPERAR`) exige sembrarlo en el catálogo de roles y es una
decisión abierta del servicio (ver README).

## Interfaz

- **App:** pantalla «Invertir»: muestra el texto de condiciones tal cual se aceptará
  (con fecha y fuente), pide aceptar, y presenta la orden en su estado real, incluidos
  los intermedios. Pendiente de especificar en el frontend; el contrato ya trae el
  `mensaje` en voz clara para cada estado.
- **Backoffice:** ninguna acción manual en este caso.

## Restricciones aplicables

`R-INV-02`

## Evidencia que deja

`consentimiento_inversion` · `orden_inversion` · `instruccion_libro` · `evento_dominio`
(outbox). La retención misma queda en el libro de `nucleo-financiero` (CU-13), que este
servicio solo ve por contrato.

## Criterios de aceptación

```gherkin
Dado saldo libre
Cuando invierte voluntariamente
Entonces reserva el importe y guarda el consentimiento con la version y el hash que acepto

Dada la misma orden enviada dos veces con la misma clave
Cuando se repite
Entonces devuelve la original y no duplica orden, retencion ni posicion

Dada la misma clave con OTRO monto
Cuando se repite el pedido
Entonces se rechaza AP-CU121-06 y no se crea nada nuevo

Dado un saldo afectado a un pozo
Cuando intenta invertirlo
Entonces rechaza AP-CU121-05 y no crea orden; en el borde exacto de lo invertible, pasa

Dado saldo insuficiente sin pozo de por medio
Cuando ordena mas de lo que tiene
Entonces AP-CU121-04, no AP-CU121-05

Dada una cuenta de otra persona
Cuando se la usa para invertir
Entonces AP-CU121-04: no se puede usar (objeto propio, no solo rol)

Dado un monto bajo el minimo del producto
Cuando ordena
Entonces AP-CU121-03; con el minimo exacto, pasa

Dadas condiciones que cambiaron (version o hash viejos)
Cuando ordena
Entonces AP-CU121-02: hay que volver a aceptar

Dada una moneda que no es BOB o un producto inexistente
Cuando ordena
Entonces AP-CU121-08 en el primer caso y AP-CU121-01 en el segundo

Dado el libro caido al verificar el saldo
Cuando ordena
Entonces AP-CU121-07 y no se reserva ni se crea nada (falla cerrada)
```

Además, las pruebas de concurrencia (doce pedidos simultáneos con la misma clave dejan
una orden, una posición y un débito; dos órdenes distintas que juntas superan el saldo)
y el rechazo de la base por `R-INV-02` (una orden cuyo consentimiento es de otra
persona).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-02 | Tasas, plazos, base de días, mínimos y costos **reales** de cada producto. Hoy el mínimo, los costos y la moneda soportada (solo `BOB`) son sintéticos | Aliado + producto |

Dependencia con el núcleo: el saldo con `titularId` y `comprometidoEnPozos`, el motivo
de retención de inversión y las operaciones de débito y crédito de inversión **no
existen todavía en el productor**. El pedido de contrato está en
`evidencia/carril-D/contrato-requerido-nucleo.md`. Mientras no exista, este caso solo
corre contra el doble del libro y no se puede activar contra un núcleo real.

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-122 Confirmar la posición sin duplicar el saldo]] · [[CU-123 Devengar y valorar una posición de inversión]] · [[CU-124 Solicitar el rescate de una inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]]
