#!/usr/bin/env bash
# Activa en TEST el proveedor de pago QR_INTEROP (SOLO TEST/dev), para que el aviso simulado de la pasarela
# SIMULADA de aportes (POST /qr/simulador/avisos-de-pago) deje de responder AP-CU100-12 «proveedor inactivo».
#
# QUE HACE (y solo eso)
#   · Aplica seeders/dev/19-proveedor-qr-interop-activo-test.json (el SQL de abajo lo genera scripts/generar_semillas.py;
#     no se edita): UN UPDATE, `proveedor_pago.activo = TRUE` para el codigo QR_INTEROP.
#   · NO toca la semilla minima de produccion (seeders/minimos/14), que lo deja activo = FALSE, ni url_base,
#     credenciales, comisiones o prioridad. No hay contrato con ese proveedor: en TEST lo atiende el DOBLE.
#   · No maneja secretos ni imprime ninguno.
#
# COMO SE CORRE (desde tu maquina)
#   ssh root@161.97.85.216 'bash -s'               < despliegue/ensayos/proveedor_qr_test.sh   # aplica (idempotente)
#   ssh root@161.97.85.216 'bash -s -- --estado'   < despliegue/ensayos/proveedor_qr_test.sh   # solo lee
#   ssh root@161.97.85.216 'bash -s -- --retirar'  < despliegue/ensayos/proveedor_qr_test.sh   # vuelve a activo = FALSE
#
# IDEMPOTENTE: el UPDATE solo toca filas inactivas; la segunda corrida imprime «cambios: 0».
#
# GUARDAS (abortan sin escribir nada; `--estado` y `--retirar` no las aplican)
#   1. Perfil: el contenedor aportes debe correr con el perfil `local` (el de TEST) y no con uno de produccion.
#   2. Marca de entorno de la base: si `app.entorno` esta definida, no puede ser de produccion.
#
# VARIABLES (opcionales): PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   APORTES_CONTENEDOR=<nombre>  OMITIR_GUARDA_DE_PERFIL=1 (SOLO para un ensayo local con un Postgres desechable)

CODIGO="QR_INTEROP"

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=aplicar
  case "${1:-}" in
    "") ;;
    --retirar) modo=retirar ;;
    --estado) modo=estado ;;
    -h|--help) echo "uso: ssh <host> 'bash -s [-- --estado|--retirar]' < proveedor_qr_test.sh"; return 0 ;;
    *) echo "argumento desconocido: $1" >&2; return 2 ;;
  esac

  local PG="${PG_CONTENEDOR:-aportaya-postgres}" ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}" BASE="${PG_BASE:-pasanaku}"
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
  psql_pg() { PGPASSWORD="$pgp" docker exec -i -e PGPASSWORD "$PG" psql -X -q -v ON_ERROR_STOP=1 -U "$pgu" -d "$BASE" "$@"; }
  psql_pg -tA -c 'SELECT 1' </dev/null >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; return 3; }

# >>> SQL-GENERADO · no editar: lo escribe scripts/generar_semillas.py desde seeders/dev/19-proveedor-qr-interop-activo-test.json
# search_path como el de los orquestadores: este guion corre en su propia sesion de psql.
IFS= read -r -d '' SEMILLA_SQL <<'SQL_SEMILLA_PROVEEDOR_QR' || true
SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;
-- Activa el proveedor de pago QR_INTEROP SOLO en DEV y TEST, para que la pasarela simulada de aportes pueda recorrer orden → aviso → conciliacion (el aviso simulado exige un proveedor activo con webhook).
-- GENERADO desde seeders/dev/19-proveedor-qr-interop-activo-test.json — no editar a mano.

