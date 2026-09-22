# 98 — Microservicios y límites de servicio

Pasanaku **no es un monolito**. Eso no es un detalle de despliegue: cambia qué significa "está
hecho". En un monolito, una transacción de base te salva de la mitad de los errores de diseño.
Acá no hay esa red: cada llamada entre servicios puede fallar, llegar tarde o llegar dos veces, y
el estado puede quedar partido entre dos bases que nadie puede juntar con un `JOIN`.

Esta regla fija lo que **no se negocia** cuando el cambio cruza un límite de servicio.

## 98.1 El límite es el contrato, y el contrato es sagrado

1. **Un servicio es dueño de sus datos.** Prohibido que un servicio lea o escriba la base de
   otro: ni una vista, ni un `JOIN` entre esquemas, ni "solo para un reporte", ni un usuario de
   solo lectura. Si necesitás datos ajenos, los pedís por su API o los recibís por evento.
2. **Prohibido compartir tablas entre servicios.** Una base compartida es un monolito con más
   latencia y sin transacciones: lo peor de los dos mundos.
3. **El contrato se versiona y no se rompe.** Quitar un campo, renombrarlo, cambiar su tipo o su
   semántica es un **cambio incompatible**: versión nueva, convivencia de ambas y fecha de retiro
   declarada. Ver `service-contracts-versioning`.
4. **El consumidor no se entera por el deploy.** Todo cambio de contrato tiene su prueba de
   contrato (consumer-driven) corriendo en CI, del lado del productor. Si no hay test de
   contrato, no hay contrato: hay una costumbre.
5. **Prohibido publicar la entidad del ORM como evento o como respuesta.** Se mapea a un payload
   propio, mínimo y estable. La entidad cambia con tu modelo; el contrato, no.

## 98.2 Toda llamada remota falla — diseñá para eso

1. **Prohibida una llamada saliente sin timeout explícito.** El timeout por defecto de la
   librería no es una decisión: es una omisión que se paga con hilos colgados en cascada.
2. **Reintentos solo sobre operaciones idempotentes**, con backoff exponencial, *jitter* y tope.
   Reintentar un `POST /cobros` sin clave de idempotencia es cobrar dos veces.
3. **Circuit breaker** hacia toda dependencia externa e inter-servicio, con su fallback
   declarado: qué devuelve la API cuando el dependido está caído, y si eso es aceptable.
4. **Degradación declarada, no accidental.** Por cada dependencia, el diseño dice qué pasa si no
   responde: falla el pedido, responde parcial, o encola. "No lo pensamos" no es una opción.
5. **Prohibida la cadena sincrónica de más de dos saltos** en un camino de usuario. Tres
   servicios en fila multiplican latencia y probabilidad de fallo; a partir de ahí se rediseña
   con evento o con datos replicados.
6. Ver `resilience-patterns` y `service-communication-patterns`.

## 98.3 No hay transacciones distribuidas: hay sagas y outbox

1. **Prohibido el commit de dos fases (2PC)** y prohibido cualquier diseño que asuma que dos
   servicios cambian de estado a la vez.
2. Un flujo que cruza servicios y mueve dinero (aporte → acreditación → contabilidad → entrega)
   es una **saga con compensaciones**: estado persistido, timeout por paso y compensación
   idempotente. Ver `saga-distributed-transactions`.
3. **Guardar en la base y publicar el evento es una sola unidad atómica: outbox.** Prohibido
   `await broker.publish()` dentro de la transacción, o después sin outbox, en cualquier flujo
   que no tolere perder el evento. Ver `async-messaging-events`.
4. **Todo consumidor es idempotente**, con deduplicación transaccional por `message_id`. La
   entrega es at-least-once; exactly-once no existe.
5. **Ningún paso irreversible va primero.** Primero lo compensable; al final lo que no se puede
   deshacer (mandar la plata, mandar el WhatsApp).

## 98.4 La consistencia eventual se muestra, no se esconde

1. Si el dato que el usuario acaba de cambiar todavía no se ve en otra pantalla, la UI lo dice
   ("procesando"), no miente con un estado viejo. Ver `frontend-ux-states`.
