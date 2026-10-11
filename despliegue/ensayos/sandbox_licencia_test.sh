#!/usr/bin/env bash
# Habilita en TEST el servicio GRUPO_PASANAKU como SANDBOX SINTETICO de pruebas, para que la cuenta A de prueba
# (despliegue/ensayos/sembrar_test.sh) pueda crear grupos. Sin esto, crear un grupo en TEST responde
# 422 AP-CU20-04 «El servicio no esta habilitado en este momento» (R-LIC-01, CU-46): la licencia esta EN_TRAMITE.
#
# QUE HACE (y solo eso)
#   · Aplica seeders/dev/18-licencia-sandbox-test.json (el SQL de abajo lo genera scripts/generar_semillas.py;
#     no se edita): UNA fila de cumplimiento.entorno_prueba_regulado, ACTIVA, solo para GRUPO_PASANAKU.
#   · Es la SEGUNDA puerta de R-LIC-01 (licencia OTORGADA que cubra el servicio, O entorno de prueba activo).
#     NO toca la licencia: sigue ASFI / CERTIFICADO_ADECUACION / EN_TRAMITE, sin numero de resolucion. No se
#     inventa ningun numero de licencia ni ninguna autoridad. La fila queda marcada «sandbox de TEST: no es una
#     licencia ASFI» (campo alcance) y es sintetica (apto_produccion=false en el seeder).
#   · No habilita ningun otro servicio: BILLETERA, RECARGA, RETIRO y TRANSFERENCIA_P2P siguen denegados.
#   · No toca otra tabla y no imprime secretos (no maneja ninguno).
#
# COMO SE CORRE (desde tu maquina)
#   ssh root@161.97.85.216 'bash -s'               < despliegue/ensayos/sandbox_licencia_test.sh   # aplica (idempotente)
#   ssh root@161.97.85.216 'bash -s -- --estado'   < despliegue/ensayos/sandbox_licencia_test.sh   # solo lee
#   ssh root@161.97.85.216 'bash -s -- --retirar'  < despliegue/ensayos/sandbox_licencia_test.sh   # lo da de baja
#   (con `bash -s` el guion llega por la entrada estandar: todo el cuerpo esta dentro de main() y ningun comando
#    lee la entrada estandar, para que nada se coma lo que falta leer.)
#
# IDEMPOTENTE: id estable + ON CONFLICT (id) DO NOTHING; volver a correrlo no cambia nada («cambios: 0»).
# Si el sandbox se retiro (--retirar) o su ventana vencio, correrlo de nuevo lo REACTIVA (una ventana nueva de
# 90 dias). `--retirar` pasa la fila a FINALIZADO: el alcance vuelve a «habilitado:false, via NINGUNA».
#
# GUARDAS (abortan sin escribir nada)
#   1. Perfil: los contenedores identidad, cumplimiento y grupos deben correr con el perfil `local` (el de
#      desarrollo/TEST) y NINGUNO con un perfil de produccion. Donde no corre el perfil `local`, esto no se aplica.
#   2. Marca de entorno de la base: si `app.entorno` esta definida, no puede ser de produccion.
#   3. Licencia: si ya existe una licencia OTORGADA no se mezcla un sandbox con una licencia real.
#   4. Debe existir la licencia sembrada (EN_TRAMITE) de la que cuelga el entorno de prueba.
#   `--estado` y `--retirar` no aplican las guardas 1, 3 y 4: leer o apagar un sandbox nunca es peligroso.
#
# VARIABLES (todas opcionales, para otros hosts o ensayos locales)
#   PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   IDENTIDAD_CONTENEDOR=<nombre>  CUMPLIMIENTO_CONTENEDOR=<nombre>  GRUPOS_CONTENEDOR=<nombre>
#   OMITIR_GUARDA_DE_PERFIL=1   (SOLO para el ensayo local con un Postgres desechable sin contenedores de servicio)

