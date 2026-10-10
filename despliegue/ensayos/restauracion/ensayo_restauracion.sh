#!/usr/bin/env bash
# Ensayo REAL de restauracion y reanudacion (H13.S1.M4). PostgreSQL desechable en Docker.
#
#   bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash despliegue/ensayos/restauracion/ensayo_restauracion.sh
#
# Nunca toca una base viva: levanta su propio contenedor en 127.0.0.1:${PUERTO} (5439 por
# omision), lo destruye a proposito, levanta otro VACIO, restaura el respaldo, concilia el
# mayor y recien entonces reanuda los eventos. Mide cada fase. Todo dato es sintetico.
#
# Sale con 0 solo si: (a) la conciliacion dio verde sobre el punto de recuperacion, (b) las
# diferencias con el origen se explican por lo posterior al respaldo, (c) al reponer eso
# desde la fuente externa el mayor queda identico al del origen, y (d) las tres incidencias
# inyectadas (search_path perdido, saldo adulterado, secuencia retrocedida) CIERRAN la
# compuerta. Una compuerta que nunca se probo cerrada no es una compuerta.
set -u

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$AQUI/../../.." && pwd)"
PUERTO="${PUERTO:-5439}"
CT="${CONTENEDOR:-aportaya-pg-s2}"
IMAGEN="${IMAGEN:-postgres:16}"
TMP="${TMP:-${TEMP:-/tmp}/ensayo-restauracion-s2}"
mkdir -p "$TMP"
FALLAS=0
INICIE_DOCKER=0

ms() { echo $(( $(date +%s%N) / 1000000 )); }
dur() { echo "$(( ($2 - $1) ))"; }
paso() { echo; echo "== $*"; }
falla() { echo "FALLA: $*"; FALLAS=$((FALLAS + 1)); }
psql_en() { MSYS_NO_PATHCONV=1 docker exec -i "$CT" psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku "$@"; }

esperar_base() {
  for _ in $(seq 1 90); do
    docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && sleep 3 \
      && docker exec "$CT" pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && return 0
    sleep 3
  done
  return 1
}

levantar() {
  docker rm -f -v "$CT" >/dev/null 2>&1
  MSYS_NO_PATHCONV=1 docker run -d --name "$CT" \
    -e POSTGRES_DB=pasanaku -e POSTGRES_USER=pasanaku -e POSTGRES_PASSWORD=pasanaku \
    -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" -p "127.0.0.1:${PUERTO}:5432" \
    -v "$REPO/sql:/repo/sql:ro" -v "$REPO/despliegue/compose/init:/docker-entrypoint-initdb.d:ro" \
    "$IMAGEN" -c max_connections=100 -c shared_buffers=128MB -c timezone=America/La_Paz >/dev/null \
    || { echo "no pude crear $CT"; return 1; }
  esperar_base
}

limpiar() {
  docker rm -f -v "$CT" >/dev/null 2>&1
  if [ "$INICIE_DOCKER" = 1 ]; then
    docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1
  fi
}
trap limpiar EXIT

# El manifiesto (clave=valor) se vuelve las -v de la conciliacion.
args_de() {
  local f="$1" a=""
  while IFS='=' read -r k v; do
    case "$k" in
      asientos) a="$a -v m_asientos=$v" ;;        suma_debe) a="$a -v m_suma_debe=$v" ;;
      huella_mayor) a="$a -v m_huella=$v" ;;      max_numero) a="$a -v m_max_numero=$v" ;;
      eventos) a="$a -v m_eventos=$v" ;;          ev_pendientes) a="$a -v m_pendientes=$v" ;;
      ev_tomados) a="$a -v m_tomados=$v" ;;       ev_publicados) a="$a -v m_publicados=$v" ;;
      consumidos_erp) a="$a -v m_consumidos=$v" ;; tablas_con_rls) a="$a -v m_rls=$v" ;;
      tablas) a="$a -v m_tablas=$v" ;;
    esac
  done < "$f"
  echo "$a"
}

# conciliar <manifiesto> <etiqueta> -> imprime las filas. Devuelve 0 si todo es `t`, 1 si alguna
# comprobacion dio `f` (la compuerta CIERRA) y 2 si la consulta no devolvio las 17 filas (error
# del ensayo, no una compuerta cerrada: no se confunden).
conciliar() {
  local m="$1" etiqueta="$2" t0 t1 salida
  t0=$(ms)
  # shellcheck disable=SC2046
  salida=$(psql_en -tA -F'|' $(args_de "$m") < "$AQUI/conciliacion_del_mayor.sql" 2>&1)
  t1=$(ms)
  echo "$salida" | sed 's/^/   /'
  echo "   [$etiqueta] conciliacion: $(dur "$t0" "$t1") ms"
  [ "$(echo "$salida" | grep -c '^C[0-9][0-9] ')" -ge 17 ] || return 2
  echo "$salida" | grep -q '^C[0-9][0-9] .*|f|' && return 1
  return 0
}

