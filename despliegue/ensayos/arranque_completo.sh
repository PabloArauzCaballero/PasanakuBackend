#!/usr/bin/env bash
# Arranque COMPLETO del stack de TEST en Docker local, desechable, y humo sobre el.
#
#   bash despliegue/ensayos/arranque_completo.sh            # levanta, espera readiness, humo, baja
#
# Reproduce lo que se despliega en TEST: docker-compose.coolify.yml SIN EDITAR (solo se le suman
# memoria y puertos con arranque/servicios.override.yml), imagenes aportaya/<servicio>:test
# construidas con despliegue/Dockerfile (arranque/construir_imagenes.sh), y PostgreSQL + PgBouncer +
# Redis + MinIO + Kafka aparte (arranque/infra.yml), como en /opt/aportaya/. Los secretos son
# sinteticos, se generan en $D3_DIR (fuera del repo) y nunca se imprimen.
#
# Corre BAJO el guardian de RAM (heavy.sh): se relanza a si mismo con el candado. Un candado de mas
# de 30 min se considera huerfano, asi que NO lo uses para construir imagenes (eso va por lotes en
# construir_imagenes.sh); aca solo se arranca, se prueba y se baja.
#
# Variables (todas opcionales):
#   D3_PERFIL=local|test|staging|production   perfil Spring (PERFIL_SPRING). Defecto: local, el de TEST hoy.
#   D3_OMITIR=INVITACIONES_CLAVE,QR_INTERNO_CLAVE   deja esas variables en blanco, como Coolify con un ${X} sin valor.
#   D3_ESCENARIO=limpia|actualizacion         limpia: base nueva. actualizacion: primero el sql/ de D3_COMMIT_ANTERIOR
#                                             (con datos de dev de esa version) y luego el actual.
#   D3_COMMIT_ANTERIOR=59370bbd               la version que hoy corre en TEST.
#   D3_ROLES=todos|catorce                    credencial de los roles svc_*: `todos` (el trabajo de esquema con ROLES_CLAVE,
#                                             como declara el compose) o `catorce` (estado PREVIO al arreglo: svc_inversiones
#                                             queda NOLOGIN y sin clave, que es lo que deja un roles-test.sql de catorce roles).
#   D3_MODO=completo|tandas|conjunto|individual               completo: los 15 + gateway a la vez, en las olas del compose. individual: cada
#                                             servicio solo (--no-deps), uno tras otro, para diagnosticar con poca RAM.
#   D3_SEMBRAR=1                              en modo conjunto, siembra usuarios sinteticos (credenciales fuera del repo).
#   D3_TANDA=4                                servicios por tanda en modo tandas.
#   D3_SOLO="a b c"                           en modo individual, solo esos servicios.
#   D3_MANTENER=1                             no bajar al terminar (consume RAM: lo usa el agente E).
#   D3_ETIQUETA=nombre                        sufijo de los archivos de evidencia.
#   D3_TOPE_S=900                             segundos maximos de espera de readiness.
#   D3_XMX=144m D3_MEM=320m                   heap y limite de cada JVM.
#   D3_IMAGEN_MINIO=cgr.dev/chainguard/minio:latest   en macOS (quay.io 401, sin MinIO en Docker Hub).
#   COMPOSE_PROFILES=observabilidad           suma el Collector OTel y Prometheus a la infra (tableros, D4); con
#                                             METRICAS_OTLP_HABILITADAS=true y PROMETHEUS_URL=http://prometheus:9090
#                                             exportadas, los servicios empujan OTLP y auditoria lee Prometheus.
HEAVY=C:/Users/Usuario/.claude/ram-guard/heavy.sh
if [ -z "${D3_BAJO_HEAVY:-}" ] && [ -f "$HEAVY" ]; then
  export D3_BAJO_HEAVY=1
  exec bash "$HEAVY" bash "$0" "$@"
fi
source "$(dirname "${BASH_SOURCE[0]}")/arranque/lib.sh"
cd "$RAIZ" || exit 2

