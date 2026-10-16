#!/usr/bin/env bash
# Crea (si no existen) los topicos de Kafka que los servicios publican. Idempotente.
#
#   bash despliegue/host/crear-topicos.sh [contenedor-de-kafka]       # por omision: aportaya-kafka
#
# Por que hace falta (carril G, hallado levantando el stack real): el broker arranca con
# KAFKA_AUTO_CREATE_TOPICS_ENABLE=false (despliegue/compose/base.yml) y nadie creaba los topicos. El relevo del outbox
# publica en `aportaya.<esquema>.<hecho>`; publicar a un topico que no existe bloquea el envio hasta el timeout del
# productor y el evento termina FALLIDO, asi que NINGUN evento llegaba a ningun consumidor (garantia -> nucleo, organizador
# -> grupos). La lista sale del codigo (scripts/listar_topicos.py): un evento nuevo entra solo en la proxima corrida.
#
# Ejecutarlo en el host de TEST despues de levantar Kafka y antes de desplegar (o despues de agregar un evento).
# Una variante equivalente es dejar KAFKA_AUTO_CREATE_TOPICS_ENABLE=true en el broker; es una decision de operacion.
set -uo pipefail
KAFKA="${1:-${KAFKA_CONTENEDOR:-aportaya-kafka}}"
BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PARALELO="${KAFKA_TOPICOS_PARALELO:-3}"

docker inspect "$KAFKA" >/dev/null 2>&1 || { echo "crear-topicos: no existe el contenedor '$KAFKA'"; exit 2; }
# En Git Bash de Windows el python nativo no entiende /c/...: se le pasa la ruta en formato mixto (C:/...).
RAIZ_PY="$(cygpath -m "$RAIZ" 2>/dev/null || printf '%s' "$RAIZ")"
LISTA="$(PYTHONUTF8=1 python3 "$RAIZ_PY/scripts/listar_topicos.py" 2>/dev/null || PYTHONUTF8=1 python "$RAIZ_PY/scripts/listar_topicos.py")"
LISTA="${LISTA//$'\r'/}"   # el python de Windows escribe CRLF
[ -n "$LISTA" ] || { echo "crear-topicos: lista vacia"; exit 3; }
TOTAL="$(printf '%s\n' "$LISTA" | wc -l | tr -d ' ')"
echo "crear-topicos: $TOTAL topicos en $KAFKA ($BOOTSTRAP), $PARALELO a la vez"

# Cada kafka-topics es una JVM: con el heap acotado, de a pocas, entran en el contenedor del ensayo (mem_limit 640m).
printf '%s\n' "$LISTA" | docker exec -i -e KAFKA_HEAP_OPTS="-Xmx64m -Xms32m" "$KAFKA" sh -c \
  "xargs -P $PARALELO -I{} kafka-topics --bootstrap-server $BOOTSTRAP --create --if-not-exists --topic {} --partitions 1 --replication-factor 1 >/dev/null 2>/tmp/topicos-err.txt"
RC=$?
CREADOS="$(docker exec -e KAFKA_HEAP_OPTS="-Xmx64m" "$KAFKA" kafka-topics --bootstrap-server "$BOOTSTRAP" --list 2>/dev/null | grep -c '^aportaya\.')"
echo "crear-topicos: rc=$RC, topicos aportaya.* existentes: $CREADOS de $TOTAL"
[ "$CREADOS" -ge "$TOTAL" ] || { docker exec "$KAFKA" sh -c 'tail -5 /tmp/topicos-err.txt' 2>/dev/null; exit 4; }
exit 0