echo "ENSAYO DE RESTAURACION · $(date -u +%Y-%m-%dT%H:%M:%SZ) · contenedor $CT · puerto $PUERTO · imagen $IMAGEN"

# ---------------------------------------------------------------- Docker y origen
if ! docker info >/dev/null 2>&1; then
  INICIE_DOCKER=1
  paso "levantando Docker Desktop"
  ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
  for _ in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
  docker info >/dev/null 2>&1 || { echo "Docker no levanto"; exit 3; }
fi
echo "libres: $(powershell.exe -NoProfile -Command '[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)' | tr -d '\r') MB"

paso "F0 origen: PostgreSQL desechable con el esquema completo"
T_INICIO=$(ms)
levantar || { echo "el origen no arranco"; exit 4; }
t0=$(ms)
psql_en -q -f /repo/sql/aplicar.sql >"$TMP/aplicar.log" 2>&1 || { tail -5 "$TMP/aplicar.log"; exit 5; }
# Siembra sin ON_ERROR_STOP: los catalogos son de otro carril y pueden estar a mitad de cambio; los errores se informan.
psql_en -v ON_ERROR_STOP=0 -q -f /repo/sql/60_semillas/sembrar.sql >"$TMP/semillas.log" 2>&1
echo "   siembra de catalogos: $(grep -c 'ERROR' "$TMP/semillas.log") error(es)"; grep 'ERROR' "$TMP/semillas.log" | head -3 | sed 's/^/   /'
echo "   esquema + catalogos: $(dur "$t0" "$(ms)") ms · tablas: $(psql_en -tA -c "select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')")"

paso "F1 datos sinteticos del lote base (400 asientos; cola viva: 8 pendientes y 2 tomados por un relevo muerto)"
psql_en -q -v lote=base -v cuantos=400 < "$AQUI/datos_sinteticos.sql" || { falla "datos base"; exit 6; }
psql_en -tA < "$AQUI/manifiesto.sql" > "$TMP/manifiesto_respaldo.txt"
sed 's/^/   /' "$TMP/manifiesto_respaldo.txt"

paso "F2 PUNTO DE RESPALDO: pg_dump (custom) y roles"
T_RESPALDO_EPOCH=$(date +%s)
T_RESPALDO_TS=$(psql_en -tA -c "select clock_timestamp()")
t0=$(ms)
MSYS_NO_PATHCONV=1 docker exec "$CT" pg_dump -U pasanaku -d pasanaku -Fc -f /tmp/respaldo.dump
MSYS_NO_PATHCONV=1 docker exec "$CT" pg_dumpall -U pasanaku --globals-only -f /tmp/globales.sql
t1=$(ms)
MSYS_NO_PATHCONV=1 docker cp "$CT:/tmp/respaldo.dump" "$TMP/respaldo.dump"
MSYS_NO_PATHCONV=1 docker cp "$CT:/tmp/globales.sql" "$TMP/globales.sql"
MS_RESPALDO=$(dur "$t0" "$t1")
echo "   respaldo: ${MS_RESPALDO} ms · $(wc -c < "$TMP/respaldo.dump") bytes (dump) · punto de recuperacion: $T_RESPALDO_TS"

paso "F3 trabajo POSTERIOR al respaldo (7 asientos que el respaldo no tiene) y estado del origen en el momento del desastre"
sleep 2
psql_en -q -v lote=posterior -v cuantos=7 < "$AQUI/datos_sinteticos.sql" || falla "datos posteriores"
psql_en -tA < "$AQUI/manifiesto.sql" > "$TMP/manifiesto_origen_final.txt"
psql_en -tA -F'|' -c "select numero, glosa, fecha from nucleo_financiero.asiento_contable where glosa like 'S2 sintetico posterior%' order by numero" > "$TMP/perdidos_origen.txt"
T_DESASTRE_EPOCH=$(date +%s)
echo "   asientos del origen: $(grep '^asientos=' "$TMP/manifiesto_origen_final.txt") (el respaldo tiene $(grep '^asientos=' "$TMP/manifiesto_respaldo.txt"))"

paso "F4 DESASTRE: se destruye el contenedor y su volumen"
docker rm -f -v "$CT" >/dev/null 2>&1
echo "   contenedor y datos destruidos"