PERFIL="${D3_PERFIL:-local}"; ESCENARIO="${D3_ESCENARIO:-limpia}"; ROLES="${D3_ROLES:-todos}"; MODO="${D3_MODO:-completo}"
COMMIT_ANT="${D3_COMMIT_ANTERIOR:-59370bbd}"; TOPE="${D3_TOPE_S:-900}"
ETQ="${D3_ETIQUETA:-${PERFIL}-${ESCENARIO}-${MODO}}"
SALIDA="$EVID/arranque-$ETQ.txt"
: > "$SALIDA"
exec > >(tee -a "$SALIDA") 2>&1

generar_entorno
entorno_de_escenario "PERFIL_SPRING=$PERFIL" ${D3_OMITIR:+"OMITIR=$D3_OMITIR"}
log "escenario=$ESCENARIO perfil=$PERFIL roles=$ROLES modo=$MODO omitir=[${D3_OMITIR:-}] xmx=${D3_XMX:-144m} mem=${D3_MEM:-320m}"
log "libres Windows al empezar: $(libres_windows_mb) MB"

# --- 0. el override tiene que cubrir TODOS los servicios del compose desplegado ----------------------
cubiertos="$(awk '/^services:/{d=1;next} d && /^  [a-z-]+:$/{print}' despliegue/ensayos/arranque/servicios.override.yml | tr -d ' :' | sort)"
reales="$(app config --services 2>/dev/null | sort)"
if [ "$cubiertos" != "$reales" ]; then
  log "ERROR: servicios.override.yml no coincide con docker-compose.coolify.yml"; diff <(echo "$cubiertos") <(echo "$reales"); exit 10
fi
for s in esquema gateway "${SERVICIOS_NEGOCIO[@]}"; do
  docker image inspect "aportaya/$s:test" >/dev/null 2>&1 || { log "ERROR: falta la imagen aportaya/$s:test (bash despliegue/ensayos/arranque/construir_imagenes.sh)"; exit 11; }
done

asegurar_docker || exit 3
RC=0
terminar() {
  detener_muestreo_ram
  log "RAM pico de los contenedores (hora,n,MB): $(ram_pico_mb)"
  if [ -z "${D3_MANTENER:-}" ]; then
    bash "$RAIZ/despliegue/ensayos/bajar_entorno.sh" quiet
    if [ "${INICIE_DOCKER:-0}" = 1 ]; then docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; fi
  else
    log "D3_MANTENER=1: el stack queda arriba (consume RAM). Bajar con: bash despliegue/ensayos/bajar_entorno.sh"
  fi
  log "libres Windows al terminar: $(libres_windows_mb) MB"
}
trap terminar EXIT

bash "$RAIZ/despliegue/ensayos/bajar_entorno.sh" quiet
iniciar_muestreo_ram

# --- 1. infraestructura ----------------------------------------------------------------------------
log "infra: postgres, pgbouncer, redis, minio, kafka"
infra up -d --wait 2>&1 | tail -3
if [ "$(docker inspect -f '{{.State.Health.Status}}' "$PROYECTO_INFRA-postgres-1" 2>/dev/null)" != healthy ]; then
  log "ERROR: la infraestructura no levanto"; RC=20; exit $RC
fi

# Carril G: la lista de topicos sale del codigo (scripts/listar_topicos.py). El broker del ensayo, como el de TEST,
# arranca con KAFKA_AUTO_CREATE_TOPICS_ENABLE=false: sin esto el relevo del outbox no puede publicar NINGUN evento.
if [ -z "${D3_SIN_TOPICOS:-}" ]; then
  log "kafka: creando los topicos aportaya.* (auto-create apagado, como TEST)"
  bash "$RAIZ/despliegue/host/crear-topicos.sh" "$PROYECTO_INFRA-kafka-1" 2>&1 | tail -3
fi

# Las variables del entorno del ensayo (cuenta de servicio de entregas, etc.) pasan tal cual a `docker compose`: el compose
# desplegado las declara sin valor (`ENTREGAS_SERVICIO_TELEFONO:`) y las toma del entorno del proceso.
set -a; source "$ENVF"; set +a

