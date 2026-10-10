#!/usr/bin/env bash
# Funciones comunes del ensayo de arranque completo (carril D3). Se hace `source` desde los
# guiones; no se ejecuta solo.
#
# Reglas que respeta, heredadas del guardian de RAM del repositorio:
#   · todo lo pesado corre bajo C:/Users/Usuario/.claude/ram-guard/heavy.sh (lo aplican los
#     guiones de entrada, no esta libreria);
#   · ningun secreto vive en el repositorio: se generan al vuelo en $D3_DIR (fuera del clon).
set -uo pipefail
export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'

LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RAIZ="$(cd "$LIB_DIR/../../.." && pwd)"
D3_DIR="${D3_DIR:-${TEMP:-/tmp}/aportaya-d3}"
ENVF="$D3_DIR/entorno.env"
EVID="${D3_EVIDENCIA:-$RAIZ/evidencia/carril-D3}"
PROYECTO_INFRA="aportaya-d3-infra"
PROYECTO_APP="aportaya-d3"
SERVICIOS_NEGOCIO=(identidad aportes auditoria cumplimiento entregas erp garantia grupos inversiones notificaciones nucleo-financiero organizador publicidad tarifas transparencia)
export D3_PUERTO_PG="${D3_PUERTO_PG:-5444}" D3_PUERTO_PGBOUNCER="${D3_PUERTO_PGBOUNCER:-6444}" D3_PUERTO_GATEWAY="${D3_PUERTO_GATEWAY:-18080}"

mkdir -p "$D3_DIR" "$EVID"

log() { echo "[d3 $(date +%H:%M:%S)] $*"; }

libres_windows_mb() { powershell.exe -NoProfile -Command '[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)' | tr -d '\r'; }

# ---- secretos sinteticos --------------------------------------------------------------------
# Se generan UNA vez por D3_DIR y se reusan: asi un reinicio del ensayo no invalida los usuarios
# sembrados. Nunca se imprimen ni se copian al repositorio.
generar_entorno() {
  [ -f "$ENVF" ] && return 0
  local h; h() { openssl rand -hex 32; }
  umask 077
  cat > "$ENVF" <<ENV
# Secretos SINTETICOS del ensayo D3, generados $(date +%FT%T). Fuera del repositorio. No reusar.
PERFIL_SPRING=${PERFIL_SPRING:-local}
BD_USUARIO_ADMIN=pasanaku
BD_CLAVE_ADMIN=$(h)
BD_CLAVE=$(h)
ARCHIVOS_USUARIO=aportaya
ARCHIVOS_CLAVE=$(h)
SEGURIDAD_PIMIENTA=$(h)
WEBHOOK_SECRETO=$(h)
CERTIFICADOS_CLAVE_FIRMA=$(h)
CUENTA_PUENTE_CUSTODIA=00000000-0000-0000-0000-0000000000c0
BASE_URL_PUBLICA=http://localhost
SIN_NIT_EMISOR=1234567890
NOTIFICATION_EMAIL_PROVIDER=local
GMAIL_CLIENT_ID=
GMAIL_CLIENT_SECRET=
GMAIL_REFRESH_TOKEN=
GMAIL_FROM_EMAIL=
JWT_CLAVE_FIRMA=
INVITACIONES_CLAVE=$(h)
QR_INTERNO_CLAVE=$(h)
WEB_HOSTS_PERMITIDOS=localhost
URL_APP=http://localhost
APORTAYA_CORS_ORIGENES=http://localhost
ENV
  log "entorno sintetico generado en $ENVF (fuera del repo)"
}

# Variante del entorno para un escenario: quita variables (quedan en blanco, que es lo que hace
# Coolify con un ${X} sin valor) o las pisa. Escribe un archivo aparte; nunca toca el original.
# uso: entorno_de_escenario "OMITIR=INVITACIONES_CLAVE,QR_INTERNO_CLAVE" "PERFIL_SPRING=staging"
entorno_de_escenario() {
  local destino="$D3_DIR/entorno.escenario.env" par
  cp "$ENVF" "$destino"
  for par in "$@"; do
    case "$par" in
      OMITIR=*) local v; for v in $(echo "${par#OMITIR=}" | tr ',' ' '); do sed -i "/^$v=/d" "$destino"; done ;;
      *=*) local k="${par%%=*}"; sed -i "/^$k=/d" "$destino"; echo "$par" >> "$destino" ;;
    esac
  done
  ENVF="$destino"
}

