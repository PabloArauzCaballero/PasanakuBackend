# Carga medida (H4) — k6

Seis escenarios: `login`, `saldo`, `transferencia`, `replay-idempotente`,
`retiro`, `outbox`. Todos pegan contra el gateway detrás de NGINX
(`http://localhost` por defecto — la misma entrada pública de cualquier
cliente), nunca directo a un servicio.

## Cómo correr uno

```bash
docker run --rm -i --network aportaya-interna \
  -e APORTAYA_BASE_URL=http://nginx \
  -v "$(pwd)/carga/k6:/scripts" grafana/k6 run /scripts/transferencia.js
```

Desde el host, con el perfil `base` publicando el puerto 80:

```bash
k6 run carga/k6/transferencia.js
```

## Baseline (H4.S1.M2) — tres corridas, con lo que pide el encargo

```bash
for i in 1 2 3; do
  echo "=== corrida $i ==="
  docker run --rm -i --network aportaya-interna -e APORTAYA_BASE_URL=http://nginx \
    -v "$(pwd)/carga/k6:/scripts" grafana/k6 run --summary-export=/scripts/resultado-$i.json /scripts/transferencia.js
done
```

Junto con cada corrida, en paralelo (no dentro de k6 — ver `outbox.js`):

```bash
# pg_locks
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM pg_locks;"

# outbox_pending — la tabla real es <esquema>.evento_dominio (sql/15_infra/mensajeria.sql)
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE publicado_en IS NULL;"

# CPU / memoria del contenedor
docker stats --no-stream aportaya-gateway aportaya-postgres

# heap de la JVM (gateway)
docker exec aportaya-gateway wget -qO- http://127.0.0.1:8080/actuator/metrics/jvm.memory.used

# lag de Kafka — cuando haya un consumer group real corriendo (todavía no
# hay ninguno productivo, solo los tópicos): kafka-consumer-groups --describe
```

## Por qué las respuestas no son 2xx todavía

Ningún escenario tiene un servicio real detrás hoy (solo el gateway está
construido en esta máquina — ver `docs/auditoria-produccion/carriles/PR5-ci-operacion.md`,
H1.S2.M6). Los datos son sintéticos a propósito (regla 97.6): no hay cuenta,
usuario ni instrumento real detrás de ningún UUID. Lo que estos scripts
miden HOY es el costo del camino hasta el rechazo (routing, rate limiting,
serialización) — sirven para correr contra el stack completo apenas los
otros carriles publiquen sus servicios, sin reescribir nada.
