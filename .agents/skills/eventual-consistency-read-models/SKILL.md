---
name: eventual-consistency-read-models
description: Consistencia eventual y modelos de lectura en Pasanaku — qué puede ser eventual y qué nunca (dinero y permisos), proyecciones construidas desde eventos, desfase máximo tolerado con su alerta, reconstrucción desde cero, read-your-own-writes para que el usuario no vea su propio cambio desaparecer, CQRS aplicado con moderación, y cómo se muestra "procesando" sin mentir. Usar al replicar datos de otro servicio, al construir un panel o listado que cruza servicios, cuando un usuario reporta que hizo algo y no lo ve, o al decidir si un número puede leerse de una réplica.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Consistencia eventual y modelos de lectura

Partir el sistema en servicios significa aceptar que, por un rato, **dos lugares dicen cosas
distintas**. Eso es manejable y normal. Lo que no es manejable es que ese rato no esté acotado,
no se mida, o afecte a un número con el que alguien decide plata.

La regla dura está en **98.4**.

## 1. Qué puede ser eventual y qué no

| Puede ser eventual | Nunca es eventual |
|---|---|
| Nombre visible del participante en un listado | El saldo con el que se liquida una entrega |
| Score de confianza en el panel | La deuda exigible al momento de deducir |
| Conteo de grupos activos en un dashboard | Si el usuario tiene permiso sobre este cupo |
| Historial y reportes | Si el cupo ya pagó este periodo, al acreditar |
| Badge de "grupo verificado" | El resultado del sorteo una vez revelado |

**Criterio único:** si alguien —persona o proceso— **toma una decisión de dinero o de permiso**
con ese número, se lee del dueño o del mayor. Todo lo demás puede venir de una proyección.

Un panel de transparencia que muestra el total del grupo puede tener 5 s de desfase declarado.
El cálculo de la entrega, no: lee del mayor, siempre (regla 91.1.5).

## 2. La proyección: cómo se construye

```
evento (outbox → broker) ──▶ proyector ──▶ tabla de lectura del servicio consumidor
```

1. El proyector es un **consumidor idempotente** más: deduplica por `message_id`
   (`async-messaging-events` §4).
2. Escribe en **su propia tabla**, nunca en la del dueño (regla 98.1.1).
3. La tabla de lectura **se puede tirar y reconstruir**. Si no se puede, no es una proyección:
   es una segunda fuente de verdad, y algún día va a discrepar sin que puedas decidir cuál gana.
4. Guarda, además de los datos: `ultimo_evento_id`, `ultimo_evento_en` y `version_proyeccion`.
   Sin eso no podés medir el desfase ni saber si una reconstrucción quedó completa.
5. El nombre de la tabla o de los campos dice que es copia: `usuario_nombre_cache`,
   `proy_panel_grupo`.

## 3. Desfase: se declara, se mide y se alerta

Por cada proyección, escrito en su documentación y en el ADR:

| Campo | Ejemplo |
|---|---|
| Desfase típico esperado | < 1 s |
| **Desfase máximo tolerable** | 30 s |
| Qué pasa si se supera | Alerta a guardia; el panel muestra "datos con retraso" |
| Quién la consume | BFF web, panel de transparencia |
| Cómo se reconstruye | `npm run proyeccion:rebuild -- panel-grupo` |

- La métrica es **antigüedad del último evento aplicado**, no la profundidad de la cola: una cola
  vacía con el proyector caído también miente.
- Superado el umbral, la UI lo dice. **Prohibido mostrar un dato viejo como si fuera de ahora**
  cuando sabés que está atrasado.

## 4. Read-your-own-writes

El caso que más reportes de soporte genera: el usuario paga, vuelve al panel y **no ve su pago**.
Técnicamente correcto; para él, el sistema perdió su plata.

Opciones, de mejor a peor:

1. **Devolver el resultado en la respuesta de la escritura** y que la UI lo use, sin volver a
   consultar. Lo más barato y lo que resuelve el 80 % de los casos.
