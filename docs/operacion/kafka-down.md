# Runbook — Kafka caído o inalcanzable

## Síntomas

- Los consumidores de eventos dejan de avanzar; `evento_dominio.publicado_en`
  se queda en `NULL` para filas nuevas (el outbox no logra publicar) — ver
  [[outbox-backlog]] para la consulta y el runbook específico de ese síntoma.
- `aportaya-kafka` no aparece `Healthy` en `docker ps`, o el healthcheck
  (`kafka-broker-api-versions --bootstrap-server kafka:9092`,
  `despliegue/compose/base.yml`) falla.
- Los servicios que sí llaman a Kafka de forma síncrona (ninguno debería,
  por diseño — es outbox asíncrono) no deberían ni notarlo en su
  `readiness`; si `readiness` cae junto con Kafka, es un hallazgo aparte
  (algo está acoplado que no debería).

## Dashboards y consultas

```bash
docker ps --filter "name=aportaya-kafka" --format "{{.Names}}: {{.Status}}"
docker logs --since 10m aportaya-kafka

# Topicos y su estado (KRaft, sin ZooKeeper — despliegue/compose/base.yml)
docker exec aportaya-kafka kafka-topics --bootstrap-server localhost:9092 --list
docker exec aportaya-kafka kafka-topics --bootstrap-server localhost:9092 --describe

# Lag de un grupo consumidor (cuando exista uno productivo — hoy ninguno,
# ver docs/auditoria-produccion/limites-de-recursos.md)
docker exec aportaya-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --list
docker exec aportaya-kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group <nombre>
```

## Diagnóstico

1. **¿El proceso está vivo?** `docker logs aportaya-kafka` — con KRaft, un
   fallo común es el `CLUSTER_ID` (`despliegue/compose/base.yml`) que no
   coincide con el que ya escribió en su volumen (pasa si se cambia el ID
   sin bajar el volumen).
2. **¿Está vivo pero no acepta broker API?** El healthcheck del propio
   compose ya lo mide (`kafka-broker-api-versions`); si falla ahí, es un
   problema de arranque interno, no de red.
3. **¿Es alcanzable desde los servicios?** Cada servicio resuelve `kafka` en
   la red `aportaya-interna` (Docker DNS) — un fallo de resolución es
   infraestructura de red, no de Kafka en sí.

## Acciones seguras

- Reiniciar el contenedor si el proceso está colgado sin error claro:
  `docker restart aportaya-kafka`. Con KRaft y un solo nodo (dev), esto NO
  pierde datos — el log persiste en el volumen.
- **Nunca** borrar el volumen de Kafka para "destrabar" un arranque: se
  pierde el outbox no publicado todavía. Investigar el log primero.

## Recuperación

- **Contenedor caído, volumen sano:**
  `docker compose -f despliegue/compose/base.yml --profile base up -d kafka`.
  Los productores (el patrón outbox de cada servicio) reintentan solos —
  ningún evento se pierde porque nunca salió de la tabla transaccional hasta
  que Kafka confirma.
- **Volumen perdido:** los eventos NO publicados (`publicado_en IS NULL`) se
  reconstruyen solos apenas Kafka vuelve, porque siguen en
  `<esquema>.evento_dominio` (la fuente de verdad es la tabla, Kafka es el
  canal). Los eventos YA publicados y consumidos no se pueden recuperar de
  Kafka — si algún consumidor dependía de re-leer el tópico desde el
  principio, esa garantía se pierde con el volumen.

## Validación

```bash
docker exec aportaya-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
# outbox_pending vuelve a bajar (no se queda estancado) en los minutos
# siguientes — ver la consulta en carga/k6/README.md
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE publicado_en IS NULL;"
```

## Escalamiento

- Backlog de outbox que no baja después de que Kafka vuelve `Healthy`:
  escalar a quien tenga el contexto del consumidor específico — puede ser
  el consumidor el que está caído, no Kafka.
- Pérdida del volumen en producción con eventos ya consumidos por sistemas
  externos (si algún día los hay): escalar como incidente de datos, la
  garantía de "at-least-once" del broker no cubre un volumen borrado.

## Ver también

[[outbox-backlog]] · [[postgres-down]]
