---
tags:
  - caso-uso
  - modulo/02-grupos-cupos-turnos-y-gobernanza
  - modulo/01-identidad-usuarios-y-seguridad
codigo: CU-69
criticidad: media
actores: [Participante, Organizador, Invitado, Sistema]
normas: [Protección de datos, ASFI Consumidor Financiero, antifraude]
---

# CU-69 — Invitar a un contacto y registrar sus referencias

> **Objetivo.** Que el pasanaku siga creciendo como crece de verdad —por gente que
> se conoce— sin que la invitación se convierta ni en una filtración de datos ni en
> un canal de spam.

## Actores y disparador

- **Actor principal:** participante u organizador del grupo.
- **Disparadores:** cupo libre en un grupo; armado de un grupo nuevo; alta que
  requiere referencias personales; búsqueda de avalista.

## Precondiciones

1. Quien invita es participante activo del grupo, o su organizador.
2. El grupo tiene cupos libres o está en conformación.
3. Existe [[politica_token]] con vencimiento y usos para el token de invitación.

## Flujo principal

1. Se crea [[invitacion]] con `grupo_id`, `telefono_invitado`, `nombre_sugerido`,
   `emisor_id`, `canal`, `fecha_expiracion` y un [[token_verificacion]] **de un solo
   uso** ligado en `token_id`. Estado inicial `ENVIADA`.
2. Se despacha por [[CU-80 Despachar una notificación]] con plantilla aprobada. El
   mensaje dice **quién** invita y a **qué** grupo, con el monto y la periodicidad:
   nadie debería aceptar un compromiso de dinero sin saber cuál.
3. **El mensaje no revela datos de los integrantes**: ni nombres completos, ni
   teléfonos, ni montos individuales. Quien invita ya conoce al invitado; el sistema
   no tiene por qué presentar a nadie más.
4. `envios_realizados` cuenta los reenvíos, con tope. Superado, la invitación no se
   puede reenviar: insistir tres veces es recordar, insistir diez es acoso.
5. El invitado abre el enlace, se registra si hace falta
   ([[CU-01 Registro y apertura de billetera]]) y acepta. **En la misma
   transacción** se consume el token, la invitación pasa a `ACEPTADA` con
   `fecha_respuesta` y se encadena con
   [[CU-68 Postular a un grupo y ser emparejado]] para la asignación del cupo.
6. **Referencias personales.** El usuario registra [[referencia_personal]] con
   `nombre`, `telefono`, `relacion` y `acepta_ser_avalista`. Cada referencia se
   verifica por un mensaje que **la referencia misma responde**: sin esa
   confirmación queda `verificada = false` y no cuenta como respaldo.
7. Una referencia que aceptó ser avalista puede constituirse en
   [[aval_participante]] con su tope firmado ([[CU-26 Ejecutar el aval y subrogar la deuda]]).
   **Nadie queda de avalista por figurar en la agenda de otro.**

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 1a | El teléfono ya es usuario y participa del grupo | No se envía nada; se informa a quien invita, sin revelar más |
| 2a | El invitado está en lista de supresión | No se envía (`R-NOT-03`); a quien invita se le dice que no fue posible, sin explicar por qué |
| 4a | Se supera el tope de reenvíos | La invitación no se puede reenviar; queda `EXPIRADA` al vencer |
| 5a | El token vencido o ya usado | Se ofrece pedir una invitación nueva; **el token no se reactiva** |
| 5b | El invitado acepta y el cupo ya no está | Pasa a lista de espera del grupo, avisándole con claridad qué significa |
| 6a | La referencia no responde la verificación | Queda sin verificar; el usuario puede reemplazarla. No se insiste más de lo que fija el tope |
| 6b | La referencia pide no ser contactada | Se elimina el vínculo y su teléfono va a [[lista_supresion]] |
| — | Varias invitaciones al mismo teléfono desde grupos distintos | Se aplica el tope por destinatario y día (`R-NOT-02`), no por grupo |
| 2b | `identidad` rechaza emitir el enlace (canal que la política de token vigente no permite, tope diario) | La respuesta es ese mismo rechazo (422 con su `AP-CU69-xx`), nunca un 500 «volvé a intentar»: reintentar no lo arregla |
| 2c | La invitación no llega a existir (ya hay otra vigente) tras emitir el enlace | El enlace recién emitido se revoca; si no se pudo revocar queda registro, sin datos personales |
| 5c | El canje no puede completarse localmente (postulación pendiente, sin cupos, KYC o restricción, invitación revocada o vencida) | Se rechaza **antes** de consumir el enlace: el token no se quema y el emisor puede revocar o reemitir |
| — | Invitación usada para captar clientes masivamente | El patrón se detecta y se limita: el canal de invitación es entre conocidos, no un canal comercial |

