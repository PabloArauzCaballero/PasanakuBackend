# Runbook — Backlog del outbox (eventos sin publicar)

## Síntomas

- `evento_dominio.publicado_en` queda en `NULL` para filas cada vez más
  viejas — el conteo de pendientes crece en vez de oscilar cerca de cero.
- Efectos que dependen de un evento (proyecciones, notificaciones) se
  retrasan o no aparecen, mientras la operación que los originó sí quedó
  registrada (el dato está, el efecto derivado no llegó).

## Dashboards y consultas

```bash
# Pendientes por esquema (repetir cambiando el esquema; nucleo_financiero
# es el de mayor volumen esperado — retiros y transferencias)
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE publicado_en IS NULL;"

# La más vieja sin publicar: si tiene más de unos minutos, algo dejó de
# publicar, no es solo un pico momentáneo
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT min(creado_en) FROM nucleo_financiero.evento_dominio WHERE publicado_en IS NULL;"
```

## Diagnóstico

1. **¿Kafka está sano?** Ver [[kafka-down]] primero — es la causa más común.
2. **¿El publicador del servicio está vivo?** El outbox se publica desde un
   proceso propio del servicio (poller o listener transaccional, según el
   patrón de `plataforma/comun-mensajeria`) — si el servicio está caído o
   reiniciando en bucle, nadie está leyendo la tabla.
3. **¿Hay un lock prolongado sobre `evento_dominio`?**
   `SELECT * FROM pg_locks l JOIN pg_stat_activity a ON l.pid = a.pid WHERE relation = 'nucleo_financiero.evento_dominio'::regclass;`
   — una transacción larga que no suelta esa tabla bloquea al publicador.
4. **¿Es solo un pico de volumen, no un atasco?** Comparar contra el
   baseline de `carga/k6/README.md` — un backlog que sube y baja solo, sin
   quedarse estancado, no es un incidente.

## Acciones seguras

- Reiniciar el publicador del servicio afectado (no la base, no Kafka, si
  esos dos ya están sanos): es el proceso más barato de reintentar.
- Si hay un lock prolongado de una transacción abandonada, terminarla igual
  que en [[postgres-down]] (`pg_terminate_backend`, solo transacciones
  `idle in transaction` viejas).

## Recuperación

- Ningún evento se pierde mientras la fila siga en `evento_dominio`: el
  outbox ES la fuente de verdad (patrón outbox — la publicación a Kafka es
  un efecto derivado, no el registro). Recuperar el publicador (reiniciarlo,
  o esperar a que Kafka vuelva) drena el backlog solo, sin intervención
  manual sobre los datos.
- **Nunca** marcar `publicado_en` a mano para "vaciar" el backlog: eso miente
  sobre qué se publicó de verdad, y cualquier consumidor que ya haya
  ejecutado su efecto dos veces (por un reintento normal) rompe la garantía
  de idempotencia que el propio outbox existe para dar.

## Validación

```bash
# El conteo de pendientes vuelve a bajar en los minutos siguientes
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE publicado_en IS NULL;"
```

Repetir la consulta cada pocos minutos hasta ver la tendencia bajar, no un
número único — un backlog que crece parejo con el tráfico normal no está
resuelto solo porque el conteo bajó una vez.

## Escalamiento

- Backlog que no baja después de confirmar Kafka sano y el publicador
  reiniciado: escalar al dueño del servicio — puede ser un bug en el
  publicador mismo, no infraestructura.
- Backlog en una ruta de dinero (`nucleo_financiero`, `aportes`, `entregas`)
  que supera varios minutos: tratar como riesgo financiero, no solo
  operativo — un pago que no notificó a tiempo genera reclamos.

## Ver también

[[kafka-down]] · [[postgres-down]]