ID_SANDBOX="e2e00000-0000-4000-8000-0000000000d1"
SERVICIO="GRUPO_PASANAKU"

uso() {
  echo "uso: ssh <host> 'bash -s'               < sandbox_licencia_test.sh   aplica el sandbox (idempotente)"
  echo "     ssh <host> 'bash -s -- --estado'   < sandbox_licencia_test.sh   solo lee"
  echo "     ssh <host> 'bash -s -- --retirar'  < sandbox_licencia_test.sh   lo da de baja (FINALIZADO)"
}

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=aplicar
  case "${1:-}" in
    "") ;;
    --retirar) modo=retirar ;;
    --estado) modo=estado ;;
    -h|--help) uso; return 0 ;;
    *) echo "argumento desconocido: $1" >&2; return 2 ;;
  esac

  local PG="${PG_CONTENEDOR:-aportaya-postgres}" ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}" BASE="${PG_BASE:-pasanaku}"

  # ---- la base: usuario y clave desde el .env del servidor, sin imprimirlos --------------------------
  valor_env() {   # KEY=valor, con o sin comillas; sin ejecutar nada del archivo
    [ -r "$ENV_PG" ] || return 0
    grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ENV_PG" | head -1 | sed -E "s/^[^=]*=//; s/^[\"']//; s/[\"']\$//"
  }
  docker inspect -f '{{.State.Running}}' "$PG" 2>/dev/null </dev/null | grep -q true || { echo "ERROR: no encuentro el contenedor de Postgres «$PG» en marcha" >&2; return 3; }
  local pgu pgp
  pgu="$(valor_env POSTGRES_USER)"; [ -n "$pgu" ] || pgu="$(valor_env BD_USUARIO_ADMIN)"
  [ -n "$pgu" ] || pgu="$(docker exec "$PG" printenv POSTGRES_USER 2>/dev/null </dev/null)"
  pgp="$(valor_env POSTGRES_PASSWORD)"; [ -n "$pgp" ] || pgp="$(valor_env BD_CLAVE_ADMIN)"
  [ -n "$pgu" ] || { echo "ERROR: no pude determinar el usuario administrador de la base" >&2; return 3; }
  # psql_pg lee de la entrada que le pasen (un heredoc o un pipe): nunca de la del script.
  psql_pg() { PGPASSWORD="$pgp" docker exec -i -e PGPASSWORD "$PG" psql -X -q -v ON_ERROR_STOP=1 -U "$pgu" -d "$BASE" "$@"; }
  psql_pg -tA -c 'SELECT 1' </dev/null >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; return 3; }

  # El SQL del sandbox (generado). Va antes de todo: de el sale tambien el search_path, que las consultas de abajo
  # necesitan porque fn_lic_servicio_habilitado() nombra sus tablas sin esquema.
# >>> SQL-GENERADO · no editar: lo escribe scripts/generar_semillas.py desde seeders/dev/18-licencia-sandbox-test.json
# search_path como el de los orquestadores: este guion corre en su propia sesion de psql.
IFS= read -r -d '' SEMILLA_SQL <<'SQL_SEMILLA_SANDBOX' || true
SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;
-- Entorno de prueba SINTETICO (sandbox) que habilita solo GRUPO_PASANAKU en DEV y TEST, para poder crear grupos de prueba. NO es una licencia: la licencia sigue EN_TRAMITE.
-- GENERADO desde seeders/dev/18-licencia-sandbox-test.json — no editar a mano.

-- SINTETICO · apto_produccion=false · «sandbox de TEST: no es una licencia ASFI». Cuelga de la licencia sembrada EN_TRAMITE (08-gobierno-y-licencia.json), que no se modifica. Ventana: desde ayer hasta 90 dias; pasada la ventana el servicio vuelve a denegarse solo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO entorno_prueba_regulado (id, licencia_regulatoria_id, servicio_en_prueba, alcance, limite_usuarios, limite_monto_operacion, garantia_constituida, fecha_inicio, fecha_fin, estado, informes_remitidos) VALUES
      ('e2e00000-0000-4000-8000-0000000000d1', (SELECT id FROM licencia_regulatoria WHERE organismo = 'ASFI' AND tipo = 'CERTIFICADO_ADECUACION'), 'GRUPO_PASANAKU', '["SANDBOX-TEST", "sandbox de TEST: no es una licencia ASFI", "sintetico: apto_produccion=false"]'::jsonb, 50, 50000.0, NULL, current_date - 1, (current_date + interval '+90 days'), 'ACTIVO', 0)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;