> **Política de token `INVITACION_GRUPO` (versionada).** Los canales que acepta la emisión salen de la política con
> `vigente_desde` más reciente que ya rige (`politica_token`). Agregar `ENLACE` —el canal por omisión de la app— es una
> versión nueva (`seeders/minimos/13-politicas-de-token.json`, vigente desde 2026-10-11), no una edición de la fila
> sembrada: la siembra es `DO NOTHING` y una base poblada conservaría la fila vieja. Una base ya poblada la recibe con
> `despliegue/ensayos/politica_invitacion_test.sh` (idempotente).

> **Interruptor de admisión humana** (`aportaya.grupos.admision.humana-obligatoria`, decisión adoptada por delegación del dueño en [[ADR-052 Decisiones adoptadas por delegación del dueño|ADR-052]] DD-2: `true` en producción y `false` en TEST; pendiente de ratificación de negocio y cumplimiento). Encendido, aceptar por enlace (que crearía al participante ACTIVO sin expediente) se rechaza y el camino es el canje, que deja una solicitud pendiente de backoffice; apagado conserva el comportamiento de TEST. Producción debe fijar `ADMISION_HUMANA_OBLIGATORIA=true`.

## Enlace del grupo (QR) — 2026-10-19

Además de la invitación a un teléfono, el organizador puede compartir **un enlace del grupo** —con su
QR— que sirve para cualquier persona. Decisión del dueño (2026-10-19): **el organizador aprueba** a
cada persona, y el enlace sirve **hasta llenarse los cupos o 7 días**, o hasta que el organizador lo
anule.

1. El organizador (administrador del grupo) genera [[enlace_grupo]]: un código aleatorio del que la
   base guarda **solo el hash** (`token_hash`), `estado = ACTIVO` y `fecha_expiracion` a 7 días. Un
   grupo tiene **un solo** enlace activo: generar otro anula el anterior.
2. El enlace se comparte como `https://<app>/g/<código>` y como QR. En un Android con la app
   instalada abre la app (App Links); sin la app abre la app web en la misma pantalla.
3. Quien lo abre, con sesión, ve el grupo (nombre, aporte, periodicidad, cupos libres) **sin datos
   de los integrantes**, y pide unirse.
4. Pedir unirse crea una [[solicitud_ingreso]] `PENDIENTE` con `enlace_grupo_id` y el
   nombre completo (`nombre_visible`) de quien pide, de su cuenta en `identidad`. Solo el nombre:
   ni teléfono ni correo (decisión del dueño 2026-10-20; `telefono_contacto` y `correo_contacto`
   quedan sin uso), y la app se lo avisa a quien pide antes de tocar «Pedir unirme». Con las mismas
   precondiciones de [[CU-68 Postular a un grupo y ser emparejado]] (KYC, restricciones,
   reputación, cupos). El organizador la acepta o la rechaza (CU-68).
5. Repetir el pedido devuelve la misma solicitud pendiente: no se crean dos.

| # | Situación | Resultado |
| :-: | --- | --- |
| E1 | Enlace vencido, anulado o inexistente | `ENLACE_INVALIDO`; el organizador puede generar otro |
| E2 | El grupo ya no tiene cupos libres | `SIN_CUPOS_LIBRES` |
| E3 | Quien pide ya participa del grupo | `YA_ES_PARTICIPANTE` |
| E4 | Quien genera o anula no administra ese grupo | `EMISOR_NO_HABILITADO` |

