# Módulo 5 — Notificaciones y Comunicaciones

> **Pregunta de negocio que responde este módulo:**
> *¿Cómo le aviso a la gente que tiene que pagar, de manera que efectivamente
> pague, sin quemar el canal a fuerza de spam y sin gastar de más?*

Este módulo parece accesorio y es, en la práctica, **el motor de cobro de la
plataforma**. En un pasanaku presencial el que cobra es una persona que te ve la
cara: te lo recuerda en el pasillo, te manda un audio, te pregunta delante de los
demás. Esa presión es lo que hace que la gente pague. Digitalmente hay que
reconstruirla, y el canal para hacerlo en Bolivia es WhatsApp.

La realidad operativa que el modelo tiene que soportar —y que casi nadie modela
hasta que le explota en producción:

- Las plantillas de WhatsApp **requieren aprobación previa del proveedor**, y las
  rechazan.
- Fuera de la **ventana de 24 horas** de conversación, solo se pueden enviar
  plantillas aprobadas de categoría UTILITY o AUTHENTICATION.
- El opt-in es obligatorio, y las quejas por spam degradan la reputación del
  remitente **para todos los usuarios**.
- Cada mensaje cuesta plata.
- La gente **contesta** los mensajes ("YA PAGUÉ", "1", "AYUDA") y alguien tiene que
  hacer algo con esas respuestas.

---

## La separación en tres niveles

Antes de las fichas, la decisión de diseño que explica el módulo:

| Nivel | Entidad | Qué representa |
| --- | --- | --- |
| 1 | `Notificacion` | **El hecho a comunicar.** Una por evento y destinatario. "Hay que avisarle a Juan que su aporte vence mañana". |
| 2 | `EnvioNotificacion` | **Un intento por un canal concreto.** "Se lo mandamos por WhatsApp". Si no llega, se crea otro envío por SMS. |
| 3 | `EventoEntregaMensaje` | **Lo que el proveedor reporta después.** "WhatsApp confirma: entregado a las 14:32, leído a las 19:05". |

Un mismo aviso puede tener tres envíos (WhatsApp → SMS → correo) y cada envío
varios eventos de entrega. Aplanar esto en una sola tabla haría imposible medir
qué canal realmente hace pagar a la gente, y cuánto cuesta cada uno.

---

## Paquete: Catálogo de eventos y plantillas

### `EventoNotificable` / `evento_notificable` — Política configurable

**Qué es.** El catálogo de los 20 hechos del negocio que ameritan avisarle a
alguien, cada uno con su política de comunicación.

**Para qué sirve (negocio).** Centraliza decisiones que de otro modo quedarían
dispersas en el código de ocho módulos distintos:

- `prioridad`: un código de verificación es CRÍTICO (bloquea al usuario si no
  llega); un resumen mensual es BAJO.
- `esTransaccional`: **si es transaccional, ignora el horario de no molestar.**
  "Tu entrega se acreditó" o "tu pago fue rechazado" se avisan a la hora que sea.
  Un recordatorio de aporte, no.
- `permiteAgrupacion`: evita mandar cinco mensajes cuando se pueden juntar en uno.
- `ventanaDeduplicacionMin`: cuánto tiempo tiene que pasar para que el mismo aviso
  se considere nuevo y no repetido.
- `cadenaRespaldo`: el orden de canales a intentar. WhatsApp → SMS → correo.

**Por qué debe existir.** Sin catálogo, cada módulo decide por su cuenta cómo
notificar, y el usuario recibe cinco mensajes de cinco estilos distintos por cosas
relacionadas. Además, ajustar la política de comunicación (por ejemplo, dejar de
mandar SMS porque se disparó el costo) requeriría tocar ocho módulos.

---

### `PlantillaMensaje` / `plantilla_mensaje`

**Qué es.** Una plantilla de mensaje para un evento y un canal, con su estado de
aprobación ante el proveedor.

**Para qué sirve (negocio).** Modela una restricción externa que no se puede
ignorar: **WhatsApp exige que las plantillas se registren y se aprueben antes de
poder usarse**, y las rechaza con frecuencia (por tono comercial, por variables
mal definidas, por categoría incorrecta).

`categoriaProveedor` (UTILITY / AUTHENTICATION / MARKETING) tiene consecuencias
económicas y funcionales directas: las de marketing cuestan más, requieren opt-in
más estricto y **no se pueden enviar fuera de la ventana de 24 horas**. Clasificar
mal un recordatorio de cobro como marketing significa no poder enviarlo cuando más
falta hace.

