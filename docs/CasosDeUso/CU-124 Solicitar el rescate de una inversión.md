---
tags:
  - caso-uso
  - modulo/15-inversiones-voluntarias
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-124
criticidad: alta
actores: [Usuario, Sistema, Aliado]
normas: [Rechazo de doble disponibilidad, Idempotencia de dinero]
---

# CU-124 — Solicitar el rescate de una inversión

> **Objetivo.** Pedir el rescate sin poder comprometer dos veces lo mismo, informando el
> **corte** aplicable y sin dar el dinero por disponible hasta que el aliado confirme
> los fondos.

> [!warning] Inversión de grupos DESACTIVADA y parámetros SINTÉTICOS
> Ver [[CU-120 Consultar productos de inversión y sus condiciones]]. Plazos de rescate,
> hora de corte, penalización de cancelación anticipada y calendario de días hábiles
> aplicados acá son los de las condiciones sintéticas que la persona aceptó.

## Actores y disparador

- **Actor principal:** el usuario titular de la posición (`BILLETERA_OPERAR`).
- **Tercero externo:** el aliado, por el puerto `AliadoDeInversion`; la referencia es el
  id del rescate, estable en los reintentos.
- **Disparador:** `POST /inversiones/posiciones/{posicionId}/rescates` con
  `Idempotency-Key`.

## Precondiciones

1. La posición existe, es de la persona de la sesión y está `ABIERTA`.
2. Para un fondo, hay cuotas libres: las que quedan menos las ya comprometidas en
   rescates en curso (`SOLICITADO`, `PENDIENTE`, `INCIERTO`).
3. Para un DPF, la política de cancelación anticipada es la de la **versión de
   condiciones que se aceptó**.

## Flujo principal

1. Se calcula la huella del pedido (posición + cuotas, o «todo») y se busca un rescate
   previo por titular + clave. Misma huella: se devuelve el rescate original (y se lo
   sincroniza, [[CU-125 Liquidar el rescate y acreditar al titular]]) **sin volver a escribirle al aliado**. Otra huella:
   **AP-CU124-05**.
2. Se **bloquea la posición** y se la valida: inexistente o ajena, **AP-CU124-06**; no
   abierta, **AP-CU124-03**.
3. **DPF.** No admite cuotas (**AP-CU124-04**). Si hoy es el vencimiento o después, el
   tipo es `VENCIMIENTO`. Si es antes y el producto permite la cancelación anticipada,
   `ANTICIPADO`. Si no la permite: **AP-CU124-01**, con la fecha en que se cobra.
4. **Fondo.** Las cuotas pedidas (por omisión, todas las libres) deben ser mayores que
   cero y tener a lo sumo seis decimales (**AP-CU124-04**) y no superar las libres
   (**AP-CU124-02**). El tipo es `TOTAL` si son todas las de la posición y `PARCIAL` si
   no. Se calcula e **informa el corte**: si el pedido llega un día hábil antes de la
   hora de corte del producto (hora de La Paz) toma el valor de hoy; si llega después, o
   en un día no hábil, el del **próximo día hábil** (un viernes tarde, el lunes).
5. Se inserta el rescate `SOLICITADO` y el evento `inversiones.rescate_solicitado`. Es
   la **base** la que impide la doble disponibilidad (`R-INV-05`): bloquea la posición y
   rechaza si las cuotas pedidas más las ya comprometidas superan las que hay, y no
   deja un segundo rescate vigente del mismo depósito. Si salta, **AP-CU124-02**.
6. **Fuera de la transacción** se pide el rescate al aliado:
   - **confirmó** → se liquida ([[CU-125 Liquidar el rescate y acreditar al titular]]);
   - **pendiente** → `PENDIENTE`, con la fecha valor y la fecha de liquidación que
     informa;
   - **rechazó** → `RECHAZADO` con su motivo; las cuotas vuelven a estar libres;
   - **no se sabe** → `INCIERTO`; las cuotas siguen comprometidas y, al volver el
     aliado, se consulta y se reenvía con la misma referencia.
7. Se responde `201` con el rescate en el estado en que quedó. **Solicitar un rescate
   no significa tener el dinero disponible**: el mensaje lo dice.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | Misma clave, mismo pedido | Devuelve el **mismo** rescate; no vuelve a escribirle al aliado |