## Postcondiciones

- Toda incorporación por invitación tiene emisor identificado y token consumido una
  sola vez.
- Ninguna referencia figura como respaldo sin haberlo confirmado ella misma.

## Contrato · `openapi/grupos.yaml`

```ts
export const EntradaCU69 = z.object({
  grupoId: z.string().uuid(),
  telefonoInvitado: z.string().regex(/^\+591\d{8}$/),
  nombreSugerido: z.string().max(80).nullable(),
  canal: z.enum(['SMS','WHATSAPP','ENLACE']),
}).strict()

export const EntradaReferenciaCU69 = z.object({
  nombre:   z.string().min(3).max(120),
  telefono: z.string().regex(/^\+591\d{8}$/),
  relacion: z.enum(['FAMILIAR','AMIGO','LABORAL','VECINO']),
  aceptaSerAvalista: z.boolean(),
}).strict()

export const SalidaCU69 = z.object({
  invitacionId: z.string().uuid().nullable(),
  referenciaId: z.string().uuid().nullable(),
  estado: z.enum(['ENVIADA','ACEPTADA','RECHAZADA','EXPIRADA','NO_ENVIABLE']),
  enviosRealizados: z.number().int(),
  expiraEn: z.string().datetime().nullable(),
  verificada: z.boolean().nullable(),
}).strict()

export const ErroresCU69 = {
  SIN_CUPOS_LIBRES:      'AP-CU69-01',
  DESTINATARIO_SUPRIMIDO:'AP-CU69-02',
  YA_ES_PARTICIPANTE:    'AP-CU69-03',
  TOPE_REENVIOS:         'AP-CU69-04',
  TOKEN_INVALIDO:        'AP-CU69-05',
  REFERENCIA_DUPLICADA:  'AP-CU69-06',
  EMISOR_NO_HABILITADO:  'AP-CU69-07',
  LIMITE_DIARIO_DE_INVITACIONES: 'AP-CU69-08',
  INVITACION_VIGENTE_EXISTENTE:  'AP-CU69-09',
  ADMISION_HUMANA_OBLIGATORIA:   'AP-CU69-10',
  ENLACE_INVALIDO:       'AP-CU69-11',
  GRUPO_TODAVIA_NO_ABIERTO: 'AP-CU69-12',
  SIN_FIRMA_PENDIENTE:   'AP-CU69-13',
  NO_ES_INTEGRANTE:      'AP-CU69-14',
  LIMITE_DE_PARTICIPACIONES: 'AP-CU69-15',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `SIN_CUPOS_LIBRES` | El grupo está completo y no admite lista de espera |
| `DESTINATARIO_SUPRIMIDO` | El teléfono está en [[lista_supresion]]; se responde sin revelar el motivo |
| `YA_ES_PARTICIPANTE` | Ya participa del grupo |
| `TOPE_REENVIOS` | Se agotaron los reenvíos permitidos |
| `TOKEN_INVALIDO` | Vencido o ya consumido; se ofrece pedir otro |
| `REFERENCIA_DUPLICADA` | El mismo teléfono ya figura como referencia del usuario |
| `EMISOR_NO_HABILITADO` | Quien invita no es participante activo ni organizador del grupo |
| `LIMITE_DIARIO_DE_INVITACIONES` | `identidad` alcanzó el tope diario de invitaciones del emisor |
| `INVITACION_VIGENTE_EXISTENTE` | Ese teléfono ya tiene una invitación vigente: revocarla antes de emitir otra |
| `ADMISION_HUMANA_OBLIGATORIA` | Con el interruptor de admisión humana encendido, la aceptación directa se cierra |
| `ENLACE_INVALIDO` | El enlace del grupo venció, fue anulado o no existe |
| `SIN_FIRMA_PENDIENTE` | Quien firma no es integrante del grupo o no tiene una firma pendiente en él |
| `LIMITE_DE_PARTICIPACIONES` | Quien pide entrar (o a quien se acepta) ya participa, sin organizarlos, en tantos grupos vivos como el tope `aportaya.grupos.maximo-de-participaciones` (4, decisión del dueño 2026-10-20) |
| `NO_ES_INTEGRANTE` | Quien pide el detalle del grupo no es integrante: no ve sus cupos ni sus nombres |
| `GRUPO_TODAVIA_NO_ABIERTO` | El organizador quiere crear el enlace de un grupo que todavía no está abierto a inscripción (sigue en `BORRADOR` mientras se fija su precio); un enlace así no le serviría a nadie |

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Átomo | `normalizarTelefono(texto)` | Formato E.164 boliviano; puro |
| Átomo | `puedeInvitar(emisor, grupo)` | Habilitación del emisor; puro |
| Molécula | `InvitacionRepositorio` · `ReferenciaRepositorio` | Persistencia, unicidad y conteo de reenvíos |
| Molécula | `EmisorDeTokens` | Token de un solo uso con vencimiento y registro de intentos |
| Organismo | `CU69Invitar` · `CU69AceptarInvitacion` | Transacción: consumo de token, estado y encadenamiento con el ingreso |
| Página | `POST /grupos/:id/invitaciones` · `POST /invitaciones/:token/aceptar` · `POST /referencias` | Traduce y delega |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `invitacion.enviada` | Notificación con plantilla aprobada y tope diario | `PARTICIPANTE` |
| `invitacion.aceptada` | Postulación al cupo y aviso al emisor | — |
| `referencia.verificada` | Habilita constituirla en aval | — |
| — | Trabajo que expira invitaciones vencidas y limpia tokens | — |

## Interfaz

- **App:** *Grupo → Invitar*: elegir un contacto, ver el mensaje exacto que se va a
  enviar antes de mandarlo, y el estado de cada invitación. En *Mis referencias*, el
  estado de verificación de cada una y si aceptó ser avalista.
- **Backoffice:** invitaciones por grupo con tasa de aceptación, y control de abuso
  del canal.

## Restricciones aplicables

`R-NOT-01` · `R-NOT-02` · `R-NOT-03` · `R-GRP-15` · `R-SEG-03` · `R-AUD-04`

## Evidencia que deja

[[invitacion]] · [[referencia_personal]] · [[token_verificacion]] ·
[[intento_validacion_token]] · [[envio_notificacion]] · [[aval_participante]] ·
`evento_dominio` ·
[[alcance_invitacion]] · [[enlace_grupo]] · [[solicitud_ingreso]]

## Criterios de aceptación

```gherkin
Dado el organizador de un grupo con cupos libres
Cuando genera el enlace del grupo
Entonces recibe un código que vence a los 7 días y la base guarda solo su hash

