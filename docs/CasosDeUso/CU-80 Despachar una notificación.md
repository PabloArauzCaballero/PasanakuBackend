---
tags:
  - caso-uso
  - modulo/05-notificaciones-y-comunicaciones
codigo: CU-80
criticidad: alta
actores: [Sistema, Proveedor de mensajería]
normas: [Consumidor financiero, protección de datos, deber de reserva]
---

# CU-80 — Despachar una notificación desde el outbox

> **Objetivo.** Que avisar sea **confiable y no invasivo**: el mensaje sale porque
> ocurrió un hecho registrado, sale una sola vez, y no sale si el destinatario no
> quiere o si la norma manda callar.

## Actores y disparador

- **Actor principal:** el trabajador del outbox.
- **Actor secundario:** proveedor de mensajería (WhatsApp, push, SMS, correo).
- **Disparador:** un `evento_dominio` pendiente con una
  [[evento_notificable]] configurada.

## Precondiciones

1. Existe [[plantilla_mensaje]] con [[version_plantilla]] aprobada para el evento
   y el canal.
2. El destinatario tiene [[canal_vinculado]] verificado.
3. El evento no está marcado como reservado ([[CU-44 De alerta de monitoreo a reporte de operación sospechosa]]
   **no notifica al titular**).

## Flujo principal

1. El trabajador toma el evento con `SELECT … FOR UPDATE SKIP LOCKED`: dos réplicas
   nunca procesan el mismo.
2. Se resuelve el destinatario y se consulta [[preferencia_notificacion]]: canal
   preferido, horario permitido y si optó por no recibir esa categoría.
3. Se verifica [[lista_supresion]]: un número dado de baja no recibe nada, aunque
   el evento lo pida.
4. Se crea [[notificacion]] y se renderiza la plantilla con sus variables. **Los
   datos sensibles no viajan en el cuerpo**: se manda el monto y el concepto, nunca
   el número de cuenta ni el documento.
5. Se encola [[envio_notificacion]] con `clave_idempotencia` derivada de
   `(evento_id, destinatario, canal)`: el reintento del trabajador no duplica el
   mensaje (`R-NOT-01`).
6. El adaptador del proveedor envía y devuelve su identificador; se registran los
   [[evento_entrega_mensaje]] a medida que llegan los acuses (enviado, entregado,
   leído, fallido).
7. Si el mensaje incluye una acción —pagar, confirmar—, el enlace se firma con
   [[token_verificacion]] de un solo uso y vencimiento corto, y queda registrado en
   [[enlace_pago_notificado]].

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | Fuera del horario permitido | Se reprograma para la primera hora hábil: cobrar a las 3 de la mañana es una mala práctica, no una eficiencia |
| 3a | Destinatario en lista de supresión | No se envía; queda constancia del motivo. **Nunca se fuerza un envío suprimido** |
| 6a | El proveedor falla | Reintento con espera exponencial hasta el máximo; agotado, va a [[cola_muerta]] y se avisa al equipo |
| 6b | El proveedor responde éxito y después reporta fallo | El estado se actualiza con el acuse: manda el último evento, no el primero |
| 1a | El evento ya fue notificado | La clave de idempotencia lo corta antes de gastar un envío |
| — | Mensaje regulatorio obligatorio (vencimiento de plazo, cambio de tarifario) | **Ignora la preferencia comercial** pero respeta la supresión legal; se marca `es_obligatorio` |

## Postcondiciones

- Un hecho, un mensaje; con su acuse o su motivo de no envío.
- Ningún dato sensible viajó por el canal.

## Contrato · `openapi/notificaciones.yaml`