2. **Leer del dueño** (no de la proyección) en la pantalla inmediatamente posterior a la acción.
3. **Estado explícito "procesando"** con actualización cuando llega la confirmación
   (`frontend-ux-states`, `realtime-websockets`).
4. Sticky de sesión a la réplica actualizada. Frágil; último recurso.

**Prohibido**: que la UI reintente en un loop hasta que aparezca, sin decirle nada al usuario.

## 5. CQRS con moderación

Separar el modelo de escritura del de lectura está bien **cuando la lectura tiene una forma que
la escritura no puede dar barato**: el panel de transparencia, el reporte de cierre, el listado
de grupos con score.

No está bien como default. Dos modelos son dos verdades que sincronizar. Si el `SELECT` del
dueño alcanza, usá el `SELECT` del dueño.

Señales de que la proyección se justifica: la consulta cruza datos de tres servicios · se lee
mil veces más de lo que se escribe · el cálculo es caro y el resultado cambia poco.

## 6. Reconstrucción

Toda proyección tiene un procedimiento de reconstrucción **probado**, no teórico:

```bash
# probado en staging, con tiempo medido y anotado en el runbook
npm run proyeccion:rebuild -- panel-grupo --desde=2026-01-01
# verificación: comparar el total reconstruido contra el mayor (fuente de verdad)
```

- Requiere que los eventos estén **retenidos** el tiempo suficiente, o que exista un snapshot.
  Decidí la retención antes de necesitarla.
- La reconstrucción es idempotente y se puede correr con el servicio arriba (o se declara que
  necesita ventana).
- **Se verifica contra la fuente de verdad**, no contra la proyección anterior. Reconstruir desde
  los mismos datos malos da el mismo resultado malo.

## 7. Detectar la deriva antes que el usuario

Un chequeo periódico que compara la proyección contra el dueño y alerta si difieren:

- Conteos por grupo y por periodo.
- Sumas de control de los importes agregados, contra el mayor
  (`distributed-data-integrity` cubre la conciliación interna entre servicios).
- Antigüedad del último evento aplicado por proyección.

Una proyección que derivó y nadie notó es un reporte que miente con confianza.

## Anti-patrones

- Decidir un desembolso con un saldo leído de una proyección.
- Resolver un permiso con una copia replicada del rol.
- Proyección que no se puede reconstruir.
- Proyección sin métrica de desfase ni alerta.
- Mostrar datos atrasados sin indicarlo, sabiendo que están atrasados.
- Que el proyector escriba en la tabla del servicio dueño.
- CQRS en todo el sistema "porque es la arquitectura".
- Arreglar la deriva con un `UPDATE` manual sobre la proyección, sin arreglar el proyector.
- Retener eventos menos tiempo del que tarda una reconstrucción.

## Checklist

- [ ] Ningún número de dinero ni de permiso se lee de una proyección.
- [ ] La copia está nombrada como copia y documentada.
- [ ] Proyector idempotente, con deduplicación por `message_id`.
- [ ] Desfase típico y máximo declarados, con métrica y alerta.
- [ ] La UI comunica el retraso cuando lo hay, y el "procesando" cuando corresponde.
- [ ] Read-your-own-writes resuelto en el flujo posterior a una escritura.
- [ ] Procedimiento de reconstrucción probado y cronometrado, en el runbook.
- [ ] Retención de eventos suficiente para reconstruir.
- [ ] Chequeo periódico de deriva contra la fuente de verdad.

## Evidencia / DoD

1. Ficha de la proyección: fuente, eventos consumidos, desfase declarado, consumidores.
2. Salida de la reconstrucción completa, con tiempo y verificación contra la fuente de verdad.
3. Salida del chequeo de deriva, en cero.
4. Captura o salida que muestre el comportamiento de la UI con desfase superado.
5. Prueba de read-your-own-writes: la secuencia escribir → leer, con su salida.