Dado un grupo que todavia esta en BORRADOR
Cuando el organizador genera el enlace
Entonces recibe AP-CU69-12 y no queda ningun enlace

Dada una persona que ya participa en el maximo de grupos vivos
Cuando intenta entrar a otro
Entonces recibe AP-CU69-15

Dada una persona que organiza grupos o ya salio de otros
Cuando se cuentan sus participaciones
Entonces solo cuentan los grupos vivos donde participa sin organizar

Dado un integrante aceptado que todavia no firmo
Cuando firma el reglamento vigente
Entonces queda ACTIVO, su cupo OCUPADO y el detalle lo muestra FIRMADO

Dado un grupo con todos sus cupos reservados
Cuando firma el ultimo integrante
Entonces el grupo queda CONFORMADO con un periodo por cupo y el organizador puede sortear

Dado un grupo conformado por las firmas
Cuando el organizador compromete y revela el sorteo
Entonces cada integrante tiene un turno distinto en el detalle

Dado el organizador de un grupo viejo, pendiente de firma y sin cupo
Cuando firma
Entonces ocupa el primer cupo libre, figura FIRMADO y el grupo cuenta dos cupos ocupados

Dado el organizador que ya firmo y quedo ACTIVO sin cupo
Cuando abre el grupo y vuelve a firmar
Entonces se le pide firmar, recupera un cupo y su firma no se duplica