`idPlantillaProveedor` guarda el identificador que devuelve Meta/Twilio: es el que
hay que mandar en cada envío.

`estadoAprobacion = PAUSADA` refleja algo real: el proveedor puede pausar una
plantilla en producción si acumula reportes de spam. Si el sistema no lo sabe,
sigue intentando enviarla y falla en silencio.

**Por qué debe existir.** Sin esta entidad, el sistema intenta enviar mensajes que
el proveedor va a rechazar —y que en algunos esquemas igual se cobran— y no hay
dónde ver qué plantillas están vivas.

---

### `VersionPlantilla` / `version_plantilla`

**Qué es.** El texto concreto de una plantilla, en un idioma, en una versión.

**Para qué sirve (negocio).** Dos razones:

1. **Idioma.** Un recordatorio de cobro en español a alguien que opera en quechua
   o aymara es un recordatorio que no cobra. El modelo soporta versiones por
   idioma de la misma plantilla.
2. **Versionado.** Cuando alguien reclama "a mí nunca me avisaron que había
   recargo", hay que poder reconstruir el texto exacto que se le envió ese día.
   `contenidoEnviado` en el envío guarda el resultado renderizado; la versión
   guarda la plantilla de la que salió.

`botones` (JSON) es lo que habilita el CTA "Pagar ahora" y las respuestas rápidas
del tipo "Ya pagué / Necesito plazo". Ese botón es literalmente la diferencia
entre un mensaje que informa y uno que cobra.

`validarVariables(contexto)` evita el error clásico de mandar "Hola {{1}}, tu
aporte de {{2}} vence el {{3}}" con una variable sin resolver.

**Por qué debe existir.** Sin versiones, cambiar un texto reescribe la historia y
no se puede probar qué se comunicó.

---

### `MensajeRenderizado` — Objeto de valor

**Qué es.** El resultado de aplicar el contexto a una plantilla: asunto, cuerpo,
longitud, cuántos segmentos SMS ocupa.

**Para qué sirve (negocio).** `segmentosSMS` es directamente dinero: un SMS de
161 caracteres cuesta el doble que uno de 160. `truncarPara(canal)` adapta el
mismo contenido a las restricciones de cada canal antes de enviarlo, en vez de
descubrir el problema en la factura.

---

## Paquete: Destinatarios y consentimiento

### `CanalVinculado` / `canal_vinculado`

**Qué es.** Un canal concreto de un usuario —su WhatsApp, su SMS, su correo, su
token push— con su estado de verificación y consentimiento.

**Para qué sirve (negocio).** Es el destinatario real. Tres campos hacen el
trabajo pesado:

- **`optInEn` / `optOutEn`.** El consentimiento es por canal, no por usuario:
  alguien puede aceptar WhatsApp y rechazar SMS. Enviar sin opt-in no solo es
  problema legal, es problema operativo: genera reportes de spam que degradan la
  reputación del remitente y afectan la entregabilidad **de todos los demás
  usuarios**.
- **`ventanaConversacionHasta`.** La ventana de 24 horas de WhatsApp: después de
  que el usuario escribe, hay 24 horas para responderle con mensajes libres;
  pasadas esas horas solo se pueden enviar plantillas aprobadas. Guardar la ventana
  evita intentar envíos que el proveedor va a rechazar **y que en algunos esquemas
  igual se cobran**.
- **`rebotesConsecutivos` + `estado = NO_ALCANZABLE`.** El número cambió de dueño,
  el correo no existe. Seguir escribiendo a un destino muerto cuesta plata y
  ensucia las métricas de entregabilidad.

**Por qué debe existir.** Si el destinatario fuera simplemente `usuario.telefono`,
no habría dónde guardar el opt-in, la ventana de conversación ni el estado de
alcanzabilidad — y el sistema seguiría escribiéndole a quien pidió que no le
escriban.

---

### `ListaSupresion` / `lista_supresion`

**Qué es.** La lista negra de identificadores a los que no se les vuelve a
escribir, por canal.

**Para qué sirve (negocio).** Es la protección de la reputación del remitente.
Tres motivos: queja por spam, rebote duro, solicitud legal. Se consulta **antes de
cada envío**.

La diferencia con el opt-out del `CanalVinculado` es importante: la supresión es
**por identificador, no por usuario**. Si un número reportó spam, no se le escribe
a ese número aunque mañana lo registre otra persona con otra cuenta. Es una
protección a nivel de infraestructura de mensajería, no de relación con el
usuario.

