---
name: distributed-tracing-correlation
description: Trazabilidad de punta a punta en Pasanaku — correlationId y causationId, propagación por HTTP y por el sobre de los eventos, W3C Trace Context y OpenTelemetry, spans que valen la pena y atributos que nunca se ponen (datos personales o importes), muestreo que no pierde los errores ni los caminos de dinero, logs estructurados correlacionados y cómo reconstruir un pago que cruzó cinco servicios. Usar al crear un servicio o un consumidor, al instrumentar un flujo de dinero, cuando un incidente obliga a cruzar logs por timestamp, o al revisar que un cambio no rompa la traza.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Traza distribuida y correlación

En un monolito, un stack trace te dice qué pasó. Acá el "stack" está repartido en cinco procesos
y tres colas. **Sin correlación, diagnosticar un pago perdido es cruzar logs por timestamp**: eso
no es observabilidad, es arqueología, y en un incidente de dinero se paga en horas.

La obligación está en la **regla 98.5**. Acá está cómo se implementa.

## 1. Los tres identificadores

| Id | Qué es | Vive |
|---|---|---|
| `correlationId` | **La operación de negocio completa**: un aporte desde que el usuario toca "pagar" hasta que se asienta, se notifica y se puntúa | De punta a punta, cruza servicios y colas |
| `causationId` | El id del mensaje o request **inmediatamente anterior** | Reconstruye el árbol de causas |
| `traceId` / `spanId` | Traza técnica del estándar W3C | Lo maneja OpenTelemetry |

`correlationId` es de negocio y sobrevive a colas, reintentos y días. `traceId` es técnico y
suele cortarse en los saltos asíncronos. **Necesitás los dos.**

Regla: se genera **una sola vez, en el borde** (gateway, `api-gateway-bff`) o en el job que
origina la operación. Si llegó uno, se propaga; no se reemplaza nunca.

## 2. Propagación

### HTTP

```http
X-Correlation-Id: 0b0f2c…      # el nuestro, de negocio
traceparent: 00-4bf92f…-00f0…-01   # W3C Trace Context, el estándar
```

### Eventos y colas

Va en el **sobre**, no en el payload (`async-messaging-events` §7):

```ts
{ id, type, version, occurredAt, correlationId, causationId, actorId, tenantId, payload }
```

- El consumidor **reabre el contexto** desde el sobre antes de procesar: correlación, actor,
  tenant y traza. Sin eso, todo lo que loguee el consumidor queda huérfano.
- Un servicio que recibe correlación y no la propaga **rompe la traza de todos los que siguen**.
  Es un hallazgo bloqueante en revisión, igual que un test borrado.
- En NestJS: middleware/interceptor que lee o genera, lo mete en un `AsyncLocalStorage`, y un
  logger que lo lee solo. Prohibido pasarlo a mano por parámetro en cada función: se olvida.

## 3. Logs estructurados correlacionados

```json
{"ts":"2026-09-20T14:02:11.318Z","level":"info","service":"pagos","version":"1.8.2",
 "correlationId":"0b0f2c…","causationId":"7d31…","actorId":"usr_91…",
 "evento":"aporte.acreditado","obligacionId":"obl_44…","duracionMs":132}
```

Obligatorio: `ts`, `level`, `service`, `version`, `correlationId`.
Muy recomendable: `actorId`, el id del agregado tocado, y la duración.

**Prohibido en el log** (regla 90.2): nombre, documento, cuenta bancaria, teléfono, payload del
QR, y **el importe atribuible a una persona**. Se loguea `obligacionId`, no "Bs 350 de Juan".

Un log sin `correlationId` en un sistema distribuido es un log que no sirve en el momento en que
lo necesitás.

## 4. Spans: qué instrumentar

No instrumentes todo: instrumentá lo que explica una demora o una falla.

| Vale un span | No vale |
|---|---|
| Llamada HTTP saliente a otro servicio | Cada función privada |
| Consulta a base que puede ser lenta | Un getter |
| Publicación y consumo de un mensaje | Mapeos de DTO |
| Llamada al proveedor de pagos | Validaciones triviales |
| Paso de una saga | |