PSQL_ADMIN() { docker exec -i -e PGPASSWORD="$(grep ^BD_CLAVE_ADMIN= "$ENVF" | cut -d= -f2)" "$PROYECTO_INFRA-postgres-1" psql -v ON_ERROR_STOP=1 -q -U pasanaku -d pasanaku "$@"; }
CLAVE_BD="$(grep ^BD_CLAVE= "$ENVF" | cut -d= -f2)"
CONTEOS="select tablename||'='||(xpath('/row/c/text()', query_to_xml('select count(*) c from '||quote_ident(schemaname)||'.'||quote_ident(tablename), false, true, '')))[1]::text from pg_tables where schemaname not in ('pg_catalog','information_schema') order by 1"

# --- 2. (actualizacion) la version anterior, con datos ---------------------------------------------------
if [ "$ESCENARIO" = actualizacion ]; then
  log "version anterior $COMMIT_ANT: esquema + semillas + datos de dev + credencial de los roles"
  ANT="$D3_DIR/anterior"; rm -rf "$ANT"; mkdir -p "$ANT"
  git archive "$COMMIT_ANT" sql | tar -x -C "$ANT" || { log "ERROR: git archive $COMMIT_ANT"; RC=21; exit $RC; }
  docker cp "$ANT/sql" "$PROYECTO_INFRA-postgres-1:/tmp/sql-anterior" >/dev/null
  PGPW="$(grep ^BD_CLAVE_ADMIN= "$ENVF" | cut -d= -f2)"
  docker exec -e PGPASSWORD="$PGPW" "$PROYECTO_INFRA-postgres-1" sh -c 'cd /tmp/sql-anterior && psql -v ON_ERROR_STOP=1 -q -U pasanaku -d pasanaku -f aplicar.sql >/dev/null && psql -v ON_ERROR_STOP=1 -q -U pasanaku -d pasanaku -f 60_semillas/sembrar.sql >/dev/null' 2>&1 | tail -3
  PSQL_ADMIN -c "ALTER DATABASE pasanaku SET app.entorno = 'dev'"
  docker exec -e PGPASSWORD="$PGPW" "$PROYECTO_INFRA-postgres-1" sh -c 'cd /tmp/sql-anterior && psql -v ON_ERROR_STOP=1 -q -U pasanaku -d pasanaku -f 61_dev/sembrar_dev.sql >/dev/null' 2>&1 | tail -3
  PSQL_ADMIN -c "ALTER DATABASE pasanaku RESET app.entorno"
  # Como TEST: los roles que existian ya tienen credencial.
  for r in $(PSQL_ADMIN -tA -c "select rolname from pg_roles where rolname like 'svc\_%' order by 1"); do
    PSQL_ADMIN -c "ALTER ROLE $r LOGIN PASSWORD '$CLAVE_BD'"; done
  PSQL_ADMIN -tA -c "$CONTEOS" > "$D3_DIR/conteos-antes.txt"
  log "version anterior: $(wc -l < "$D3_DIR/conteos-antes.txt") tablas; usuarios=$(PSQL_ADMIN -tA -c 'select count(*) from identidad.usuario') movimientos=$(PSQL_ADMIN -tA -c 'select count(*) from nucleo_financiero.transaccion_billetera')"
fi

# --- 3. el trabajo de esquema, tal como lo corre Coolify -------------------------------------------------
log "esquema: docker compose up esquema (service_completed_successfully en TEST)"
app up --no-deps esquema 2>&1 | enmascarar | tail -12
ESQ_RC="$(docker inspect -f '{{.State.ExitCode}}' "$(contenedor esquema)" 2>/dev/null)"
log "esquema: exit code = $ESQ_RC"
if [ "$ESQ_RC" != 0 ]; then app logs esquema 2>&1 | enmascarar | tail -30; RC=30; exit $RC; fi
if [ "$ROLES" = catorce ]; then
  log "roles: estado PREVIO al arreglo -- svc_inversiones sin login ni clave (roles-test.sql de catorce roles)"
  PSQL_ADMIN -c "ALTER ROLE svc_inversiones NOLOGIN PASSWORD NULL"