**Por qué debe existir.** Una tasa alta de quejas hace que el proveedor limite o
suspenda la cuenta de envío. Cuando eso pasa, **ningún** usuario recibe
recordatorios, y la cobranza de toda la plataforma se cae.

---

## Paquete: Emisión y entrega

### `Notificacion` / `notificacion` — Raíz de agregado

**Qué es.** El hecho a comunicar a una persona. Una por evento y destinatario.

**Para qué sirve (negocio).** Es la intención: "hay que avisarle a Juan que su
aporte vence mañana". Independiente de por dónde se le avise y de cuántas veces se
intente.

`contexto` (JSON) lleva los datos del hecho: grupo, aporte, monto, fecha. Se
guarda con la notificación, no se recalcula al enviar, porque **el mensaje debe
decir lo que era cierto cuando se generó**. Si el monto cambió entre la generación
y el envío, mandar el nuevo confundiría al usuario.

`claveDeduplicacion` es el campo que resuelve el problema más molesto de todo
sistema de recordatorios: **el bombardeo**. El job de recordatorios se reejecuta
—por un reintento, por un despliegue, por un error— y la persona recibe cinco
mensajes idénticos. Eso genera opt-outs y quejas por spam. La clave (hash de
tipoEvento + usuarioId + aporteId + día) con índice único parcial lo hace
imposible por construcción.

`correlationId` permite trazar: este recordatorio pertenece al mismo flujo que
generó la orden de cobro y que registró el intento de pago (M3, M9).

**Por qué debe existir.** Sin el nivel de "hecho a comunicar" separado del envío,
no se puede escalar a canal de respaldo sin duplicar la notificación, ni deduplicar,
ni mostrar en la bandeja de la app.

**A nivel de sistema.**
`CREATE UNIQUE INDEX ON notificacion (clave_deduplicacion) WHERE estado NOT IN
('CANCELADA','SUPRIMIDA')`. Tabla de alto volumen: particionada por mes sobre
`creada_en`.

---

### `EnvioNotificacion` / `envio_notificacion`

**Qué es.** Un intento de entregar una notificación por un canal concreto, con su
costo y su estado.

**Para qué sirve (negocio).** Es donde vive la **cadena de respaldo** (RF-16):
`orden = 1` WhatsApp; si a los N minutos no hay confirmación de entrega, se crea
`orden = 2` por SMS; después correo. Cada eslabón guarda su propio `costo` y su
propio `estado`.

Eso permite responder la pregunta que decide el presupuesto de la operación:
**¿qué canal realmente hace que la gente pague?** Si el 80% de los pagos ocurre
tras el WhatsApp y el SMS solo agrega 3% a un costo diez veces mayor, la cadena de
respaldo se recorta. Sin costo y estado por eslabón, esa decisión se toma a ciegas.

`contenidoEnviado` guarda el texto exacto que se envió. Es la prueba ante "a mí
nunca me avisaron".

**Por qué debe existir.** Sin envíos separados, no hay respaldo entre canales, ni
costo por canal, ni forma de reintentar un canal sin reintentar todos.

**A nivel de sistema.** `id_mensaje_proveedor` es `UNIQUE`: es la llave con la que
llegan los webhooks de estado. `proximo_reintento_en` indexado para el barrido de
reintentos.

---

### `EventoEntregaMensaje` / `evento_entrega_mensaje`

**Qué es.** Cada callback del proveedor sobre el destino de un mensaje: enviado,
entregado, leído, fallido, rechazado.

**Para qué sirve (negocio).** Es la única fuente confiable de si el mensaje llegó.
"Enviado" no es "entregado" y "entregado" no es "leído". Esa distinción es la que
alimenta la cadena de respaldo: si a los 15 minutos no hay `delivered`, se escala
al siguiente canal.

El estado `leído` tiene un uso adicional en cobranza: **alguien que lee los
recordatorios y no paga es un caso distinto de alguien que no los lee**. El
primero es una decisión; el segundo puede ser un problema de canal. El módulo 8
usa exactamente esa señal (`NO_ABRE_MENSAJES` como alerta temprana).

**Por qué debe existir.** Los proveedores reintentan sus callbacks.
`claveIdempotencia` `UNIQUE` evita aplicar dos veces el mismo evento y contar dos
entregas donde hubo una.