SQL_SEMILLA_SANDBOX
# <<< SQL-GENERADO
  local SP; SP="$(printf '%s\n' "$SEMILLA_SQL" | grep -m1 '^SET search_path')"
  [ -n "$SP" ] || { echo "ERROR: el SQL generado no trae el search_path; correr scripts/generar_semillas.py" >&2; return 2; }

  conteos() {   # una linea con lo que este guion maneja; sirve para probar «cambios: 0»
    psql_pg -tA -F ' ' <<SQL
$SP
SELECT 'sandbox_filas=' || count(*) FROM cumplimiento.entorno_prueba_regulado WHERE id = '$ID_SANDBOX'
UNION ALL SELECT 'sandbox_activo_vigente=' || count(*) FROM cumplimiento.entorno_prueba_regulado
   WHERE id = '$ID_SANDBOX' AND estado = 'ACTIVO' AND current_date BETWEEN fecha_inicio AND fecha_fin
UNION ALL SELECT 'otros_entornos_activos=' || count(*) FROM cumplimiento.entorno_prueba_regulado WHERE id <> '$ID_SANDBOX' AND estado = 'ACTIVO'
UNION ALL SELECT 'licencias_otorgadas=' || count(*) FROM catalogo.licencia_regulatoria WHERE estado = 'OTORGADA'
UNION ALL SELECT 'licencias_en_tramite=' || count(*) FROM catalogo.licencia_regulatoria WHERE estado = 'EN_TRAMITE'
UNION ALL SELECT 'fn_lic_$SERVICIO=' || fn_lic_servicio_habilitado('$SERVICIO')
UNION ALL SELECT 'fn_lic_BILLETERA=' || fn_lic_servicio_habilitado('BILLETERA');
SQL
  }
  linea_de_conteos() { conteos | tr '\n' ' '; }

  if [ "$modo" = estado ]; then
    echo "[estado] $(linea_de_conteos)"
    return 0
  fi

  if [ "$modo" = retirar ]; then
    local antes; antes="$(linea_de_conteos)"
    psql_pg <<SQL || { echo "ERROR: el retiro fallo; no quedo nada a medias (una sola transaccion)" >&2; return 4; }
