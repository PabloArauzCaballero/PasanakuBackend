---
name: automatizacion-tareas
description: "Ejecutar exactamente una vez lo que una regla decidió en AportaYa: clave de idempotencia derivada del hecho disparador, toma con SKIP LOCKED entre réplicas, un registro por intento, reevaluación de la condición al ejecutar, confirmación humana que caduca y fallo que avisa a una persona. Úsala al escribir cualquier trabajo programado, motor de automatización o consumidor que produzca efectos."
---

# Ejecutar tareas automáticas

`trabajos-outbox` cubre el efecto externo que sale de una transacción. Esta skill
cubre el otro lado: la tarea que **una regla decidió** y que hay que ejecutar una
vez, a su hora, con el sistema cayéndose y tres réplicas corriendo.

```
regla dispara → tarea_automatizada (clave única por hecho) → [confirmación humana]
      → trabajador toma con SKIP LOCKED → ejecucion_tarea (una fila por intento)
      → acción en su propia transacción, con la misma clave
```

## La clave es del hecho, no del intento

```ts
claveIdempotencia = uuidv5(`${reglaId}:${ambitoId}:${hechoDisparadorId}`, NS_APORTAYA)
```

`uq_tarea_automatizada_clave` (`R-ORG-07`) hace que el mismo hecho procesado dos
veces produzca **una** tarea. Si la regla se evalúa dos veces por un reintento del
outbox, no pasa nada.

## Toma concurrente

```sql
SELECT * FROM tarea_automatizada
 WHERE estado = 'PROGRAMADA' AND programada_para <= now()
 ORDER BY programada_para
 FOR UPDATE SKIP LOCKED
 LIMIT 20;
```

`SKIP LOCKED` es lo que permite escalar a N réplicas sin coordinación externa. Sin
él, o se serializa todo o se ejecuta dos veces.

## Un registro por intento

[[ejecucion_tarea]] guarda `iniciada_en`, `finalizada_en`, `resultado`,
`registros_afectados`, `detalle` y `mensaje_error`. **Cada corrida deja fila.** Sin
eso no se puede explicar por qué algo pasó tres veces o ninguna, y esa pregunta
siempre llega.

## Reevaluar la condición al ejecutar

Entre programar y ejecutar pasa tiempo, y el mundo cambia: el período cerró, el
participante se fue, la obligación ya se pagó. **La condición se vuelve a evaluar
antes de actuar**, y si ya no se cumple la tarea se cancela con motivo.

Forzar la acción porque "así estaba programado" es la causa más común de que un
sistema le cobre a alguien que ya pagó.

## Confirmación humana

Las acciones sensibles (`R-ORG-06`) quedan en `ESPERANDO_CONFIRMACION` con una
**vista previa en lenguaje llano** de lo que va a hacer. Y la regla que importa:

> **El silencio no es consentimiento.** Si nadie confirma en el plazo, la tarea
> **caduca**. No se ejecuta por vencimiento.

## Fallar bien

| Clase | Qué se hace |
| --- | --- |
| Transitorio | reprograma con retroceso exponencial **con jitter**, hasta el tope |
| Permanente | `FALLIDA`, y si se repite se desactiva la regla y se avisa a quien la definió |
| Proceso muerto a mitad | la tarea queda tomada hasta que vence el bloqueo, y se reintenta con la misma clave |

**Una automatización que falla en silencio es peor que no tenerla.** Toda tarea que
llega a `FALLIDA` avisa a una persona.

## Ventanas y prioridad

- Fuera de la ventana operativa, lo no obligatorio se reprograma; lo obligatorio
  corre igual.
- Las atrasadas se procesan por prioridad y antigüedad, y **el atraso acumulado es
  un indicador operativo**, no un detalle: si crece, algo está roto.
- Al desactivar una regla, sus tareas programadas **se cancelan**; las que ya están
  ejecutando terminan.

## Atribución

Toda ejecución escribe en [[bitacora_evento]] con `es_automatico = true` y la regla
que la originó. En cualquier auditoría hay que poder separar lo que hizo una persona
de lo que hizo el sistema.

## Qué no hacer

- No usar `SELECT ... FOR UPDATE` sin `SKIP LOCKED` en la cola: serializa las réplicas.
- No ejecutar la acción en la misma transacción que marca la tarea como completada
  sin compartir la clave de idempotencia.
- No reintentar sin jitter: N réplicas reintentando al mismo milisegundo son una
  tormenta, no un reintento.
- No ejecutar por vencimiento de confirmación.
- No dejar tareas fallidas sin destinatario humano.
- No confiar en `Date.now()` para el orden: la fuente de tiempo es la base.

## Ver también

- [[CU-96 Programar y ejecutar una tarea automatizada]] ·
  [[CU-95 Definir una regla de automatización]] ·
  [[CU-83 Enrutar el envío por proveedor de mensajería]]
- `R-ORG-06` · `R-ORG-07` · `R-BIL-06` en [[Restricciones]]
- Skills: `trabajos-outbox`, `idempotencia-reintentos`, `motor-de-reglas`,
  `resiliencia-rendimiento`, `observabilidad`