-- SINTETICO · apto_produccion=false. Activa QR_INTEROP solo en dev/test; la semilla minima de produccion lo deja inactivo. UPDATE re-ejecutable: sin filas que cambiar en la segunda corrida.
UPDATE proveedor_pago SET activo = TRUE WHERE codigo = 'QR_INTEROP' AND activo = FALSE;
SQL_SEMILLA_PROVEEDOR_QR
# <<< SQL-GENERADO
  local SP; SP="$(printf '%s\n' "$SEMILLA_SQL" | grep -m1 '^SET search_path')"
  [ -n "$SP" ] || { echo "ERROR: el SQL generado no trae el search_path; correr scripts/generar_semillas.py" >&2; return 2; }

  estado() {   # una linea con lo que este guion maneja; sirve para probar «cambios: 0»
    psql_pg -tA -F ' ' <<SQL | tr '\n' ' '
$SP
SELECT 'qr_interop_activo=' || count(*) FROM aportes.proveedor_pago WHERE codigo = '$CODIGO' AND activo
UNION ALL SELECT 'qr_interop_filas=' || count(*) FROM aportes.proveedor_pago WHERE codigo = '$CODIGO'
UNION ALL SELECT 'otros_proveedores_activos=' || count(*) FROM aportes.proveedor_pago WHERE codigo <> '$CODIGO' AND activo;
SQL
  }

  if [ "$modo" = estado ]; then echo "[estado] $(estado)"; return 0; fi

  if [ "$modo" = retirar ]; then
    local antes; antes="$(estado)"
    psql_pg <<SQL || { echo "ERROR: el retiro fallo; no quedo nada a medias" >&2; return 4; }
BEGIN;
UPDATE aportes.proveedor_pago SET activo = FALSE WHERE codigo = '$CODIGO' AND activo;
COMMIT;
SQL
    echo "[retirar] antes:   $antes"; echo "[retirar] despues: $(estado)"
    return 0
  fi

  # Guarda 1: el perfil de aportes.
  if [ "${OMITIR_GUARDA_DE_PERFIL:-}" != 1 ]; then
    local c candidatos perfil; c="${APORTES_CONTENEDOR:-}"
    if [ -z "$c" ]; then
      candidatos="$(docker ps --format '{{.Names}}' </dev/null | grep -E '^aportes-' || true)"
      [ "$(printf '%s\n' "$candidatos" | grep -c .)" = 1 ] \
        || { echo "ERROR: esperaba UN contenedor «aportes-…» en marcha y hay: $(printf '%s' "$candidatos" | tr '\n' ' ')" >&2; return 3; }
      c="$candidatos"
    fi
    perfil="$(docker exec "$c" printenv SPRING_PROFILES_ACTIVE 2>/dev/null </dev/null)"
    case ",$perfil," in
      *,prod,*|*,production,*|*,produccion,*) echo "ERROR: aportes corre con el perfil «$perfil», de produccion: aca no se activa QR_INTEROP" >&2; return 5 ;;
    esac
    case ",$perfil," in
      *,local,*) ;;
      *) echo "ERROR: aportes corre con el perfil «${perfil:-<vacio>}», no con «local»: aca no se activa QR_INTEROP" >&2; return 5 ;;
    esac
  fi
  # Guarda 2: la marca de entorno de la base.
  local marca; marca="$(psql_pg -tA -c "SELECT coalesce(current_setting('app.entorno', true), '')" </dev/null 2>/dev/null)"
  case "$marca" in
    prod|production|produccion) echo "ERROR: la base esta marcada app.entorno=«$marca»: aca no se activa QR_INTEROP" >&2; return 5 ;;
  esac

  local antes; antes="$(estado)"; echo "[qr] antes:   $antes"
  local salida rc
  salida="$({ echo 'BEGIN;'; printf '%s\n' "$SEMILLA_SQL"; echo 'COMMIT;'; } | psql_pg 2>&1)"; rc=$?
  printf '%s\n' "$salida" | grep -v -e '^NOTICE' -e '^$' | sed -E 's/^/[qr] /' || true
  [ "$rc" = 0 ] || { echo "ERROR: la siembra fallo (una sola transaccion: no quedo nada a medias)" >&2; return 4; }
  local despues; despues="$(estado)"; echo "[qr] despues: $despues"
  if [ "$antes" = "$despues" ]; then echo "[qr] cambios: 0 (idempotente)"; else echo "[qr] cambios: si (antes != despues)"; fi
  echo "[listo] QR_INTEROP activo en TEST (doble de la pasarela; no hay contrato real). Retirar: --retirar."
}

main "$@"
