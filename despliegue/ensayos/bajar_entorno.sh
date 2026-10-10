#!/usr/bin/env bash
# Baja TODO lo que levantan arranque_completo.sh y construir_imagenes.sh: los servicios, la
# infraestructura, sus volumenes anonimos y las redes aportaya-d3-*. Solo toca recursos con el
# prefijo `aportaya-d3`; no toca `aportaya-interna` ni nada de otros trabajos.
#
#   bash despliegue/ensayos/bajar_entorno.sh          # baja
#   bash despliegue/ensayos/bajar_entorno.sh quiet    # sin charla
#   D3_DOCKER_APAGAR=1 bash despliegue/ensayos/bajar_entorno.sh   # ademas apaga Docker Desktop
source "$(dirname "${BASH_SOURCE[0]}")/arranque/lib.sh"
Q=""; [ "${1:-}" = quiet ] && Q=1
[ -z "$Q" ] && log "bajando el stack D3"
docker info >/dev/null 2>&1 || { [ -z "$Q" ] && log "Docker no esta corriendo: nada que bajar"; exit 0; }
generar_entorno >/dev/null 2>&1   # compose necesita un --env-file que exista
app down -v --remove-orphans >/dev/null 2>&1
infra down -v --remove-orphans >/dev/null 2>&1
# por si algo quedo huerfano
for p in "$PROYECTO_APP" "$PROYECTO_INFRA"; do
  ids="$(docker ps -aq --filter "label=com.docker.compose.project=$p" 2>/dev/null)"
  [ -n "$ids" ] && docker rm -f -v $ids >/dev/null 2>&1
done
docker network rm aportaya-d3-interna aportaya-d3-publica >/dev/null 2>&1
if [ -z "$Q" ]; then
  log "contenedores aportaya-d3 que quedan: $(docker ps -aq --filter name=aportaya-d3 | wc -l)"
  log "redes aportaya-d3 que quedan: $(docker network ls --format '{{.Name}}' | grep -c '^aportaya-d3')"
fi
if [ -n "${D3_DOCKER_APAGAR:-}" ]; then docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; fi
exit 0