enmascarar() { # lee stdin, tapa cualquier valor secreto del entorno
  local f="$ENVF" sed_args=() k v
  while IFS='=' read -r k v; do
    case "$k" in BD_CLAVE*|ARCHIVOS_CLAVE|SEGURIDAD_PIMIENTA|WEBHOOK_SECRETO|CERTIFICADOS_CLAVE_FIRMA|INVITACIONES_CLAVE|QR_INTERNO_CLAVE|JWT_CLAVE_FIRMA)
      [ -n "$v" ] && sed_args+=(-e "s/$v/<$k>/g") ;; esac
  done < "$f"
  sed "${sed_args[@]}"
}

# ---- compose ---------------------------------------------------------------------------------
infra() { ( cd "$RAIZ" && docker compose -p "$PROYECTO_INFRA" --env-file "$ENVF" -f despliegue/ensayos/arranque/infra.yml "$@" ); }
app()   { ( cd "$RAIZ" && docker compose -p "$PROYECTO_APP" --env-file "$ENVF" -f docker-compose.coolify.yml -f despliegue/ensayos/arranque/servicios.override.yml "$@" ); }

contenedor() { docker ps -a --filter "label=com.docker.compose.project=$PROYECTO_APP" --filter "label=com.docker.compose.service=$1" --format '{{.Names}}' | head -1; }

estado_salud() { # healthy | unhealthy | starting | exited | restarting | ausente
  local c; c="$(contenedor "$1")"; [ -z "$c" ] && { echo ausente; return; }
  local st; st="$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null)"
  case "$st" in running) docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}sin-healthcheck{{end}}' "$c" ;; *) echo "$st" ;; esac
}

readiness_directo() { # cuerpo de /actuator/health/readiness desde adentro, o vacio
  local c; c="$(contenedor "$1")"; [ -z "$c" ] && return 0
  docker exec "$c" wget -q -O - http://127.0.0.1:8080/actuator/health/readiness 2>/dev/null
}

# /actuator/health completo (incluye la sonda `base` de SaludDeLaBase, que SI pega a la base):
# readiness por si solo NO la mira (el grupo `readiness` trae solo readinessState), asi que un
# servicio con la credencial de la base rechazada sale «healthy» en docker y no lo esta.
health_directo() {
  local c; c="$(contenedor "$1")"; [ -z "$c" ] && return 0
  docker exec "$c" wget -q -O - http://127.0.0.1:8080/actuator/health 2>/dev/null
}

# Espera hasta `tope` segundos a que TODOS los servicios dados esten healthy. Imprime una linea por
# servicio al terminar. Devuelve 0 si todos sanos.
esperar_sanos() {
  local tope="$1"; shift
  local t=0 pendientes ok
  while [ "$t" -lt "$tope" ]; do
    pendientes=()
    for s in "$@"; do [ "$(estado_salud "$s")" = healthy ] || pendientes+=("$s"); done
    [ "${#pendientes[@]}" -eq 0 ] && return 0
    sleep 10; t=$((t + 10))
  done
  return 1
}

# ---- RAM: muestreo del conjunto ----------------------------------------------------------------
# Suma el uso de memoria de los contenedores de los dos proyectos cada 10 s y deja el maximo.
iniciar_muestreo_ram() {
  rm -f "$D3_DIR/ram.csv"
  ( while true; do
      docker stats --no-stream --format '{{.Name}};{{.MemUsage}}' 2>/dev/null \
        | grep -E '^aportaya-d3' \
        | awk -F';' -v ts="$(date +%H:%M:%S)" '{ split($2,a," / "); v=a[1]; m=0;
              if (v ~ /GiB/) { sub("GiB","",v); m=v*1024 } else if (v ~ /MiB/) { sub("MiB","",v); m=v } else if (v ~ /KiB/) { sub("KiB","",v); m=v/1024 }
              tot+=m; n++ } END { printf "%s,%d,%.0f\n", ts, n, tot }' >> "$D3_DIR/ram.csv"
      sleep 10
    done ) &
  MUESTREO_PID=$!
}
detener_muestreo_ram() { [ -n "${MUESTREO_PID:-}" ] && kill "$MUESTREO_PID" 2>/dev/null; }
ram_pico_mb() { [ -f "$D3_DIR/ram.csv" ] && sort -t, -k3 -n -r "$D3_DIR/ram.csv" | head -1 || echo "sin-datos"; }

# ---- docker desktop --------------------------------------------------------------------------
asegurar_docker() {
  INICIE_DOCKER=0
  if ! docker info >/dev/null 2>&1; then
    INICIE_DOCKER=1
    log "levantando Docker Desktop"
    ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
    for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
    docker info >/dev/null 2>&1 || { log "Docker no levanto"; return 3; }
  fi
  export INICIE_DOCKER
}