| 1b | Misma clave, otro contenido | `AP-CU124-05` |
| 2a | Posición ajena o inexistente · posición ya cerrada | `AP-CU124-06` · `AP-CU124-03` |
| 3a | DPF que **no** admite cancelación anticipada, antes del vencimiento | `AP-CU124-01` y no queda ningún rescate |
| 3b | DPF que sí la admite | Tipo `ANTICIPADO`; se liquida con la penalización y el impuesto del contrato |
| 3c | DPF ya rescatado | La misma clave devuelve el rescate original; otra clave se rechaza porque la posición ya está cerrada |
| 4a | Cuotas ya comprometidas en otro rescate | `AP-CU124-02`; en el borde (lo que queda libre) pasa |
| 4b | Cuotas en cero, con más de seis decimales, o un DPF por cuotas | `AP-CU124-04` |
| 5a | Dos rescates **simultáneos** que juntos superan las cuotas | Solo uno entra (la base serializa por posición) |
| 6a | El aliado rechaza | `RECHAZADO` con su motivo y las cuotas vuelven a estar libres |
| 6b | El aliado está caído | `INCIERTO`; las cuotas siguen comprometidas |

## Postcondiciones

- Las cuotas (o el depósito) comprometidas en un rescate en curso **no se pueden volver a
  ofrecer**.
- La persona sabe el corte aplicable, y el dinero no figura como disponible hasta que
  el rescate se liquida y se acredita.
- Un depósito se rescata una sola vez.

## Contrato · `openapi/inversiones.yaml`

```ts
export const EntradaCU124 = z.object({
  claveIdempotencia: z.string().uuid(), // cabecera Idempotency-Key
  posicionId: z.string().uuid(),        // en la ruta
  cuotas: z.string().regex(/^\d+\.\d{6}$/).optional(), // solo fondos; sin cuotas se rescata todo lo libre
}).strict()

export const SalidaCU124 = z.object({
  rescateId: z.string().uuid(),
  posicionId: z.string().uuid(),
  tipo: z.enum(['PARCIAL', 'TOTAL', 'VENCIMIENTO', 'ANTICIPADO']),
  estado: z.enum(['SOLICITADO', 'PENDIENTE', 'INCIERTO', 'POR_ACREDITAR', 'LIQUIDADO', 'RECHAZADO']),
  cuotas: z.string().optional(),
  corte: z.object({ fechaValor: z.string().date().optional(), liquidaEn: z.string().datetime().optional() }).optional(),
  netoAcreditar: MontoSchema.optional(),
  motivoRechazo: z.string().optional(),
  mensaje: z.string(),
}).strict()

export const ErroresCU124 = {
  RESCATE_ANTICIPADO_NO_PERMITIDO: 'AP-CU124-01',
  DOBLE_DISPONIBILIDAD: 'AP-CU124-02',
  POSICION_NO_ABIERTA: 'AP-CU124-03',
  CUOTAS_INVALIDAS: 'AP-CU124-04',
  IDEMPOTENCIA_CONTENIDO_DISTINTO: 'AP-CU124-05',
  POSICION_INEXISTENTE: 'AP-CU124-06',
  RESCATE_INEXISTENTE: 'AP-CU124-07',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `RESCATE_ANTICIPADO_NO_PERMITIDO` | Un DPF sin cancelación anticipada se pide antes de su vencimiento |
| `DOBLE_DISPONIBILIDAD` | Esas cuotas (o ese depósito) ya están comprometidas en otro rescate (`R-INV-05`) |
| `POSICION_NO_ABIERTA` | La posición ya está cerrada |
| `CUOTAS_INVALIDAS` | Cuotas en cero, con más de seis decimales, o un DPF rescatado por cuotas |
| `IDEMPOTENCIA_CONTENIDO_DISTINTO` | La clave ya se usó con otro pedido de rescate |
| `POSICION_INEXISTENTE` | La posición no existe o es de otra persona |
| `RESCATE_INEXISTENTE` | El rescate no existe o es de otra persona (lo emite la sincronización de [[CU-125 Liquidar el rescate y acreditar al titular]]) |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `CorteDeOrden` | Fecha valor: hoy si es hábil y antes de la hora de corte; si no, el próximo día hábil |
| Átomo | huella del pedido | SHA-256 de posición y cuotas: distingue reintento de otro pedido |
| Molécula | `RescateRepositorio` | Rescates, cuotas en curso y la excepción de doble disponibilidad |
| Molécula | `PosicionRepositorio` | Posición bloqueada para serializar rescates concurrentes |
| Molécula | `CalendarioHabil` (plataforma) | Días hábiles de La Paz |
| Molécula | `AliadoSimuladoHttp` | Pedir el rescate con la referencia estable |
| Organismo | `CU124SolicitarRescate` | Valida, crea el rescate y lo pide al aliado |
| Página | `POST /inversiones/posiciones/{posicionId}/rescates` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `inversiones.rescate_solicitado` | El pedido al aliado y, si confirma, la liquidación de CU-125 | `BILLETERA_OPERAR` |

## Interfaz

- **App:** «Rescatar» desde el detalle de la posición: muestra cuántas cuotas están
  libres, el corte que se aplica, la penalización si es una cancelación anticipada, y el
  estado `PENDIENTE` con la leyenda «el dinero no está disponible hasta que el aliado
  confirme los fondos». Pendiente de especificar en el frontend.
- **Backoffice:** ninguna acción manual en este caso.

## Restricciones aplicables

`R-INV-05`

## Evidencia que deja

`rescate_inversion` · `evento_dominio` (outbox). Cuando se liquida, se suman los datos
de [[CU-125 Liquidar el rescate y acreditar al titular]].

## Criterios de aceptación

```gherkin
Dado un fondo con plazo de rescate
Cuando solicita
Entonces informa el corte y queda PENDIENTE: el dinero no esta disponible hasta que el aliado confirme los fondos