2. **Prohibido presentar el saldo de una réplica como definitivo.** El número con el que se toma
   una decisión de dinero se lee de su dueño, o del mayor (regla 91.1.5).
3. Un modelo de lectura replicado declara su **desfase máximo tolerable** y alerta cuando lo
   supera. Una réplica atrasada sin alerta es un reporte que miente en silencio.
4. Ver `eventual-consistency-read-models`.

## 98.5 Trazabilidad de punta a punta, o no hay diagnóstico

1. **Todo request y todo mensaje lleva `correlationId`**, propagado por cabecera y por el sobre
   del evento, desde el gateway hasta el último consumidor. Un servicio que no lo propaga rompe
   la traza de todos los que vienen detrás.
2. **Todo log estructurado incluye** `correlationId`, `service`, `version`, `actorId` (si hay) y
   el identificador del agregado. Sin contenido de datos personales ni financieros (regla 90.2).
3. **Traza distribuida activa** en los caminos de dinero. Reconstruir un pago a mano cruzando los
   logs de cuatro servicios por timestamp no es observabilidad: es arqueología.
4. Ver `distributed-tracing-correlation`.

## 98.6 Autorización en cada servicio, confianza cero

1. **Prohibido que un servicio confíe en que "el gateway ya validó".** Cada servicio valida el
   token y resuelve la autorización sobre el recurso que toca.
2. **Prohibida una cabecera de identidad inyectada y creída sin firmar** (`X-User-Id` a secas).
   La identidad viaja en un token verificable, o se resuelve de nuevo.
3. Comunicación entre servicios **autenticada** (mTLS o token de servicio con alcance acotado),
   nunca "confiable porque está en la red interna".
4. Cada servicio expone hacia adentro **solo lo que otro servicio necesita**, no su API completa.
5. Ver `service-to-service-security`.

## 98.7 Antes de crear un servicio nuevo

Un servicio nuevo cuesta: repositorio, pipeline, despliegue, base, monitoreo, alertas, guardia y
un contrato que mantener para siempre. **No se crea porque "queda más ordenado".**

Se justifica, por escrito, con al menos una de estas:

- Tiene un **ciclo de vida de datos propio** y un dueño claro (bounded context real).
- Necesita **escalar o desplegarse** con una cadencia distinta del resto.
- Tiene un **perfil de riesgo distinto** que conviene aislar (todo lo que toca dinero o KYC).
- Lo exige una frontera de **equipo** que ya existe.

Si la razón es "para que no crezca el otro", es un módulo, no un servicio. Si dos servicios
siempre se despliegan juntos y siempre cambian juntos, **son uno**: el límite está mal trazado y
lo único que ganaste es latencia. Ver `microservices-architecture`.

## 98.8 Evidencia obligatoria en un cambio que cruza servicios

No se cierra sin esto en el `REPORTE.md`:

1. **Lista de los saltos** que hace la operación, con qué servicio hace qué.
2. **Prueba de contrato** del lado productor y del lado consumidor, con salida pegada.
3. **Prueba de duplicado**: el mismo mensaje procesado dos veces, con la salida que demuestra que
   el efecto ocurrió una sola vez.
4. **Prueba de caída del dependido**: qué hace el sistema con el servicio B apagado, ejercitado
   de verdad (apagar el contenedor cuenta; mockearlo no).
5. **Traza** con el `correlationId` cruzando los servicios involucrados.
6. **Qué queda inconsistente** y por cuánto tiempo, si la operación falla en el medio.

Un E2E verde con todos los servicios sanos demuestra el camino feliz, nada más. El peldaño
`VERIFIED` (regla 30) en un sistema distribuido exige haber roto algo a propósito.

## 98.9 Skills relacionadas

`microservices-architecture` · `service-communication-patterns` ·
`saga-distributed-transactions` · `service-contracts-versioning` · `api-gateway-bff` ·
`resilience-patterns` · `distributed-tracing-correlation` · `eventual-consistency-read-models` ·
`microservices-testing` · `microservices-deployment` · `service-to-service-security` ·
`distributed-data-integrity` · `async-messaging-events` · `github-multirepo-coordination` ·
`git-workflow-multirepo`