BEGIN;
UPDATE cumplimiento.entorno_prueba_regulado SET estado = 'FINALIZADO' WHERE id = '$ID_SANDBOX' AND estado <> 'FINALIZADO';
COMMIT;
SQL
    echo "[retirar] antes:   $antes"
    echo "[retirar] despues: $(linea_de_conteos)"
    echo "[retirar] listo. La licencia no se toco (sigue EN_TRAMITE); correr el guion de nuevo reactiva el sandbox."
    return 0
  fi

  # ---- aplicar --------------------------------------------------------------------------------------
  # Guarda 1: el perfil. Estos sandbox solo existen donde corre el perfil `local` (desarrollo/TEST).
  if [ "${OMITIR_GUARDA_DE_PERFIL:-}" != 1 ]; then
    local nombre var patron c perfil candidatos
    for nombre in identidad cumplimiento grupos; do
      var="$(printf '%s' "$nombre" | tr '[:lower:]' '[:upper:]')_CONTENEDOR"
      c="${!var:-}"
      if [ -z "$c" ]; then
        patron="^${nombre}-"
        candidatos="$(docker ps --format '{{.Names}}' </dev/null | grep -E "$patron" || true)"
        [ "$(printf '%s\n' "$candidatos" | grep -c .)" = 1 ] \
          || { echo "ERROR: esperaba UN contenedor «${nombre}-…» en marcha y hay: $(printf '%s' "$candidatos" | tr '\n' ' ')" >&2; return 3; }
        c="$candidatos"
      fi
      perfil="$(docker exec "$c" printenv SPRING_PROFILES_ACTIVE 2>/dev/null </dev/null)"
      case ",$perfil," in
        *,prod,*|*,production,*|*,produccion,*) echo "ERROR: $nombre corre con el perfil «$perfil», de produccion: aca no se habilita un sandbox" >&2; return 5 ;;
      esac
      case ",$perfil," in
        *,local,*) ;;
        *) echo "ERROR: $nombre corre con el perfil «${perfil:-<vacio>}», no con «local»: aca no se habilita un sandbox" >&2; return 5 ;;
      esac
    done
  fi
  # Guarda 2: la marca de entorno de la base (si alguien la puso, no puede ser de produccion).
  local marca; marca="$(psql_pg -tA -c "SELECT coalesce(current_setting('app.entorno', true), '')" </dev/null 2>/dev/null)"
  case "$marca" in
    prod|production|produccion) echo "ERROR: la base esta marcada app.entorno=«$marca»: aca no se habilita un sandbox" >&2; return 5 ;;
  esac
  # Guardas 3 y 4: la licencia.
  local otorgadas en_tramite
  otorgadas="$(psql_pg -tA -c "SELECT count(*) FROM catalogo.licencia_regulatoria WHERE estado = 'OTORGADA'" </dev/null)"
  [ "$otorgadas" = 0 ] || { echo "ERROR: ya hay $otorgadas licencia(s) OTORGADA(s): no se mezcla un sandbox sintetico con una licencia real" >&2; return 5; }
  en_tramite="$(psql_pg -tA -c "SELECT count(*) FROM catalogo.licencia_regulatoria WHERE organismo = 'ASFI' AND tipo = 'CERTIFICADO_ADECUACION'" </dev/null)"
  [ "$en_tramite" = 1 ] || { echo "ERROR: esperaba UNA licencia ASFI/CERTIFICADO_ADECUACION sembrada y hay $en_tramite" >&2; return 5; }

  local antes; antes="$(linea_de_conteos)"
  echo "[sandbox] antes:   $antes"

  # 1) aplicar el sandbox
  local salida_sql rc_sql
  salida_sql="$({ echo 'BEGIN;'; printf '%s\n' "$SEMILLA_SQL"; echo 'COMMIT;'; } | psql_pg 2>&1)"; rc_sql=$?
  printf '%s\n' "$salida_sql" | grep -v -e '^NOTICE' -e '^$' | sed -E 's/^/[sandbox] /' || true
  [ "$rc_sql" = 0 ] || { echo "ERROR: la siembra fallo (una sola transaccion: no quedo nada a medias)" >&2; return 4; }

  # 1b) si una corrida anterior lo retiro o su ventana vencio, esta lo reactiva (solo toca esa fila)
  psql_pg <<SQL || { echo "ERROR: no pude reactivar el sandbox" >&2; return 4; }
BEGIN;
UPDATE cumplimiento.entorno_prueba_regulado
   SET estado = 'ACTIVO', fecha_inicio = current_date - 1, fecha_fin = current_date + 90
 WHERE id = '$ID_SANDBOX' AND (estado <> 'ACTIVO' OR fecha_fin < current_date);
COMMIT;
SQL

  local despues; despues="$(linea_de_conteos)"
  echo "[sandbox] despues: $despues"
  if [ "$antes" = "$despues" ]; then echo "[sandbox] cambios: 0 (idempotente)"; else echo "[sandbox] cambios: si (antes != despues)"; fi
  echo "[listo] sandbox SINTETICO de $SERVICIO activo (no es una licencia ASFI; la licencia sigue EN_TRAMITE). Retirar: --retirar."
}

main "$@"