**A nivel de sistema.** `payloadCrudo` completo: ante disputa con el proveedor,
vale lo que él mandó.

---

### `ProveedorMensajeria` / `proveedor_mensajeria`

**Qué es.** Cada proveedor de mensajería con el que se opera, con su costo, su
límite de tasa y su salud.

**Para qué sirve (negocio).** `costoPorMensaje` permite calcular el costo real de
la cobranza por grupo — un número que la mayoría de las plataformas descubre tarde.
`limiteMensajesPorSegundo` evita que el envío masivo del día de vencimiento haga
que el proveedor rechace la mitad de los mensajes por exceso de tasa.
`saludPorcentaje` + `estaSaludable()` permiten enrutar a un proveedor alternativo
cuando el principal se degrada, justo el día de mayor volumen.

**Por qué debe existir.** Con un solo proveedor hardcodeado, su caída es la caída
del cobro de todos los grupos ese día.

---

### `ColaEnvio` / `cola_envio`

**Qué es.** La cola de trabajo de los envíos pendientes.

**Para qué sirve (negocio).** El pico de envíos es brutalmente concentrado: todos
los grupos con día de cobro 5 generan sus recordatorios el día 3 a la misma hora.
La cola con `particion` y `disponibleEn` distribuye ese pico para no saturar al
proveedor ni exceder su límite de tasa.

`bloqueadaHasta` implementa el lease: dos workers no toman el mismo envío y no se
manda el mensaje dos veces.

**Por qué debe existir.** Sin cola persistente, un reinicio del servicio en pleno
envío pierde los mensajes pendientes y nadie recibe su recordatorio ese día.

---

### `ColaMuerta` / `cola_muerta`

**Qué es.** Los envíos que fallaron definitivamente, con su payload, para revisión
manual.

**Para qué sirve (negocio).** Un mensaje que falla para siempre y desaparece es un
usuario que no se enteró de que tenía que pagar y al que después se le va a cobrar
mora. La cola muerta hace visible ese fallo: alguien puede revisarlo, corregir el
canal y reprocesar.

**Por qué debe existir.** Sin ella, los fallos permanentes son invisibles y se
descubren como reclamos.

---

## Paquete: Interacción y acción directa

### `EnlacePagoNotificado` / `enlace_pago_notificado`

**Qué es.** El enlace de pago que viaja dentro de una notificación, con su
seguimiento de clics y conversión.

**Para qué sirve (negocio).** Es la materialización del **"pago en un toque"**
(RF-16) y, sobre todo, **la métrica que dice si el módulo funciona**:
`clicks` vs `convertidoEnPago` es el embudo del recordatorio. Si mucha gente toca
el enlace y poca paga, el problema está en la pantalla de pago, no en el mensaje.
Si poca gente toca, el problema está en el mensaje.

**Por qué debe existir.** Sin esta entidad no se puede atribuir un pago al
recordatorio que lo causó, y por lo tanto no se puede saber si la inversión en
mensajería se justifica.

**A nivel de sistema.** `token_id` → `TokenEnlaceFirmado` (M1): HMAC, uso único.
`orden_cobro_id` → M3. El enlace no expone datos del grupo.

---

### `RespuestaEntrante` / `respuesta_entrante`

**Qué es.** Lo que el participante contesta por el canal, con la intención
detectada.

**Para qué sirve (negocio).** Esta entidad reconoce algo que todo sistema de
notificaciones descubre a la mala: **la gente contesta**. Escriben "YA PAGUÉ",
"cuánto debo", "sáquenme de aquí", "1". Si nadie procesa esas respuestas, o se
pierden (y el usuario siente que le habla a una pared) o alguien tiene que leer
chats a mano.

`intencionDetectada` clasifica en cinco intenciones accionables:
- `YA_PAGUE` → dispara el flujo de comprobante manual del módulo 3.
- `CONSULTAR_SALDO` → responde con el estado de cuenta.
- `AYUDA` → abre ticket de soporte (M9).
- `BAJA` → registra opt-out en el canal. **Esto no es opcional: es requisito
  regulatorio y de los proveedores** que responder "BAJA" funcione.
- `DESCONOCIDA` → escala a un humano.

**Por qué debe existir.** Sin respuestas entrantes, el canal es unidireccional y
el usuario que quiere cooperar (avisar que ya pagó) no tiene cómo hacerlo por el
canal que sí usa.

---

### `ProgramacionRecordatorio` / `programacion_recordatorio` — Política

