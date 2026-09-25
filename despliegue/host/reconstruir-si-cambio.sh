#!/usr/bin/env bash
# Autodespliegue REAL de TEST: si `origin/test` avanzo, reconstruye SOLO las imagenes
# que ese commit toca —de a una— y recien entonces dispara el despliegue en Coolify.
#
# Por que no lo construye Coolify: al hacerlo, compilaba los 15 servicios a la vez
# (reescribe los Dockerfile para inyectar sus ARG, asi que no reusa cache) y con
# `-Xmx3g` por build eso son 45 GB de heap pedidos en una maquina de 23 GB que ademas
# sostiene Atlas. Se midio: 16 JVM, carga 49, rumbo al OOM.
#
# Por que un temporizador y no un puerto escuchando: no hace falta abrir nada nuevo al
# internet. El webhook de GitHub ya llega a Coolify; esto solo mira si la rama avanzo.
#
# Vive en el host en /opt/aportaya/bin/. Se versiona aca porque las tres fallas del
# 2026-09-21 estaban en este archivo y nadie podia verlas en revision. Instalar con:
#   install -m 755 despliegue/host/reconstruir-si-cambio.sh /opt/aportaya/bin/
set -uo pipefail

# Una construccion a la vez. Hoy lo garantiza systemd —no arranca una segunda instancia
# del servicio mientras la primera corre—, pero este script tambien se corre a mano para
# forzar un despliegue, y dos construcciones simultaneas sobre el mismo arbol de trabajo
# se pisan: la segunda hace `git checkout` de otro commit debajo de la primera. El flock
# es no bloqueante a proposito: si ya hay una corriendo, esta se va y el temporizador la
# vuelve a llamar en dos minutos.
exec 9>/opt/aportaya/reconstruir.lock
if ! flock -n 9; then
  echo "[$(date +%FT%T)] ya hay una reconstruccion en curso; salgo" >> /opt/aportaya/logs/autodespliegue.log
  exit 0
fi

cd /opt/aportaya/repo
REG=/opt/aportaya/logs/autodespliegue.log
marca() { echo "[$(date +%FT%T)] $*" >> "$REG"; }

