---
name: api-gateway-bff
description: El borde de Pasanaku — qué responsabilidades van en el gateway y cuáles nunca, BFF por cliente (web Angular, app Flutter, WhatsApp), composición de respuestas sin encadenar servicios, autenticación en el borde sin que los servicios confíen en ella, rate limiting, versionado y CORS, manejo del fallo parcial cuando un servicio compuesto no responde, y cómo evitar que el gateway se vuelva un monolito nuevo. Usar al exponer un endpoint hacia afuera, al armar la pantalla que necesita datos de varios servicios, al agregar autenticación o rate limiting en el borde, o cuando el gateway empieza a tener reglas de negocio.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Gateway y BFF

El borde es lo único que ven los clientes. Hace dos trabajos y **no debe hacer un tercero**:

1. **Gateway** — lo transversal: TLS, ruteo, autenticación, rate limiting, CORS, correlación,
   límites de tamaño, versionado de la API pública.
2. **BFF** — la forma de la respuesta para *un* cliente concreto: composición, recorte y
   adaptación a lo que esa pantalla necesita.

El tercer trabajo prohibido es **decidir reglas de negocio**. La regla de negocio vive en el
servicio dueño. Un gateway con reglas es un monolito nuevo, con la desventaja de que además es el
punto único de caída (`microservices-architecture`).

## 1. Qué va en el gateway

| Responsabilidad | Detalle |
|---|---|
| Terminación TLS y cabeceras de seguridad | HSTS, `X-Content-Type-Options`, CSP para lo que sirva HTML |
| Ruteo por prefijo | `/v1/grupos/*` → servicio `grupos` |
| **Autenticación** | Valida el token una vez y lo propaga; **no autoriza recursos** |
| Correlación | Genera `X-Correlation-Id` si no vino, y lo propaga siempre |
| Rate limiting y cuotas | Por IP, por usuario y por endpoint (`security-guardrails`) |
| Límite de tamaño de body y timeouts | Antes de que llegue a un servicio |
| Versionado público | `/v1`, deprecación anunciada (`service-contracts-versioning`) |
| CORS | Lista blanca de orígenes, nunca `*` con credenciales |
| Observabilidad de borde | Latencia y tasa de error por ruta (`backend-observability`) |

## 2. Qué NUNCA va en el gateway

- **Reglas de negocio.** "Si el grupo está cerrado, no dejar aportar" vive en `grupos`.
- **Autorización sobre el recurso.** El gateway no sabe si este usuario es dueño de este cupo.
  Eso lo resuelve el servicio dueño, siempre (regla 98.6.1).
- **Acceso directo a una base.** El gateway no tiene base de negocio.
- **Estado de sesión propio** que los servicios necesiten para funcionar.
- **Transformaciones de dinero.** Ni redondeos, ni conversiones, ni sumas. Nada que calcule un
  importe (regla 91).

> [!important] El gateway autentica; los servicios autorizan.
> Si algún servicio confía en que "el gateway ya validó", basta un pedido que entre por la red
> interna para saltearse todo. Cada servicio valida el token y resuelve permisos por su cuenta.

## 3. Un BFF por cliente, no uno para todos

Pasanaku tiene tres clientes con necesidades distintas:

| Cliente | Necesita |
|---|---|
| Web Angular | Respuestas ricas, pocos requests, datos para tablas y paneles |
| App Flutter | Payloads chicos, tolerancia a red mala, sincronización offline (`mobile-offline-sync`) |
| WhatsApp / enlaces de pago | Respuestas mínimas, sin sesión, token de un solo uso |

Un BFF único que sirve a los tres termina con `?include=` y `?fields=` por todos lados, y cada
cambio para uno rompe a los otros. **Un BFF por cliente, mantenido por quien mantiene ese
cliente.**

Lo transversal (auth, rate limit, correlación) no se duplica: vive en el gateway, debajo.

## 4. Composición y fallo parcial

```ts
// ✅ en paralelo, con timeout y política por dependencia declarada
const [grupo, score, saldo] = await Promise.allSettled([
  gruposClient.panel(id,        { timeout: 300 }),
  confianzaClient.score(id,     { timeout: 200 }),   // opcional: si falla, se omite
  contabilidadClient.saldo(id,  { timeout: 400 }),   // dinero: si falla, falla el endpoint
]);
```