fi
PSQL_ADMIN -tA -c "select rolname||' login='||rolcanlogin||' clave='||(rolpassword is not null) from pg_roles where rolname like 'svc\_%' order by 1" 2>/dev/null | sed 's/^/  rol /' > "$D3_DIR/roles.txt"
echo "roles svc_* sin login: $(grep -c 'login=false' "$D3_DIR/roles.txt")"; grep 'login=false' "$D3_DIR/roles.txt"
if [ "$ESCENARIO" = actualizacion ]; then
  PSQL_ADMIN -tA -c "$CONTEOS" > "$D3_DIR/conteos-despues.txt"
  perdidas="$(join -t= -j1 <(sort "$D3_DIR/conteos-antes.txt") <(sort "$D3_DIR/conteos-despues.txt") | awk -F= '$3<$2' | wc -l)"
  log "actualizacion: tablas que PERDIERON filas tras aplicar el esquema nuevo: $perdidas"
  bash "$RAIZ/despliegue/ensayos/arranque/comparar_esquemas.sh" | tee "$EVID/esquema-deriva-$ETQ.txt" | tail -40
  log "esquema: SEGUNDA corrida (idempotencia: no debe agregar columnas ni cambiar restricciones)"
  app up --no-deps esquema 2>&1 | grep -E "esquema:   \+|expandidas|actualizadas|exited with|ERROR|DETAIL|HINT|esquema: listo" | sed 's/^esquema-1  | //'
fi

# --- 4. los servicios -----------------------------------------------------------------------------------
if [ "$MODO" = tandas ]; then
  # Los quince juntos no entran en los 4 GB de Docker Desktop de esta maquina (cada JVM ~240 MB
  # con -Xmx144m; la infraestructura ya son ~1,7 GB). Se levantan en tandas de D3_TANDA servicios
  # (identidad siempre en la primera), con el gateway, se prueba el humo de lo que esta arriba y
  # se bajan antes de la siguiente. Cada servicio lleva sus `depends_on` del compose: se levanta
  # con --no-deps porque la ola anterior ya no esta; el esquema ya corrio.
  TAM="${D3_TANDA:-4}"; RESTO=("${SERVICIOS_NEGOCIO[@]}"); TOT_FALLAS=0
  app up -d --no-deps gateway 2>&1 | grep -iE "error|denied|no such" | head -3
  while [ "${#RESTO[@]}" -gt 0 ]; do
    LOTE=("${RESTO[@]:0:$TAM}"); RESTO=("${RESTO[@]:$TAM}")
    log "== tanda: ${LOTE[*]}"
    for s in "${LOTE[@]}"; do app up -d --no-deps "$s" 2>&1 | grep -iE "error|denied|no such" | head -3; done
    esperar_sanos "$TOPE" "${LOTE[@]}" gateway
    for s in gateway "${LOTE[@]}"; do
      est="$(estado_salud "$s")"; echo "RESULTADO $s: $est | readiness: $(readiness_directo "$s" | head -c 160)"
      if [ "$est" != healthy ]; then app logs --no-color --tail 80 "$s" 2>&1 | enmascarar > "$EVID/log-$ETQ-$s.txt"; echo "  (log -> evidencia/carril-D3/log-$ETQ-$s.txt)"; TOT_FALLAS=$((TOT_FALLAS+1)); fi
    done
    docker stats --no-stream --format 'RAM {{.Name}} {{.MemUsage}}' 2>&1 | grep aportaya-d3 | grep -v infra | sort | tee -a "$EVID/ram-por-contenedor-$ETQ.txt"
    D3_SERVICIOS_ARRIBA="${LOTE[*]}" bash "$RAIZ/despliegue/ensayos/arranque/humo.sh" | tee -a "$EVID/humo-$ETQ.txt"
    [ "${PIPESTATUS[0]}" = 0 ] || TOT_FALLAS=$((TOT_FALLAS+1))
    for s in "${LOTE[@]}"; do app rm -sf "$s" >/dev/null 2>&1; done
  done
  [ "$TOT_FALLAS" -eq 0 ] || RC=40