# ---------------------------------------------------------------- restauracion
paso "F5 RESTAURACION en una instancia VACIA (RTO desde aqui)"
T_RTO0=$(ms)
levantar || { echo "la instancia nueva no arranco"; exit 4; }
t0=$(ms)
MSYS_NO_PATHCONV=1 docker cp "$TMP/respaldo.dump" "$CT:/tmp/respaldo.dump"
MSYS_NO_PATHCONV=1 docker cp "$TMP/globales.sql" "$CT:/tmp/globales.sql"
# Sin ON_ERROR_STOP: pg_dumpall intenta CREATE ROLE pasanaku, que ya existe (es el superusuario de arranque),
# y con ON_ERROR_STOP el primer error abortaria el resto y ningun rol de servicio llegaria a crearse.
psql_en -v ON_ERROR_STOP=0 -q -f /tmp/globales.sql >"$TMP/globales.log" 2>&1
echo "   roles (globales): $(grep -c '^CREATE ROLE' "$TMP/globales.sql") CREATE ROLE en el dump; rechazados por ya existir (superusuario de arranque): $(grep -c 'already exists' "$TMP/globales.log")"
t1=$(ms)
MSYS_NO_PATHCONV=1 docker exec "$CT" pg_restore -U pasanaku -d pasanaku --no-owner --role=pasanaku /tmp/respaldo.dump >"$TMP/restore.log" 2>&1
RC_RESTORE=$?
t2=$(ms)
MS_RESTAURA=$(dur "$t1" "$t2")
echo "   pg_restore rc=$RC_RESTORE · $(grep -ci 'error' "$TMP/restore.log") lineas con 'error' · ${MS_RESTAURA} ms"
grep -i 'error' "$TMP/restore.log" | head -5 | sed 's/^/   /'
echo "   instancia + roles: $(dur "$T_RTO0" "$t1") ms"

paso "F6 CONCILIACION DEL MAYOR sobre el punto de recuperacion (compuerta de reapertura)"
if conciliar "$TMP/manifiesto_respaldo.txt" "punto de recuperacion"; then
  echo "   COMPUERTA: ABIERTA para este punto de recuperacion"
else
  falla "la conciliacion del punto de recuperacion no dio verde"
fi
T_CONCILIADO=$(ms)

paso "F7 DIFERENCIAS CON EL ORIGEN, explicadas"
ORIGEN_N=$(grep '^asientos=' "$TMP/manifiesto_origen_final.txt" | cut -d= -f2)
REST_N=$(psql_en -tA -c "select count(*) from nucleo_financiero.asiento_contable")
FALTAN=$((ORIGEN_N - REST_N))
LISTA_FALTAN=$(wc -l < "$TMP/perdidos_origen.txt")
PRESENTES=$(psql_en -tA -c "select count(*) from nucleo_financiero.asiento_contable where glosa like 'S2 sintetico posterior%'")
echo "   origen en el desastre: $ORIGEN_N asientos · restaurado: $REST_N · faltan: $FALTAN"
echo "   los $LISTA_FALTAN asientos con fecha posterior al respaldo (origen) y su presencia hoy: $PRESENTES"
sed 's/^/   perdido: /' "$TMP/perdidos_origen.txt" | cut -c1-110
RPO_S=$((T_DESASTRE_EPOCH - T_RESPALDO_EPOCH))
echo "   RPO observado (respaldo -> desastre): ${RPO_S} s"
if [ "$FALTAN" = "$LISTA_FALTAN" ] && [ "$PRESENTES" = "0" ]; then
  echo "   EXPLICADA: la diferencia son exactamente los $FALTAN asientos posteriores al respaldo; ningun otro asiento difiere"
else
  falla "la diferencia con el origen NO se explica solo por lo posterior al respaldo"
fi

paso "F8 REPOSICION desde la fuente externa (mismo lote, mismos ids) y conciliacion contra el origen"
t0=$(ms)
psql_en -q -v lote=posterior -v cuantos=7 < "$AQUI/datos_sinteticos.sql" || falla "reposicion"
if conciliar "$TMP/manifiesto_origen_final.txt" "mayor repuesto vs origen"; then
  echo "   IDENTICO al origen (huella del libro, sumas, numeros de asiento y cola del outbox)"
else
  falla "tras reponer, el mayor no coincide con el origen"
fi
echo "   reposicion + conciliacion: $(dur "$t0" "$(ms)") ms"

