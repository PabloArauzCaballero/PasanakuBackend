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
# Avisa, NO instala: este guion corre como root en el host, y `test` no exige revision.
# Instalarse solo desde la rama le daria una consola root a cualquiera que empuje a
# `test` (revision de seguridad, 2026-10-28). La instalacion sigue siendo a mano; lo que
# ya no puede pasar es que la copia del host quede distinta sin que el log lo diga.
actualizar_guion() {
  local nuevo=/opt/aportaya/repo/despliegue/host/reconstruir-si-cambio.sh
  local actual=/opt/aportaya/bin/reconstruir-si-cambio.sh
  if [ -f "$nuevo" ] && ! cmp -s "$nuevo" "$actual"; then
    marca "AVISO: el guion del host difiere del de test; instalar a mano (despliegue/host/README.md)"
  fi
}

REMOTO=$(git ls-remote https://github.com/PabloArauzCaballero/PasanakuBackend.git refs/heads/test | cut -f1)
[ -n "$REMOTO" ] || { marca "no se pudo leer la rama test"; exit 0; }
ANTERIOR=$(cat /opt/aportaya/ultimo-construido 2>/dev/null || echo "")
[ "$REMOTO" = "$ANTERIOR" ] && exit 0

marca "test avanzo: ${ANTERIOR:0:7} -> ${REMOTO:0:7}"
git fetch -q --depth 50 origin test || { marca "fetch fallo"; exit 1; }
# El diff entre dos commits solo necesita los dos arboles, no la historia entre ellos. Con
# `--depth 50` el commit anterior puede quedar afuera si el salto es grande: el diff falla y
# CAMBIOS vale TODO. Se trae el anterior por su sha para no caer ahi sin necesidad.
if [ -n "$ANTERIOR" ] && ! git cat-file -e "${ANTERIOR}^{commit}" 2>/dev/null; then
  git fetch -q --depth 1 origin "$ANTERIOR" 2>/dev/null || marca "no se pudo traer ${ANTERIOR:0:7}; se reconstruye todo"
fi
CAMBIOS=$(git diff --name-only "${ANTERIOR:-HEAD}" "$REMOTO" 2>/dev/null || echo TODO)
[ -n "$CAMBIOS" ] || CAMBIOS=TODO
# `-f`: la generacion de clientes reescribe archivos versionados de `clientes/dart`, y con
# ellos modificados el checkout fallaba en silencio (sin `set -e`): se desplegaba el HEAD
# viejo y el log decia «listo en <sha nuevo>». Del 29 al 30/10 TEST quedo asi en 9f642eb.
# Este clon no es lugar de trabajo; lo que pisa `-f` se regenera en esta misma corrida.
if ! git checkout -q -f "$REMOTO" || [ "$(git rev-parse HEAD)" != "$REMOTO" ]; then
  marca "FALLA el checkout de ${REMOTO:0:7}; el arbol sigue en $(git rev-parse --short HEAD)"
  exit 1
fi
# TODO tiene que alcanzar a todo, no solo a los servicios: cada condicion de abajo lo mira.
TODO_O() { [ "$CAMBIOS" = "TODO" ] || echo "$CAMBIOS" | grep -qE "$1"; }
# Una imagen que no esta en el host se reconstruye aunque el commit no la toque: el compose
# las pide con `pull_policy: never`, y una faltante deja ese servicio caido despues del
# despliegue. El 2026-10-29 la limpieza de Coolify borro backoffice y web entre el build y el
# despliegue; nada las reponia y el despliegue fallo con «No such image».
falta() { ! docker image inspect "$1" >/dev/null 2>&1; }

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
falta aportaya/gateway:test && construir /opt/aportaya/bin/construir.sh gateway plataforma
for s in $(ls -d servicios/*/ | xargs -n1 basename); do
  falta "aportaya/$s:test" && SERVICIOS="${SERVICIOS:-} $s"
done
SERVICIOS=$(echo ${SERVICIOS:-} | tr ' ' '\n' | sort -u)
for s in ${SERVICIOS:-}; do construir /opt/aportaya/bin/construir.sh "$s" servicios; done

# El esquema viaja con el despliegue, pero su imagen lleva sql/ adentro.
#
# La segunda condicion no es defensiva de mas: los quince servicios la piden con
# `pull_policy: never` y `condition: service_completed_successfully`, asi que si el tag
# no esta en el host Coolify para los contenedores viejos, falla al crear los nuevos
# («No such image») y el backend queda en CERO. Reconstruirla solo cuando el commit toca
# sql/ hacia que no se recuperara sola nunca. Medido dos veces: 2026-09-17 y 2026-09-21.
if TODO_O '^(sql/|despliegue/aplicar-esquema.sh|despliegue/Dockerfile.esquema)' \
   || ! docker image inspect aportaya/esquema:test >/dev/null 2>&1; then
  construir docker build -f despliegue/Dockerfile.esquema -t aportaya/esquema:test .
fi

# Todo workspace nuevo del monorepo se agrega ACA y en los dos Dockerfile: si falta en
# el Dockerfile, `yarn install --immutable` se cae en getWorkspaceByDescriptor; si falta
# aca, un commit que toque solo ese paquete no reconstruye ningun front.
FRONT_ANGULAR='^(packages/ui/|packages/tokens/|packages/tutoriales/|packages/simulado/|packages/dominio-cliente/|clientes/angular/)'
# Los dos Dockerfile copian `clientes/angular`, que no se versiona (ADR-016): sin
# regenerarlo, un cambio de contrato construye el front contra el cliente de la corrida
# anterior. Se regenera una vez, antes de cualquiera de los dos.
CONTRATO='^servicios/[^/]+/src/main/resources/openapi/'
FALTA_BACKOFFICE=0; falta aportaya/backoffice:test && FALTA_BACKOFFICE=1
FALTA_WEB=0; falta aportaya/web:test && FALTA_WEB=1
if TODO_O "^apps/backoffice/|^apps/web/|$FRONT_ANGULAR|$CONTRATO" || [ $FALTA_BACKOFFICE = 1 ] || [ $FALTA_WEB = 1 ]; then
  construir /opt/aportaya/bin/generar-clientes.sh
fi
{ TODO_O "^apps/backoffice/|$FRONT_ANGULAR|$CONTRATO" || [ $FALTA_BACKOFFICE = 1 ]; } && \
  construir docker build -f apps/backoffice/docker/Dockerfile.backoffice --build-arg CONF_NGINX=apps/backoffice/docker/nginx.desplegado.conf --label "aportaya.commit=$REMOTO" -t aportaya/backoffice:test .
{ TODO_O "^apps/web/|$FRONT_ANGULAR|$CONTRATO" || [ $FALTA_WEB = 1 ]; } && \
  construir docker build -f apps/web/docker/Dockerfile.web --label "aportaya.commit=$REMOTO" -t aportaya/web:test .

# El movil va por su guion, NO por `docker build` pelado: el Dockerfile trae los clientes
# con `COPY clientes/dart clientes/dart` desde el clon, y esa carpeta no esta versionada
# —la genera Gradle sin cache, que es lo que hace construir-movil-web.sh antes de
# construir—. Con el docker build pelado el temporizador metia siempre los clientes de la
# ultima corrida a mano: del 17 al 21 de septiembre la app movil fallaba con
# `Type 'ContratoVigente' not found` y se desplegaba la imagen vieja sin avisar.
# Tambien lo dispara un cambio de contrato, que es lo que deja viejo al cliente generado.
{ TODO_O '^(apps/movil/|packages/diseno_flutter/|packages/tokens/|servicios/[^/]+/src/main/resources/openapi/)' \
    || falta aportaya/movil-web:test; } && \
  construir /opt/aportaya/bin/construir-movil-web.sh

# No se despliega un sha cuyas imagenes no construyeron todas: desplegar igual es lo que
# hacia que el log dijera «listo» sobre imagenes viejas. Se marca el sha como visto para
# no reconstruirlo cada dos minutos para siempre; el arreglo va en un commit nuevo.
# Una construccion «exitosa» no prueba que la imagen sea la de este commit: el
# 2026-10-27 el log dijo «listo en 608cdf1» con aportaya/backoffice:test de dos dias
# antes. Cada front construido aca lleva la etiqueta aportaya.commit; si se pidio
# reconstruirlo y la etiqueta no es este sha, cuenta como falla y no se despliega.
verificar_imagen() {
  local imagen="$1" etiqueta
  etiqueta=$(docker image inspect -f '{{index .Config.Labels "aportaya.commit"}}' "$imagen" 2>/dev/null || echo "")
  if [ "$etiqueta" != "$REMOTO" ]; then
    marca "FALLA $imagen quedo en '${etiqueta:0:7}', no en ${REMOTO:0:7}"
    FALLAS=$((FALLAS + 1))
    FALLIDAS="$FALLIDAS $imagen"
  fi
}
{ TODO_O "^apps/backoffice/|$FRONT_ANGULAR|$CONTRATO" || [ $FALTA_BACKOFFICE = 1 ]; } && verificar_imagen aportaya/backoffice:test
{ TODO_O "^apps/web/|$FRONT_ANGULAR|$CONTRATO" || [ $FALTA_WEB = 1 ]; } && verificar_imagen aportaya/web:test
# Y antes de desplegar, que esten TODAS: una que falta tira su servicio aunque el resto este bien.
for imagen in aportaya/gateway:test aportaya/esquema:test aportaya/backoffice:test aportaya/web:test \
              aportaya/movil-web:test $(ls -d servicios/*/ | xargs -n1 basename | sed 's|.*|aportaya/&:test|'); do
  if falta "$imagen"; then
    marca "FALLA $imagen no esta en el host"
    FALLAS=$((FALLAS + 1))
    FALLIDAS="$FALLIDAS $imagen"
  fi
done

if [ "$FALLAS" -gt 0 ]; then
  echo "$REMOTO" > /opt/aportaya/ultimo-construido
  echo "${REMOTO:0:7}:$FALLIDAS" > /opt/aportaya/ultimo-fallido
  marca "SIN DESPLEGAR — $FALLAS construccion(es) fallaron:$FALLIDAS"
  marca "el sha ${REMOTO:0:7} queda marcado; corregir y empujar un commit nuevo"
  actualizar_guion
  exit 1
fi

marca "desplegando"
/opt/aportaya/bin/disparar-despliegue.sh >> "$REG" 2>&1
echo "$REMOTO" > /opt/aportaya/ultimo-construido
rm -f /opt/aportaya/ultimo-fallido
marca "listo en ${REMOTO:0:7}"

# Si la copia del host quedo distinta de la versionada, el log lo dice (el 2026-10-27
# corria otra version y nadie lo sabia).
actualizar_guion