elif [ "$MODO" = conjunto ]; then
  # Un subconjunto que QUEDA ARRIBA (con D3_MANTENER=1) para pruebas de aceptacion: D3_SOLO lista los
  # servicios (identidad y el gateway siempre). Con D3_SEMBRAR=1 siembra los usuarios sinteticos
  # (admin, backoffice, dos participantes) y deja sus credenciales fuera del repositorio.
  LISTA=(identidad ${D3_SOLO:-grupos nucleo-financiero cumplimiento})
  LISTA=($(printf '%s
' "${LISTA[@]}" | awk '!v[$0]++'))
  app up -d --no-deps gateway 2>&1 | grep -iE "error|denied|no such" | head -3
  for s in "${LISTA[@]}"; do app up -d --no-deps "$s" 2>&1 | grep -iE "error|denied|no such" | head -3; done
  esperar_sanos "$TOPE" gateway "${LISTA[@]}"
  for s in gateway "${LISTA[@]}"; do
    est="$(estado_salud "$s")"; echo "RESULTADO $s: $est | readiness: $(readiness_directo "$s" | head -c 160)"
    if [ "$est" != healthy ]; then app logs --no-color --tail 80 "$s" 2>&1 | enmascarar > "$EVID/log-$ETQ-$s.txt"; RC=40; fi
  done
  [ -n "${D3_SEMBRAR:-}" ] && bash "$RAIZ/despliegue/ensayos/arranque/sembrar_usuarios.sh"
  D3_SERVICIOS_ARRIBA="${LISTA[*]}" bash "$RAIZ/despliegue/ensayos/arranque/humo.sh" | tee "$EVID/humo-$ETQ.txt"
  [ "${PIPESTATUS[0]}" = 0 ] || RC=40
  log "gateway: http://127.0.0.1:${D3_PUERTO_GATEWAY}  postgres: 127.0.0.1:${D3_PUERTO_PG}  pgbouncer: 127.0.0.1:${D3_PUERTO_PGBOUNCER}"
elif [ "$MODO" = individual ]; then
  LISTA=(${D3_SOLO:-gateway "${SERVICIOS_NEGOCIO[@]}"})
  for s in "${LISTA[@]}"; do
    log "== $s solo (--no-deps)"
    app up -d --no-deps "$s" 2>&1 | grep -iE "error|denied|no such|not found|invalid" | head -3
    esperar_sanos "$TOPE" "$s"; est="$(estado_salud "$s")"
    echo "RESULTADO $s: $est | readiness: $(readiness_directo "$s" | head -c 200)"
    if [ "$est" != healthy ]; then app logs --no-color --tail 80 "$s" 2>&1 | enmascarar > "$EVID/log-$ETQ-$s.txt"; echo "  (log -> evidencia/carril-D3/log-$ETQ-$s.txt)"; fi
    docker stats --no-stream --format '  RAM {{.Name}} {{.MemUsage}}' "$(contenedor "$s")" 2>/dev/null
    app rm -sf "$s" >/dev/null 2>&1
  done
else
  log "app up -d (olas del compose: identidad primero, de a cuatro, cada una espera a la anterior sana)"
  ( app up -d gateway "${SERVICIOS_NEGOCIO[@]}" 2>&1 | enmascarar | tail -5 ) &
  UPPID=$!
  esperar_sanos "$TOPE" gateway "${SERVICIOS_NEGOCIO[@]}"
  for s in gateway "${SERVICIOS_NEGOCIO[@]}"; do
    est="$(estado_salud "$s")"; echo "RESULTADO $s: $est | readiness: $(readiness_directo "$s" | head -c 160)"
    if [ "$est" != healthy ]; then app logs --no-color --tail 80 "$s" 2>&1 | enmascarar > "$EVID/log-$ETQ-$s.txt"; echo "  (log -> evidencia/carril-D3/log-$ETQ-$s.txt)"; fi
  done
  docker stats --no-stream --format 'RAM {{.Name}} {{.MemUsage}}' 2>&1 | grep aportaya-d3 | sort | tee "$EVID/ram-por-contenedor-$ETQ.txt"
  kill $UPPID 2>/dev/null
  # --- 5. humo ---
  bash "$RAIZ/despliegue/ensayos/arranque/humo.sh" | tee "$EVID/humo-$ETQ.txt"
  [ "${PIPESTATUS[0]}" = 0 ] || RC=40
fi
exit $RC