Dado un pedido despues de la hora de corte
Cuando se solicita el rescate
Entonces informa el proximo dia habil; un viernes tarde, el lunes

Dadas cuotas ya comprometidas en otro rescate
Cuando se piden otra vez
Entonces AP-CU124-02; el borde (lo que queda libre) si pasa

Dado un aliado que rechaza el rescate
Cuando se solicita
Entonces queda RECHAZADO con su motivo y las cuotas vuelven a estar libres

Dado el aliado CAIDO al pedir el rescate
Cuando se solicita
Entonces INCIERTO y las cuotas siguen comprometidas; al volver, se consulta y se reenvia con la misma referencia

Dados cuotas en cero o con mas de seis decimales, una posicion ajena o inexistente y una clave reutilizada con otro contenido
Cuando se solicita
Entonces se rechaza con su codigo y no se escribe nada

Dado un DPF que NO admite cancelacion anticipada
Cuando pide rescate antes del vencimiento
Entonces AP-CU124-01 y no queda ningun rescate

Dado un DPF que si admite la cancelacion
Cuando pide rescate anticipado
Entonces el tipo es ANTICIPADO y se liquida con penalizacion e impuesto del contrato

Dado un deposito ya rescatado
Cuando se pide otra vez con la misma clave o con otra
Entonces la misma clave devuelve el rescate original y otra clave se rechaza porque la posicion ya esta cerrada
```

Además, la prueba de reintento (la misma clave dos veces devuelve el mismo rescate y no
vuelve a escribirle al aliado), la de concurrencia (dos rescates simultáneos que juntos
superan las cuotas: solo uno entra) y los rechazos de la base por `R-INV-05` (doble
disponibilidad aunque la aplicación no la vea; un DPF por cuotas y un segundo rescate
vigente del mismo depósito).

## Decisiones abiertas

| ID | Qué falta | Dueño |
| --- | --- | --- |
| DR-INV-05 | Contrato en `nucleo-financiero` para el **crédito de rescate** (y el débito de inversión): la operación `acreditarRescate` que este servicio invoca **no existe todavía en el productor** | Carril A (núcleo) |

Parámetros sin fuente aprobada que afectan a este caso: plazos de rescate, hora de corte
y penalización de cancelación anticipada de cada producto.

## Ver también

[[CU-120 Consultar productos de inversión y sus condiciones]] · [[CU-121 Aceptar las condiciones y ordenar una inversión]] · [[CU-122 Confirmar la posición sin duplicar el saldo]] · [[CU-123 Devengar y valorar una posición de inversión]] · [[CU-125 Liquidar el rescate y acreditar al titular]]
