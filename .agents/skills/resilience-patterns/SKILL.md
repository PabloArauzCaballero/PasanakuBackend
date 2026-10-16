---
name: resilience-patterns
description: Patrones de resiliencia para dependencias que fallan — timeouts en cascada bien calculados, reintentos con backoff y jitter solo sobre operaciones idempotentes, circuit breaker con sus tres estados, bulkhead para que una dependencia lenta no consuma todo el pool, fallback y degradación declarada, load shedding, y el peligro particular de reintentar una operación de dinero. Usar al integrar cualquier dependencia externa o inter-servicio, al diagnosticar una caída en cascada o un pool agotado, al elegir valores de timeout y reintento, y antes de declarar que un flujo aguanta la caída de un dependido.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Patrones de resiliencia

Un sistema distribuido está **siempre parcialmente caído**. La pregunta no es si el dependido
falla, sino qué hace tu servicio cuando pasa. Si la respuesta no está escrita, la respuesta es
"cae también".

## 1. Timeout: el patrón más importante y el más olvidado

Sin timeout, un dependido lento no te devuelve un error: te **consume los hilos y el pool de
conexiones** hasta que caés vos, y con vos los que te llaman. Así se propaga una caída en
cascada.

```ts
// ❌ sin timeout: el default de la librería puede ser infinito
const r = await http.get(`${PAGOS}/ordenes/${id}`);

// ✅ timeout explícito, derivado del presupuesto del endpoint
const r = await http.get(`${PAGOS}/ordenes/${id}`, { timeout: 400 });
```

Reglas de cálculo:

1. El endpoint tiene un **presupuesto p95** (`service-communication-patterns` §3). Los timeouts
   de los saltos suman menos que ese presupuesto.
2. **Timeout del cliente < timeout del servidor del dependido.** Si no, el dependido sigue
   trabajando un pedido que ya nadie espera.
3. Timeout **también** en: conexión, lectura, consulta a base, adquisición de conexión del pool y
   job de cola. El que falta es el que te va a doler.
4. Un timeout de 30 s en una llamada interna no es tolerancia: es una fuga.

## 2. Reintentos: solo si es idempotente

```ts
// Backoff exponencial con jitter: sin jitter, todos los clientes reintentan a la vez
// y la avalancha vuelve a tumbar al que se estaba recuperando.
const espera = Math.min(base * 2 ** intento, techo) * (0.5 + Math.random() / 2);
```

| Reintentar | No reintentar |
|---|---|
| Timeout, 503, 429 (respetando `Retry-After`), error de conexión, deadlock | 400, 401, 403, 404, 409, 422 |
| Lock no adquirido | Cualquier error de validación o de regla de negocio |
| Consulta de solo lectura | **Cualquier mutación de dinero sin clave de idempotencia** |

> [!important] Reintentar un cobro o un desembolso sin `Idempotency-Key` es cobrar o pagar dos
> veces. Si el proveedor no soporta clave de idempotencia, **no se reintenta automáticamente**:
> se registra "intentado", se consulta el estado y se decide (regla 91.1.4).

- Tope de intentos siempre; agotados, el error sube o va a DLQ (`async-messaging-events`).
- **Reintento solo en una capa.** Cliente + gateway + cola reintentando cada uno 3 veces son 27
  pedidos. Elegí dónde vive el reintento y sacalo de las otras capas.
- El reintento cuenta contra el presupuesto de latencia: 3 intentos de 400 ms son 1,2 s.

## 3. Circuit breaker

Cuando el dependido está caído, seguir llamándolo empeora todo: le agrega carga y a vos te agrega
latencia. El breaker **falla rápido** mientras dura la caída.

| Estado | Comportamiento | Transición |
|---|---|---|
| **Cerrado** | Pasan todas las llamadas; se cuentan fallos | Supera el umbral (p. ej. 50 % de 20 llamadas) ⇒ abierto |
| **Abierto** | Falla inmediato sin llamar; se aplica el fallback | Pasado el tiempo de espera ⇒ semiabierto |
| **Semiabierto** | Deja pasar unas pocas de prueba | Éxito ⇒ cerrado · fallo ⇒ abierto de nuevo |

- Un breaker **por dependencia**, no uno global.
- El estado del breaker es una **métrica y una alerta**: si se abre, alguien tiene que enterarse.
- Contá como fallo el timeout y el 5xx; **no** el 4xx, que es culpa del pedido, no del dependido.
- El breaker no reemplaza al fallback: define *cuándo* aplicarlo.

## 4. Bulkhead: aislar para no hundirte entero

