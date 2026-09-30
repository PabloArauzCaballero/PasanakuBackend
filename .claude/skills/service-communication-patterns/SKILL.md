---
name: service-communication-patterns
description: Cómo se hablan los servicios de Pasanaku — decidir entre sincrónico y asíncrono, REST interno vs gRPC vs mensajería, request/response vs evento vs comando, timeouts y presupuesto de latencia por salto, propagación de contexto (correlación, actor, idempotencia), llamadas chatty y el patrón de datos replicados, y qué hacer cuando necesitás el dato de otro servicio ya mismo. Usar antes de agregar una llamada entre servicios, al diseñar un flujo que cruza dos o más, cuando un endpoint lento resulta ser tres llamadas encadenadas, o al decidir si publicar un evento o llamar una API.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Comunicación entre servicios

La pregunta no es "¿REST o mensajería?". Es **"¿el que pide necesita la respuesta para seguir?"**.
Todo lo demás sale de ahí.

## 1. El árbol de decisión

```
¿El usuario espera el resultado en pantalla para continuar?
├── SÍ  → ¿el dato es del otro servicio y no lo puedo tener replicado?
│         ├── SÍ  → request/response sincrónico, con timeout y fallback declarados
│         └── NO  → leelo de tu propio modelo de lectura (replicado por evento)
└── NO  → ¿es un pedido a un dueño concreto o un hecho que ya pasó?
          ├── pedido  → COMANDO async (un solo consumidor, puede rechazarse)
          └── hecho   → EVENTO (cero a N consumidores, no se rechaza)
```

**Regla de oro de Pasanaku:** si el flujo mueve dinero, empezá por asumir asíncrono con outbox.
El sincrónico se justifica, no al revés.

## 2. Sincrónico: cuándo sí

Casos legítimos en Pasanaku:

| Caso | Por qué sincrónico |
|---|---|
| `pagos` pregunta a `identidad` si el usuario está verificado antes de emitir un QR | La respuesta cambia lo que se devuelve ahora |
| El gateway resuelve el token contra `identidad` | Sin identidad no hay request |
| `entregas` consulta a `garantia` la deuda exigible al liquidar | Es un invariante del cálculo de este instante |
| Una pantalla compone datos de dos servicios en el BFF | El usuario está esperando |

Y aun así, cada una lleva: **timeout explícito, reintento solo si es idempotente, circuit breaker
y comportamiento declarado ante caída** (`resilience-patterns`).

## 3. Presupuesto de latencia

Un endpoint tiene un presupuesto, y los saltos lo consumen. Escribilo antes de codificar:

```
GET /grupos/:id/panel   presupuesto p95: 800 ms
  ├── BFF                              30 ms
  ├── grupos.obtenerPanel             150 ms  (su propia base)
  ├── confianza.scoreDeGrupo          120 ms  (timeout 300 ms, fallback: sin score)
  └── contabilidad.saldoDelMayor      200 ms  (timeout 400 ms, SIN fallback: es dinero)
                                   ≈ 500 ms + red
```

- **El timeout de cada llamada es menor que el presupuesto restante.** Un timeout de 30 s en una
  llamada interna no protege nada: tumba el pool de conexiones del que llama.
- Regla práctica: timeout del cliente < timeout del servidor del dependido, para que el pedido no
  siga corriendo del otro lado después de que te rendiste.
- Todo salto sin fallback declarado convierte la caída del dependido en tu caída. A veces está
  bien (dinero), pero tiene que ser **una decisión escrita**.

## 4. REST interno, gRPC o mensajería

| Transporte | Usalo para | Cuidado |
|---|---|---|
| **REST/JSON interno** | Lo normal entre servicios de Pasanaku; legible, depurable, con OpenAPI (`api-openapi-docs`) | Verboso; fácil caer en CRUD ajeno |
| **gRPC** | Llamadas de altísima frecuencia y baja latencia entre dos servicios propios | Contrato binario: hace falta disciplina de `.proto` y herramientas para depurar |
| **Mensajería (cola/tópico)** | Todo lo que no bloquea al usuario; el default para dinero | Consistencia eventual visible; consumidores idempotentes obligatorios |

No mezcles estilos por gusto: **un servicio expone una sola forma de API sincrónica**. Dos estilos
es el doble de contratos, de tests y de errores.

## 5. Contexto que viaja en toda llamada

Sincrónica o asíncrona, siempre:

```http
X-Correlation-Id: 0b0f…        # obligatorio, se genera en el borde si no vino
X-Causation-Id: 4c21…          # qué mensaje o request causó este
Authorization: Bearer …        # identidad verificable, nunca X-User-Id a secas (regla 98.6)
Idempotency-Key: 7f9a…         # obligatorio en toda operación que muta dinero
Traceparent: 00-…              # traza distribuida (distributed-tracing-correlation)
```