**Qué es.** La regla de cuándo mandar cada recordatorio, relativa a una fecha de
referencia.

**Para qué sirve (negocio).** `desfaseDias` es elegante en su simplicidad: `-3`
es tres días antes del vencimiento, `+1` es un día después. Con eso se arma toda
la secuencia de cobranza preventiva sin escribir código:

- `-3`: "tu aporte vence el viernes"
- `-1`: "tu aporte vence mañana"
- `0`: "hoy vence tu aporte"
- `+1`: "tu aporte venció ayer"
- `+3`, repetido: escalamiento

`horaEnvio` importa más de lo que parece: mandar el recordatorio de cobro a las
9 de la mañana de un lunes tiene tasa de conversión distinta que a las 8 de la
noche de un viernes. Poder ajustarlo por configuración permite optimizar sin
desplegar.

`grupoId` nullable permite que un grupo tenga su propia cadencia (los grupos
familiares toleran menos recordatorios que los formales).

**Por qué debe existir.** Sin programación configurable, la secuencia de
recordatorios está hardcodeada y no se puede experimentar con lo que realmente
mejora la puntualidad — que es la métrica que define el éxito del producto.

---

### `BandejaEntrada` / `bandeja_entrada`

**Qué es.** El espejo de las notificaciones dentro de la app.

**Para qué sirve (negocio).** WhatsApp y SMS se pierden en la marea de mensajes.
La bandeja en la app es el registro persistente: el usuario entra y ve todo lo que
le comunicaron, con su acción pendiente. Es también la red de seguridad cuando el
usuario hizo opt-out de todos los canales externos — **algo tiene que poder
avisarle que su turno está listo para cobrar**.

`urlAccion` lleva directo a lo que hay que hacer.

**Por qué debe existir.** Sin bandeja, un usuario que borró el WhatsApp no tiene
cómo enterarse de nada, y la única constancia de lo comunicado está fuera de la
plataforma.

---

### `AvisoInterno` / `aviso_interno`

**Qué es.** El registro persistente de avisos dirigidos a operadores internos o a roles funcionales en el backoffice.

**Para qué sirve (negocio).** Alerta a operadores sobre hechos operativos críticos: una nueva alerta de monitoreo, un caso asignado, una ficha técnica pendiente de revisión o un fallo de despliegue. Puede dirigirse a un operador específico o a todo un rol (ej. `ADMIN_PLATAFORMA`, `OFICIAL_CUMPLIMIENTO`).

**Por qué debe existir.** Sin este registro, los operadores no tendrían una campana de notificaciones real que les informe de eventos urgentes con enlace directo a la pantalla de resolución.

---

### `ServicioNotificaciones` — Servicio de dominio

**Qué es.** El orquestador: emite, emite en lote, procesa webhooks de entrega,
procesa mensajes entrantes y reintenta fallidos.

**Para qué sirve (negocio).** `emitirLote()` es el que atiende el caso masivo (los
recordatorios de todos los grupos que vencen mañana) aplicando deduplicación,
supresión y preferencias en una sola pasada.

---

## Resumen: qué se cae si se quita cada bloque

| Bloque | Si no existe… |
| --- | --- |
| `EventoNotificable` | Cada módulo notifica a su manera; ajustar la política de comunicación toca ocho módulos. |
| `Plantilla` + `Version` | Se intentan enviar mensajes que WhatsApp rechaza; no se puede probar qué se comunicó. |
| `CanalVinculado` | Se le escribe a quien pidió que no le escriban; se pagan envíos fuera de la ventana de 24 h. |
| `ListaSupresion` | Las quejas por spam degradan el canal **para todos los usuarios**. |
| `Notificacion` (con deduplicación) | El job reejecutado bombardea con cinco mensajes idénticos y genera opt-outs. |
| `EnvioNotificacion` | No hay cadena de respaldo ni forma de saber qué canal hace pagar a la gente. |
| `EventoEntregaMensaje` | No se sabe si el mensaje llegó; el escalamiento a respaldo es a ciegas. |
| `ColaEnvio` | Un reinicio en pleno pico de envío deja a todo un día sin recordatorios. |
| `EnlacePagoNotificado` | No se puede atribuir un pago al recordatorio que lo causó. |
| `RespuestaEntrante` | El usuario que quiere avisar que ya pagó le habla a una pared. |
| `ProgramacionRecordatorio` | La secuencia de cobranza está hardcodeada y no se puede optimizar. |