Reglas:

1. **En paralelo**, no en cadena. Encadenar suma latencias y es la cadena que prohíbe la regla
   98.2.5.
2. Cada dependencia declara si es **esencial** u **opcional**.
3. **Opcional que falla** ⇒ la respuesta sale sin esa parte y **lo dice** (`score: null` +
   `parcial: ['score']`), para que la UI muestre "no disponible" en vez de inventar
   (`frontend-ux-states`).
4. **Esencial que falla** ⇒ error, con el código que corresponda. Nunca un default.
5. **Prohibido inventar un valor por defecto para un dato de dinero.** Un `saldo: 0` porque
   `contabilidad` no respondió es una mentira con consecuencias (regla 91.1.5).
6. El estado parcial se comunica en el cuerpo, no solo en el código HTTP.

## 5. Rate limiting en el borde

| Superficie | Límite orientativo | Por qué |
|---|---|---|
| Login y OTP | Estricto, por IP **y** por identificador | Fuerza bruta (`auth-session-pentest`) |
| Emisión de QR / enlaces de pago | Estricto por usuario | Costo por operación y abuso |
| Webhooks entrantes de la pasarela | Alto, pero con firma verificada obligatoria | No podés limitar al proveedor, pero sí validar |
| Consultas de panel | Normal | |
| Exportes y reportes | Muy estricto | Costo y fuga de datos (`data-privacy-financial`) |

El límite devuelve `429` con `Retry-After`. **Prohibido devolver 200 con la lista vacía** cuando
en realidad limitaste: el cliente no puede distinguirlo de "no hay datos".

## 6. El gateway no puede ser un punto ciego

- Cada request registra: ruta, método, estado, latencia, `correlationId`, servicio destino.
  **Sin datos personales ni financieros en la línea de log** (regla 90.2).
- Métrica por ruta y por servicio destino; alerta por tasa de 5xx del borde, que es la que ve el
  usuario.
- El gateway también se despliega y se revierte (`microservices-deployment`); un cambio de ruteo
  es un cambio de producción con el mismo ceremonial que cualquier otro.
- Health check propio que **no** dependa de que todos los servicios estén sanos: si no, un
  servicio secundario caído marca el borde entero como no disponible.

## Anti-patrones

- Gateway con lógica de negocio o con su propia base.
- Servicio que confía en la autenticación del gateway y no valida nada.
- BFF único con `?include=` para servir a tres clientes distintos.
- Composición en cadena en vez de en paralelo.
- Fallo parcial oculto: devolver `0`, `[]` o `null` sin decir que falló.
- `CORS: *` con credenciales.
- Timeout del gateway mayor que el de los servicios: el cliente espera de más y el servicio ya
  se rindió.
- Exponer hacia afuera, tal cual, la API interna de un servicio: acoplás el mundo a tu modelo.
- Health check del gateway que devuelve rojo porque `notificaciones` está caído.

## Checklist

- [ ] El endpoint nuevo no metió reglas de negocio en el borde.
- [ ] El servicio destino autoriza por su cuenta; no confía en el gateway.
- [ ] Composición en paralelo, con timeout por dependencia.
- [ ] Cada dependencia marcada esencial u opcional, y el fallo parcial se comunica.
- [ ] Ningún valor de dinero inventado ante fallo.
- [ ] Rate limiting definido para la superficie nueva.
- [ ] `correlationId` generado o propagado.
- [ ] Logs del borde sin datos personales ni financieros.
- [ ] Versionado y deprecación de la API pública contemplados.

## Evidencia / DoD

1. Salida del endpoint con todos los servicios sanos.
2. Salida del mismo endpoint con una dependencia **opcional** apagada, mostrando la respuesta
   parcial declarada.
3. Salida con una dependencia **esencial** apagada, mostrando el error correcto (y no un default).
4. Salida de la prueba de rate limiting: el `429` con `Retry-After`.
5. Línea de log del borde, pegada, demostrando que no contiene datos sensibles.