- El servicio que no propaga `X-Correlation-Id` **rompe la traza de todos los que siguen**. Es un
  defecto bloqueante en revisión, no un detalle.
- El `Idempotency-Key` lo genera **quien origina la intención**, no cada salto. Un reintento reusa
  el mismo valor; una intención nueva genera uno nuevo.

## 6. Llamadas chatty y el patrón de datos replicados

Síntoma: un listado de 50 cupos que hace 50 llamadas a `identidad` para mostrar el nombre.

| Solución | Cuándo |
|---|---|
| **Endpoint batch** (`POST /usuarios/lookup` con hasta N ids) | Arreglo inmediato, sigue siendo un salto |
| **Replicar el campo** vía evento (`usuario.perfil_actualizado` → copia local de nombre visible) | Listados frecuentes; el default para nombres, alias y avatar |
| **Incluirlo en el evento original** | Cuando el dato es parte del hecho ("quién pagó") |

Reglas de la copia replicada:

1. Se declara como copia en el nombre (`nombre_visible_cache`) o en la documentación de la tabla.
2. **Nunca se usa para decidir dinero ni permisos.** Para eso se pregunta al dueño.
3. Tiene desfase máximo tolerado y alerta si se supera (`eventual-consistency-read-models`).
4. Se puede reconstruir desde cero reproduciendo los eventos. Si no se puede, no es una copia: es
   una segunda fuente de verdad y ya estás en problemas.

## 7. Comando vs evento — nombrar bien evita rediseños

- **Comando**: imperativo, dirigido a un dueño: `emitir-orden-de-cobro`, `ejecutar-aval`. Un
  consumidor. Puede fallar y devolver rechazo.
- **Evento**: pasado, sin destinatario: `aporte.acreditado`, `turno.sorteado`,
  `incumplimiento.declarado-firme`. Cero a N consumidores. **No se rechaza**: ya pasó.

Un evento nombrado en imperativo (`crear-asiento`) es un comando disfrazado: acopla al productor
con el consumidor y te va a obligar a esperar respuesta. Corregí el nombre y el diseño se acomoda.

## 8. Anti-patrón: RPC disfrazado de evento

```ts
// ❌ evento que espera respuesta: es una llamada sincrónica con pasos extra y sin timeout
await bus.publish('calcular-deuda', { cupoId });
const deuda = await bus.waitFor('deuda-calculada', { cupoId, timeout: 5000 });

// ✅ si necesitás la respuesta ahora, pedila y asumí el costo
const deuda = await garantiaClient.deudaExigible(cupoId, { timeout: 400 });

// ✅ si no la necesitás ahora, seguí con el evento y reaccioná cuando llegue
await outbox.add({ type: 'entrega.liquidacion-iniciada', payload: { entregaId } });
```

## Anti-patrones

- Llamada sin timeout, o con un timeout mayor que el presupuesto del endpoint.
- Reintento automático sobre una operación no idempotente que mueve dinero.
- Cadena sincrónica de tres o más servicios en un camino de usuario (regla 98.2.5).
- Servicio A que expone un CRUD de su tabla solo para que B lo use: base compartida con HTTP.
- `X-User-Id` inyectado y creído.
- Evento con el agregado entero serializado, "por si el consumidor lo necesita".
- Consultar al dueño en un loop en vez de un batch o una copia replicada.
- Fallback silencioso que devuelve `0` como saldo cuando el dependido no responde. Un cero
  inventado en una pantalla de dinero es peor que un error.

## Checklist

- [ ] Cada llamada nueva pasó por el árbol de decisión de §1 y la decisión está escrita.
- [ ] Presupuesto de latencia del endpoint declarado, con el timeout de cada salto adentro.
- [ ] Reintentos solo sobre operaciones idempotentes, con backoff y tope.
- [ ] Fallback declarado por dependencia, o "sin fallback" declarado como decisión.
- [ ] `correlationId`, identidad verificable e `Idempotency-Key` se propagan.
- [ ] Nada de N+1 entre servicios: batch o copia replicada.
- [ ] Toda copia replicada está nombrada como copia y no decide dinero ni permisos.
- [ ] Comandos en imperativo, eventos en pasado.

## Evidencia / DoD

1. Tabla de saltos del flujo con timeout, reintento y fallback de cada uno.
2. Salida de una corrida con el dependido apagado, mostrando el comportamiento declarado.
3. Traza con un mismo `correlationId` cruzando todos los servicios del flujo.
4. Medición de latencia p95 del endpoint contra su presupuesto.