```ts
export const EntradaCU80 = z.object({
  eventoDominioId: z.string().uuid(),
  destinatarioId:  z.string().uuid(),
  canal:  z.enum(['WHATSAPP','PUSH','SMS','CORREO','BANDEJA']),
  plantillaCodigo: z.string().max(40),
  variables: z.record(z.string()),
  esObligatorio: z.boolean().default(false),
}).strict()

export const SalidaCU80 = z.object({
  notificacionId: z.string().uuid(),
  envioId: z.string().uuid().nullable(),
  estado: z.enum(['ENCOLADA','ENVIADA','SUPRIMIDA','REPROGRAMADA','FALLIDA']),
  motivoNoEnvio: z.string().nullable(),
  reprogramadaPara: z.string().datetime().nullable(),
}).strict()

export const ErroresCU80 = {
  SIN_PLANTILLA_APROBADA: 'AP-CU80-01',
  CANAL_NO_VERIFICADO:    'AP-CU80-02',
  DESTINATARIO_SUPRIMIDO: 'AP-CU80-03',
  EVENTO_RESERVADO:       'AP-CU80-04',
  FILTRO_DE_LISTADO_INVALIDO: 'AP-CU80-05',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `SIN_PLANTILLA_APROBADA` | No hay plantilla vigente y aprobada para ese evento y canal: no se improvisa el texto |
| `CANAL_NO_VERIFICADO` | El teléfono o el correo del destinatario no está verificado |
| `DESTINATARIO_SUPRIMIDO` | Está en [[lista_supresion]] para esa categoría; **nunca se fuerza el envío** |
| `EVENTO_RESERVADO` | Otra réplica lo tomó con `SKIP LOCKED`; no es falla, es concurrencia sana |
| `FILTRO_DE_LISTADO_INVALIDO` | El listado de plantillas recibió un estado de aprobación desconocido, o `antesDe` sin `antesDeId` (o al revés) |

**Plantillas para Auditoría (backoffice carril A · A6).** `GET /notificaciones/plantillas`
(`listarPlantillas`, `AUDITORIA_LEER`) lista una fila por versión de plantilla con su código, evento,
canal, categoría y estado de aprobación del proveedor, si está activa, idioma y vigencia. Sin filtro trae
todos los estados de aprobación. **No devuelve asunto, cuerpo, botones ni media**: dice qué plantilla rige,
no lo que dice. Pagina por `(vigenteDesde, versionId)` de la más reciente a la más antigua. No hay permiso de
mensajería sembrado; `AUDITORIA_LEER` es el único que aplica.

**Avisos internos para operadores (campana del backoffice).** `GET /avisos-internos`, `POST /avisos-internos/{id}/leido`, `POST /avisos-internos/marcar-todos-leidos`, `POST /avisos-internos`.
Gestiona los avisos persistentes en [[aviso_interno]] dirigidos a un operador o a su rol (ej. alertas de cumplimiento, revisiones de catálogo pendientes o fallas de despliegue).

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `renderizarPlantilla(version, variables)` | Interpolación pura, con escape; falla si falta una variable |
| Átomo | `ventanaDeEnvio(preferencia, ahora)` | Devuelve enviar ahora o cuándo; puro |
| Molécula | `NotificacionRepositorio` · `SupresionRepositorio` · `AvisoInternoRepositorio` | |
| Molécula | `AdaptadorWhatsApp` · `AdaptadorPush` | Un adaptador por proveedor, detrás de la misma interfaz |
| Organismo | `CU80DespacharNotificacion` · `GestionarAvisosInternos` | Consumidor del outbox y gestor de avisos internos |
| Página | `AvisosInternosController` | Campana y bandeja de avisos en backoffice |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `notificacion.enviada` | Actualización de la bandeja del usuario | Interno |
| `notificacion.fallida` | Reintento y, agotado, cola muerta con aviso | — |
| — | Trabajo que reprograma los envíos fuera de horario | — |

## Interfaz

- **App:** [[bandeja_entrada]] con el histórico y el detalle de cada aviso.
- **Backoffice:** campana de avisos en el shell y monitor de envíos con tasa de entrega por canal y la cola muerta.

## Restricciones aplicables

`R-NOT-01` · `R-NOT-02` · `R-SEG-02` · `R-AUD-04`

## Evidencia que deja

[[notificacion]] · [[envio_notificacion]] · [[evento_entrega_mensaje]] ·
[[cola_muerta]] si agota reintentos · [[lista_supresion]] consultada · [[aviso_interno]]

## Criterios de aceptación

```gherkin
Dado un evento de dominio con plantilla aprobada y destinatario verificado
Cuando el trabajador lo procesa
Entonces existe un envio_notificacion con clave de idempotencia
Y el proveedor recibió exactamente un mensaje

Dado el mismo evento reprocesado tras un reinicio
Cuando se consume otra vez
Entonces no se genera un segundo envío

Dado un destinatario en lista de supresión
Cuando se procesa el evento
Entonces el estado es SUPRIMIDA con su motivo y no hay envío

Dado un evento fuera del horario permitido
Cuando se procesa
Entonces queda REPROGRAMADA para la primera hora hábil

# Plantillas para Auditoría (backoffice carril A)
Dada una plantilla aprobada y otra en borrador, cada una con su versión
Cuando Auditoría abre las plantillas
Entonces ve las dos con su estado de aprobación y su versión, sin cuerpo ni asunto

Dadas dos versiones de plantilla
Cuando Auditoría recorre las plantillas de a una
Entonces ve cada versión una sola vez, de la más reciente a la más antigua, y la última página cierra sin cursor
```

## Ver también

[[CU-69 Invitar a un contacto y registrar sus referencias]] · [[CU-70 Registrar un evento de reputación]] · [[CU-81 Programar recordatorios de aporte]] · [[CU-82 Procesar una respuesta entrante]] · [[CU-83 Enrutar el envío por proveedor de mensajería]]