Atributos útiles en el span: servicio destino, ruta, resultado, si hubo reintento, número de
intento, si el breaker estaba abierto. **Nunca** un importe, un documento ni un nombre: los
atributos de span se exportan a un backend de terceros y quedan indexados.

## 5. Muestreo sin perder lo que importa

Trazar el 100 % en producción es caro. Muestrear al azar el 1 % hace que justo el pago problemático
no esté.

Política de la casa:

1. **100 % de los errores** y de todo span que termine en fallo.
2. **100 % de los caminos de dinero**: acreditación, entrega, ejecución de aval, cierre.
3. **100 % si el request trae el flag de depuración** (con permiso, no público).
4. Muestreo bajo (1–5 %) para el resto del tráfico de lectura.
5. Muestreo **decidido en el borde y propagado** (`sampled` en `traceparent`): si cada servicio
   decide por su cuenta, las trazas quedan partidas por la mitad.

## 6. Reconstruir una operación

El objetivo concreto: dado un `correlationId`, contar la historia completa en un minuto.

```bash
# 1. Todo lo que pasó en esa operación, en todos los servicios, ordenado
grep -h '"correlationId":"0b0f2c' logs/*.jsonl | jq -s 'sort_by(.ts) | .[] | "\(.ts) \(.service) \(.evento)"'
# 2. La traza técnica con las latencias por salto
# 3. Los mensajes de la cola con ese correlationId, incluidos los que están en DLQ
```

Esto tiene que estar en el **runbook** del servicio (`backend-observability`): no se inventa
durante el incidente.

**Regla de soporte:** todo error que devuelve la API al usuario incluye un identificador que el
soporte pueda usar para encontrar la operación (`error-handling-contract`), sin exponer detalles
internos.

## 7. Verificación de que la traza no está rota

Es fácil romper la propagación sin que nada falle. Se verifica con un test, no con confianza:

- Test de integración que dispara el flujo y **afirma que el `correlationId` del último
  consumidor es igual al del request inicial**.
- Chequeo en CI: ningún cliente HTTP que no pase por el wrapper que propaga cabeceras
  (`static-analysis-linting` o un test de arquitectura).
- Alerta operativa: porcentaje de logs sin `correlationId` por servicio. Si sube, alguien rompió
  la cadena.

## Anti-patrones

- `console.log('llegó acá')` sin correlación ni estructura.
- Generar un `correlationId` nuevo en cada servicio: cinco trazas de un solo pago.
- Correlación solo en HTTP, perdida al pasar por la cola.
- Poner el importe, el nombre o el documento como atributo de span o campo de log.
- Muestrear al azar e incluir los caminos de dinero en el descarte.
- Instrumentar cada función y ahogar la traza en ruido.
- Log sin `service` ni `version`: no sabés qué versión produjo el error.
- Depender de los timestamps para ordenar eventos de máquinas distintas.

## Checklist

- [ ] `correlationId` se genera en el borde y se propaga por HTTP y por el sobre de eventos.
- [ ] El consumidor reabre contexto (correlación, actor, tenant) antes de procesar.
- [ ] Todos los logs son estructurados e incluyen `service`, `version` y `correlationId`.
- [ ] Ningún log ni atributo de span lleva datos personales, de cuenta o importes atribuibles.
- [ ] Spans en los saltos que importan; sin ruido.
- [ ] Muestreo: 100 % en errores y caminos de dinero, decidido en el borde.
- [ ] Runbook con el comando para reconstruir una operación por `correlationId`.
- [ ] Test que verifica que la correlación sobrevive al flujo completo.
- [ ] El error que ve el usuario trae un identificador rastreable.

## Evidencia / DoD

1. Traza completa de un flujo que cruza al menos tres servicios, con un solo `correlationId`.
2. Salida del test que afirma que la correlación llega al último consumidor.
3. Líneas de log de dos servicios distintos de la misma operación, pegadas, sin datos sensibles.
4. Comando del runbook ejecutado sobre un caso real, con su salida.
