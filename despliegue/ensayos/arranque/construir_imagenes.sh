#!/usr/bin/env bash
# Construye las imagenes aportaya/<servicio>:test y aportaya/esquema:test EXACTAMENTE como las
# arma el host de TEST (/opt/aportaya/bin/construir.sh): con despliegue/Dockerfile y
# despliegue/Dockerfile.esquema, y con jOOQ introspeccionando una base viva con el esquema actual.
#
#   bash despliegue/ensayos/arranque/construir_imagenes.sh [esquema] [gateway] [servicio ...]
#   (sin argumentos: esquema, gateway y los quince servicios, DE A UNO)
#
# Va bajo el guardian de RAM (lo hace el guion de entrada o quien lo invoque):
#   bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash despliegue/ensayos/arranque/construir_imagenes.sh identidad
#
# UNICA diferencia con el build real: el Dockerfile se copia a $D3_DIR y se le agregan a la linea
# de Gradle `--max-workers=2`, `-Dorg.gradle.parallel=false` y `-Xmx1g`, porque Docker Desktop corre
# con 4 GB y gradle.properties pide `-Xmx3g`. El Dockerfile del repositorio NO se modifica.
# Se hace UNA imagen por corrida del guardian si se pasa una sola; con varias, de a una y con una
# pausa para soltar memoria entre cada una.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
cd "$RAIZ" || exit 2
generar_entorno
asegurar_docker || exit 3
# shellcheck disable=SC1090
set -a; source "$ENVF"; set +a

QUE=("$@"); [ "${#QUE[@]}" -eq 0 ] && QUE=(esquema gateway "${SERVICIOS_NEGOCIO[@]}")

# --- Dockerfile de Gradle acotado ---
DF="$D3_DIR/Dockerfile.ensayo"
sed 's#\./gradlew --no-daemon #./gradlew --no-daemon --max-workers=2 -Dorg.gradle.parallel=false "-Dorg.gradle.jvmargs=-Xmx1g -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" #' despliegue/Dockerfile > "$DF"
grep -q 'max-workers=2' "$DF" || { log "no pude acotar la linea de Gradle del Dockerfile"; exit 4; }
diff <(cat despliegue/Dockerfile) "$DF" | head -5 > "$EVID/dockerfile-ensayo.diff" || true

# --- Postgres de construccion (solo la base; el resto de la infra no hace falta para compilar) ---
log "base de construccion"
infra up -d --wait postgres >/dev/null || { log "no levanto postgres"; exit 5; }

log "esquema actual aplicado a la base de construccion (con la imagen del esquema, como TEST)"
docker build -q -f despliegue/Dockerfile.esquema -t aportaya/esquema:test . >/dev/null || { log "FALLA imagen esquema"; exit 6; }
docker run --rm --network aportaya-d3-interna -e PGHOST=postgres -e PGUSER="$BD_USUARIO_ADMIN" -e PGPASSWORD="$BD_CLAVE_ADMIN" -e PGDATABASE=pasanaku \
  aportaya/esquema:test 2>&1 | enmascarar | tee "$EVID/construccion-esquema-job.log" | tail -4

RC_TOTAL=0
for s in "${QUE[@]}"; do
  [ "$s" = esquema ] && continue
  ARGS=(--build-arg SERVICIO="$s")
  [ "$s" = gateway ] && ARGS+=(--build-arg MODULO=plataforma)
  log "construyendo aportaya/$s:test (libres Windows: $(libres_windows_mb) MB)"
  t0=$(date +%s)
  docker build --network host -f "$DF" "${ARGS[@]}" \
    --build-arg BD_URL_ADMIN="jdbc:postgresql://127.0.0.1:${D3_PUERTO_PG}/pasanaku" \
    --build-arg BD_USUARIO_ADMIN="$BD_USUARIO_ADMIN" --build-arg BD_CLAVE_ADMIN="$BD_CLAVE_ADMIN" \
    -t "aportaya/$s:test" . > "$D3_DIR/build-$s.log" 2>&1
  rc=$?; dur=$(( $(date +%s) - t0 ))
  enmascarar < "$D3_DIR/build-$s.log" | tail -60 > "$EVID/construccion-$s.log"
  if [ "$rc" -ne 0 ]; then log "FALLA construyendo $s (rc=$rc, ${dur}s) -> $EVID/construccion-$s.log"; RC_TOTAL=1; else log "OK $s (${dur}s)"; fi
  docker image prune -f >/dev/null 2>&1
  sleep 5
done

log "bajando la base de construccion"
infra down -v >/dev/null 2>&1
docker images --format '{{.Repository}}:{{.Tag}} {{.Size}}' | grep '^aportaya/' | tee "$EVID/imagenes-construidas.txt"
exit $RC_TOTAL