Nombre de los mamparos de un barco: una bodega inundada no hunde el resto.

- **Pool de conexiones separado por dependencia.** Si `notificaciones` se pone lenta y comparte
  pool con `contabilidad`, tu servicio deja de poder asentar por culpa de un WhatsApp.
- **Límite de concurrencia por dependencia**: máximo N llamadas en vuelo; lo que exceda falla
  rápido en vez de encolarse para siempre.
- Colas separadas por tipo de trabajo: un pico de notificaciones no puede frenar la acreditación
  de aportes.
- En la base: no dejar que un reporte pesado consuma todas las conexiones del pool transaccional.

## 5. Fallback y degradación declarada

Por cada dependencia, el diseño responde: **¿qué pasa si no responde?** Las respuestas válidas:

| Estrategia | Cuándo | Ejemplo Pasanaku |
|---|---|---|
| **Fallar** | El dato es esencial o es dinero | No hay saldo del mayor ⇒ el panel no miente, muestra error |
| **Omitir** | Dato accesorio | Sin score de confianza ⇒ panel sin score, marcado como no disponible |
| **Valor en caché viejo** | Tolerable y marcado como viejo | Catálogo de bancos de hace una hora |
| **Encolar** | El efecto puede ocurrir después | Notificación que se manda cuando el canal vuelva |
| **Rechazar y pedir reintento** | Operación de usuario reintentable | `503` con `Retry-After` |

**Prohibido el fallback silencioso** que devuelve un valor plausible inventado. Si la UI no puede
distinguir "cero" de "no sé", el diseño está mal.

## 6. Load shedding y presión de vuelta

Cuando entra más de lo que podés procesar, aceptar todo garantiza caer con todo adentro.

- Límite de concurrencia en la entrada; lo que excede recibe `503` rápido y honesto.
- Prioridad por tipo: la acreditación de un aporte pasa antes que un reporte.
- Cola con tope y política de descarte explícita: una cola sin tope es memoria que se acaba.
- `429`/`503` con `Retry-After` es una respuesta correcta y sana, no un fracaso.

## 7. Verificar que funciona: romper a propósito

La regla 98.8.4 lo exige: **no se cierra un cambio que cruza servicios sin apagar el dependido**.

```bash
docker compose stop confianza
curl -s -o /dev/null -w '%{http_code} %{time_total}\n' localhost:8080/v1/grupos/$ID/panel
# esperado: 200 con "parcial":["score"] en menos del presupuesto, NO un cuelgue de 30 s
docker compose start confianza
```

- Probá también el **dependido lento**, que es peor que el caído: `tc`/toxiproxy o un stub que
  duerme. Un dependido que tarda 20 s revela los timeouts que faltan.
- Probá la **recuperación**: que el breaker cierre y el tráfico vuelva sin intervención.

## Anti-patrones

- Llamada sin timeout, o con el default de la librería.
- Reintentar un `POST` no idempotente que mueve dinero.
- Reintentos en tres capas a la vez.
- Backoff sin jitter: avalancha sincronizada sobre el que se recupera.
- Breaker global para todas las dependencias.
- Fallback que inventa un importe, un saldo o una cuota.
- `catch {}` que traga el error y sigue: eso no es resiliencia, es ceguera.
- Cola sin tope ni DLQ.
- Health check que depende de todos los dependidos: uno caído te marca caído.
- Declarar "es resiliente" sin haber apagado nada.

## Checklist

- [ ] Toda llamada saliente tiene timeout explícito, derivado del presupuesto.
- [ ] Timeout del cliente menor que el del servidor del dependido.
- [ ] Reintentos solo sobre operaciones idempotentes, con backoff, jitter y tope.
- [ ] Reintento en una sola capa, declarada.
- [ ] Ninguna mutación de dinero se reintenta sin clave de idempotencia.
- [ ] Circuit breaker por dependencia, con métrica y alerta.
- [ ] Pool y concurrencia aislados por dependencia (bulkhead).
- [ ] Estrategia de degradación declarada por dependencia, sin valores inventados.
- [ ] Probado con el dependido apagado **y** con el dependido lento.
- [ ] Probada la recuperación automática.

## Evidencia / DoD

1. Tabla de dependencias con timeout, reintento, breaker y estrategia de degradación.
2. Salida del flujo con el dependido apagado, con código y tiempo de respuesta.
3. Salida del flujo con el dependido lento, demostrando que corta en el timeout.
4. Métrica o log que muestre el breaker abriéndose y volviendo a cerrar.
5. Para operaciones de dinero: la prueba de que un reintento no duplicó el efecto.