paso "F9 REANUDACION DE EVENTOS (solo con la compuerta abierta)"
t0=$(ms)
psql_en -q -tA -F'|' < "$AQUI/reanudacion_de_eventos.sql" | tee "$TMP/reanudacion.txt" | sed 's/^/   /'
grep -q '|f|' "$TMP/reanudacion.txt" && falla "la reanudacion de eventos no dio verde"
[ "$(grep -c '^R0' "$TMP/reanudacion.txt")" = "5" ] || falla "faltan comprobaciones de reanudacion"
echo "   reanudacion: $(dur "$t0" "$(ms)") ms"
T_REABRE=$(ms)
echo "   RTO (instancia vacia -> mayor conciliado y eventos corriendo): $(dur "$T_RTO0" "$T_REABRE") ms"

# ---------------------------------------------------------------- la compuerta cierra
paso "F10 INCIDENCIAS INYECTADAS: la compuerta tiene que CERRARSE"
cp "$TMP/manifiesto_origen_final.txt" "$TMP/m_actual.txt"
# El estado de la cola cambio con la reanudacion: se refresca la parte de la cola para aislar el defecto inyectado.
psql_en -tA < "$AQUI/manifiesto.sql" > "$TMP/m_actual.txt"

echo "-- I1: la base pierde su search_path (ALTER DATABASE no viaja en pg_dump)"
psql_en -q -c "ALTER DATABASE pasanaku RESET search_path"
conciliar "$TMP/m_actual.txt" "I1" >"$TMP/i1.txt" 2>&1; RC=$?; if [ "$RC" != 1 ]; then falla "I1: la compuerta debia cerrar (rc=1) y dio rc=$RC con search_path perdido"; else grep "|f|" "$TMP/i1.txt" | sed "s/^/   /"; echo "   I1 OK: compuerta CERRADA (C14)"; fi
psql_en -q -c "ALTER DATABASE pasanaku SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public"

echo "-- I2: un saldo almacenado deja de coincidir con el mayor"
psql_en -q -c "update nucleo_financiero.cuenta_contable set saldo = saldo + 1 where id = '5e000000-0000-4000-8000-000000000001'"
conciliar "$TMP/m_actual.txt" "I2" >"$TMP/i2.txt" 2>&1; RC=$?; if [ "$RC" != 1 ]; then falla "I2: la compuerta debia cerrar (rc=1) y dio rc=$RC con saldo adulterado"; else grep "|f|" "$TMP/i2.txt" | sed "s/^/   /"; echo "   I2 OK: compuerta CERRADA (C08)"; fi
psql_en -q -c "select nucleo_financiero.fn_ctb_recalcular_saldo('5e000000-0000-4000-8000-000000000001')" 2>/dev/null \
  || psql_en -q -tA -c "select fn_ctb_recalcular_saldo('5e000000-0000-4000-8000-000000000001')" >/dev/null
echo "   reparacion: saldo recalculado DESDE el mayor (no se edita a mano)"

echo "-- I3: la secuencia de asientos retrocede (el defecto clasico de un dump sin setval)"
psql_en -q -tA -c "select setval('nucleo_financiero.asiento_contable_numero_seq', 1)" >/dev/null
conciliar "$TMP/m_actual.txt" "I3" >"$TMP/i3.txt" 2>&1; RC=$?; if [ "$RC" != 1 ]; then falla "I3: la compuerta debia cerrar (rc=1) y dio rc=$RC con secuencia retrocedida"; else grep "|f|" "$TMP/i3.txt" | sed "s/^/   /"; echo "   I3 OK: compuerta CERRADA (C09)"; fi
psql_en -q -tA -c "select setval('nucleo_financiero.asiento_contable_numero_seq', (select max(numero) from nucleo_financiero.asiento_contable))" >/dev/null

echo "-- tras reparar las tres, la compuerta vuelve a abrir"
if conciliar "$TMP/m_actual.txt" "reparado"; then echo "   COMPUERTA: ABIERTA"; else falla "reparado el defecto, la compuerta sigue cerrada"; fi

paso "RESUMEN"
echo "respaldo (pg_dump + roles): ${MS_RESPALDO} ms"
echo "restauracion (pg_restore):  ${MS_RESTAURA} ms"
echo "RTO medido:                 $(dur "$T_RTO0" "$T_REABRE") ms (instancia vacia -> compuerta abierta -> eventos reanudados)"
echo "RPO observado:              ${RPO_S} s (asientos posteriores al respaldo: $FALTAN, repuestos desde la fuente)"
echo "duracion total del ensayo:  $(dur "$T_INICIO" "$(ms)") ms"
echo "fallas: $FALLAS"
[ "$FALLAS" = 0 ] && echo "ENSAYO: PASS" || echo "ENSAYO: FAIL"
exit $((FALLAS > 0))