REMOTO=$(git ls-remote https://github.com/PabloArauzCaballero/PasanakuBackend.git refs/heads/test | cut -f1)
[ -n "$REMOTO" ] || { marca "no se pudo leer la rama test"; exit 0; }
ANTERIOR=$(cat /opt/aportaya/ultimo-construido 2>/dev/null || echo "")
[ "$REMOTO" = "$ANTERIOR" ] && exit 0

marca "test avanzo: ${ANTERIOR:0:7} -> ${REMOTO:0:7}"
git fetch -q --depth 50 origin test || { marca "fetch fallo"; exit 1; }
CAMBIOS=$(git diff --name-only "${ANTERIOR:-HEAD}" FETCH_HEAD 2>/dev/null || echo TODO)
git checkout -q FETCH_HEAD

# Una falla de construccion NO puede pasar inadvertida: antes se marcaba y el script
# seguia igual hasta `desplegando`, asi que el log decia «listo en <sha>» mientras
# Coolify levantaba las imagenes viejas. El 2026-09-21 los tres fronts fallaron y las
# cuatro superficies contestaron 200 con codigo de dias atras.
FALLAS=0
FALLIDAS=""
construir() {
  local que="$1"
  marca "construyendo $que"
  "$@" >> "$REG" 2>&1 || {
    marca "FALLA construyendo $que"
    FALLAS=$((FALLAS + 1))
    FALLIDAS="$FALLIDAS $que"
    return 1
  }
}

# Lo troncal (plataforma, esquema, gradle) obliga a rehacer los quince servicios.
# `plataforma/gateway/` es SOLO el gateway; el resto de plataforma/ lo comparten los
# catorce servicios y obliga a rehacerlos.
TRONCAL='^(plataforma/(comun-|$)|buildSrc/|gradle|settings.gradle|build.gradle|despliegue/Dockerfile$)'
if [ "$CAMBIOS" = "TODO" ] || echo "$CAMBIOS" | grep -qE "$TRONCAL"; then
  SERVICIOS=$(ls -d servicios/*/ | xargs -n1 basename)
  construir /opt/aportaya/bin/construir.sh gateway plataforma
else
  SERVICIOS=$(echo "$CAMBIOS" | grep -oE '^servicios/[^/]+' | cut -d/ -f2 | sort -u)
  echo "$CAMBIOS" | grep -q '^plataforma/gateway/' && construir /opt/aportaya/bin/construir.sh gateway plataforma
fi
for s in ${SERVICIOS:-}; do construir /opt/aportaya/bin/construir.sh "$s" servicios; done

# El esquema viaja con el despliegue, pero su imagen lleva sql/ adentro.
#
# La segunda condicion no es defensiva de mas: los quince servicios la piden con
# `pull_policy: never` y `condition: service_completed_successfully`, asi que si el tag
# no esta en el host Coolify para los contenedores viejos, falla al crear los nuevos
# («No such image») y el backend queda en CERO. Reconstruirla solo cuando el commit toca
# sql/ hacia que no se recuperara sola nunca. Medido dos veces: 2026-09-17 y 2026-09-21.
if echo "$CAMBIOS" | grep -qE '^(sql/|despliegue/aplicar-esquema.sh|despliegue/Dockerfile.esquema)' \
   || ! docker image inspect aportaya/esquema:test >/dev/null 2>&1; then
  construir docker build -f despliegue/Dockerfile.esquema -t aportaya/esquema:test .
fi

# Todo workspace nuevo del monorepo se agrega ACA y en los dos Dockerfile: si falta en
# el Dockerfile, `yarn install --immutable` se cae en getWorkspaceByDescriptor; si falta
# aca, un commit que toque solo ese paquete no reconstruye ningun front.
FRONT_ANGULAR='^(packages/ui/|packages/tokens/|packages/tutoriales/|packages/simulado/|packages/dominio-cliente/|clientes/angular/)'
echo "$CAMBIOS" | grep -qE "^apps/backoffice/|$FRONT_ANGULAR" && \
  construir docker build -f apps/backoffice/docker/Dockerfile.backoffice --build-arg CONF_NGINX=apps/backoffice/docker/nginx.desplegado.conf -t aportaya/backoffice:test .
echo "$CAMBIOS" | grep -qE "^apps/web/|$FRONT_ANGULAR" && \
  construir docker build -f apps/web/docker/Dockerfile.web -t aportaya/web:test .

# El movil va por su guion, NO por `docker build` pelado: el Dockerfile trae los clientes
# con `COPY clientes/dart clientes/dart` desde el clon, y esa carpeta no esta versionada
# —la genera Gradle sin cache, que es lo que hace construir-movil-web.sh antes de
# construir—. Con el docker build pelado el temporizador metia siempre los clientes de la
# ultima corrida a mano: del 17 al 21 de septiembre la app movil fallaba con
# `Type 'ContratoVigente' not found` y se desplegaba la imagen vieja sin avisar.
# Tambien lo dispara un cambio de contrato, que es lo que deja viejo al cliente generado.
echo "$CAMBIOS" | grep -qE '^(apps/movil/|packages/diseno_flutter/|packages/tokens/|servicios/[^/]+/src/main/resources/openapi/)' && \
  construir /opt/aportaya/bin/construir-movil-web.sh

# No se despliega un sha cuyas imagenes no construyeron todas: desplegar igual es lo que
# hacia que el log dijera «listo» sobre imagenes viejas. Se marca el sha como visto para
# no reconstruirlo cada dos minutos para siempre; el arreglo va en un commit nuevo.
if [ "$FALLAS" -gt 0 ]; then
  echo "$REMOTO" > /opt/aportaya/ultimo-construido
  echo "${REMOTO:0:7}:$FALLIDAS" > /opt/aportaya/ultimo-fallido
  marca "SIN DESPLEGAR — $FALLAS construccion(es) fallaron:$FALLIDAS"
  marca "el sha ${REMOTO:0:7} queda marcado; corregir y empujar un commit nuevo"
  exit 1
fi

marca "desplegando"
/opt/aportaya/bin/disparar-despliegue.sh >> "$REG" 2>&1
echo "$REMOTO" > /opt/aportaya/ultimo-construido
rm -f /opt/aportaya/ultimo-fallido
marca "listo en ${REMOTO:0:7}"