Dado un grupo sin cupos libres y el organizador sin cupo
Cuando firma
Entonces se rechaza con AP-CU69-01 y sigue pendiente de firma

Dado un grupo con un enlace activo
Cuando el organizador genera otro
Entonces el anterior queda ANULADO y deja de servir

Dado un enlace activo del grupo
Cuando una persona verificada pide unirse
Entonces queda una solicitud PENDIENTE con su nombre visible para el organizador

Dada una persona con una solicitud PENDIENTE por el enlace
Cuando vuelve a pedir unirse
Entonces recibe la misma solicitud y no se crea otra

Dado un enlace vencido o anulado
Cuando alguien pide unirse
Entonces se rechaza con ENLACE_INVALIDO

Dado un participante que no administra el grupo
Cuando intenta generar o anular su enlace
Entonces se rechaza con EMISOR_NO_HABILITADO

Dado un enlace ya aceptado
Cuando la misma persona repite con la MISMA clave
Entonces recibe la misma participación y no se duplica nada

Dado un enlace sin aceptación previa
Cuando se pregunta por una repetición
Entonces no hay participación que devolver

Dado un enlace ya consumido
Cuando se intenta aceptar con OTRA clave
Entonces se rechaza con TOKEN_INVALIDO

Dado un enlace ya aceptado por una persona
Cuando otra persona intenta con la MISMA clave
Entonces se rechaza con TOKEN_INVALIDO y no recibe la participación ajena

Dado un participante activo y un grupo con cupos libres
Cuando invita a un teléfono nuevo
Entonces existe una invitacion ENVIADA con token de un solo uso
Y el mensaje no contiene datos de los otros integrantes

Dado un token de invitación ya consumido
Cuando se intenta aceptar otra vez
Entonces se rechaza con TOKEN_INVALIDO

Dado un teléfono en lista de supresión
Cuando se lo intenta invitar
Entonces no se envía nada y se responde sin revelar el motivo

Dada una referencia personal registrada
Cuando la referencia no responde la verificación
Entonces queda con verificada en false y no puede constituirse en aval

Dada una invitación ENVIADA y vigente a un número en un grupo
Cuando se emite otra al mismo número con otra clave
Entonces se rechaza y no reemplaza a la anterior
Y la anterior sigue ENVIADA y el grupo tiene una sola invitación

Dada una invitación vigente a un número en un grupo
Cuando se invita al mismo número en otro grupo
Entonces la invitación se emite sin chocar con la regla de reemisión

Dada una invitación ENVIADA
Cuando su emisor la revoca
Entonces queda REVOCADA con fecha de respuesta y se emite el evento de revocación
Y se puede emitir otra invitación al mismo número

Dada una invitación ENVIADA
Cuando su emisor la revoca dos veces
Entonces queda REVOCADA sin error
Y hay un solo evento de revocación

Dada una invitación ENVIADA emitida por un participante
Cuando otro participante del grupo intenta revocarla
Entonces se rechaza y la invitación sigue ENVIADA

Dada una invitación ya ACEPTADA
Cuando su emisor intenta revocarla
Entonces se rechaza y la invitación sigue ACEPTADA

Dada una invitación cuya fecha de expiración ya pasó
Cuando se emite otra al mismo número
Entonces la vencida se cierra como EXPIRADA
Y la nueva queda ENVIADA

Dado un invitado con una postulación pendiente en el grupo
Cuando canjea una invitación vigente de ese grupo
Entonces se rechaza con AP-CU68-06 y el enlace no se consume en identidad
```

## Ver también

[[CU-01 Registro y apertura de billetera]] · [[CU-26 Ejecutar el aval y subrogar la deuda]] · [[CU-68 Postular a un grupo y ser emparejado]] · [[CU-80 Despachar una notificación]]
