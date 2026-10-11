# ALR-03 · Cola detenida (outbox sin publicar)

Regla: `despliegue/alertas/reglas.yml` (`ALR-03`, severidad ALTA). Responsable: rol `ADMIN_PLATAFORMA`.
Escala a: `RESPONSABLE_SEGURIDAD`. Evento de aviso: `OPERACION_COLA_DETENIDA`.

## Síntomas

- En cualquiera de los 15 esquemas hay `evento_dominio` en `PENDIENTE` hace más de `cola_minutos` minutos,
  `TOMADO` por un relevo que murió (`tomado_en` viejo), o `FALLIDO`.
- La evidencia lista los esquemas con más eventos detenidos y el más viejo.
- Efecto visible: la operación quedó registrada pero notificaciones, proyecciones o contabilidad no se enteran.

## Diagnóstico

Seguir [[outbox-backlog]] (es el mismo problema, esta alarma es lo que lo hace visible): Kafka sano,
publicador vivo, bloqueos en la tabla, o simple pico de volumen.

## Acciones seguras

- Reiniciar el relevo del **servicio afectado** (no la base). Un `TOMADO` huérfano lo vuelve a tomar el
  siguiente relevo por diseño (ADR-018).
- **Nunca** marcar `publicado_en`/`estado` a mano para "vaciar": miente sobre qué se publicó. (El ensayo de
  alarmas lo hace solo para simular al relevo que vuelve, sobre una base desechable.)
- En ruta de dinero (`nucleo_financiero`, `aportes`, `entregas`): tratar como riesgo financiero.

## Recuperación

- Ningún evento se pierde mientras la fila siga en su tabla. Drenada la cola, los consumidores aplican
  una sola vez (clave `(id_evento, consumidor)`); la reentrega no duplica (R04 del ensayo de reanudación).

## Validación

```bash
python despliegue/alertas/evaluar_alarmas.py --psql "<comando psql>" --registro alarmas.jsonl
# ALR-03 RESUELTA; el conteo de la evidencia baja a cero en los minutos siguientes
```

## Escalamiento

- Cola que no baja con Kafka y publicador sanos: dueño del servicio (puede ser un defecto del publicador).
- `FALLIDO` repetido con el mismo `ultimo_error`: tratarlo como incidente de seguridad solo si el error
  sugiere acceso indebido; si no, defecto de producto.
- Esta regla se evalúa sobre las tablas de cada servicio con un rol de operación de solo lectura; no la
  ejecute un servicio sobre el esquema de otro (invariante 11).

## Ver también

[[kafka-down]] · [[postgres-down]] · [[ALR-02-atraso-de-aportes]]
