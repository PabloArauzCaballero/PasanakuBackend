#!/usr/bin/env bash
# Pone en TEST la VERSION NUEVA de la politica de token INVITACION_GRUPO (canales ENLACE,SMS,WHATSAPP,CORREO), para que
# `POST /grupos/{id}/invitaciones` con `canal=ENLACE` —el canal por omision de la app— deje de responder 500 AP-INT-01
# (identidad lo rechazaba: la fila sembrada en TEST solo permite SMS,WHATSAPP,CORREO; carril L3, H2).
#
# POR QUE ES UNA VERSION NUEVA Y NO UN UPDATE
#   La politica es dato versionado: `emitir` toma la fila con `vigente_desde` mas reciente que ya rige. Cambiar un parametro
#   es AGREGAR una fila con `vigente_desde` posterior (seeders/minimos/13-politicas-de-token.json, que tambien siembra las
#   bases nuevas), nunca editar la vigente: la siembra es `ON CONFLICT (proposito, vigente_desde) DO NOTHING` y una base ya
#   poblada conserva la fila vieja si solo se edita (asi quedo TEST). Regla 97.1.5: no se corrige a mano en la base; se
#   corrige en el seeder y se vuelve a aplicar. Los tokens ya emitidos se validan con la politica que regia al emitirlos.
#
# QUE HACE (y solo eso)
#   · Aplica el SQL generado de seeders/minimos/13-politicas-de-token.json (el bloque de abajo lo escribe
#     scripts/generar_semillas.py; no se edita): INSERT ... ON CONFLICT (proposito, vigente_desde) DO NOTHING de las
#     politicas de token. En TEST las filas viejas ya existen y no se tocan; entra la fila nueva de INVITACION_GRUPO.
#   · No toca otra tabla, no imprime secretos (no maneja ninguno) y no cambia ninguna fila existente.
#   · Despues de aplicar verifica que la politica VIGENTE de INVITACION_GRUPO permita ENLACE; si no, sale con error.
#
# COMO SE CORRE (desde tu maquina; NO lo corre ningun agente)
#   ssh root@161.97.85.216 'bash -s'              < despliegue/ensayos/politica_invitacion_test.sh   # aplica (idempotente)
#   ssh root@161.97.85.216 'bash -s -- --estado'  < despliegue/ensayos/politica_invitacion_test.sh   # solo lee
#   (con `bash -s` el guion llega por la entrada estandar: todo el cuerpo esta dentro de main() y ningun comando lee la
#    entrada estandar, para que nada se coma lo que falta leer.)
#
# IDEMPOTENTE: la segunda corrida no cambia nada («cambios: 0»). No hay `--retirar`: una politica versionada no se borra;
# para volver atras se publica otra version con `vigente_desde` posterior (en el seeder).
#
# VARIABLES (todas opcionales, para otros hosts o ensayos locales)
#   PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   PG_USUARIO / PG_CLAVE   (SOLO para el ensayo local con un Postgres desechable sin .env de servidor)

PROPOSITO="INVITACION_GRUPO"
CANAL="ENLACE"

uso() {
  echo "uso: ssh <host> 'bash -s'              < politica_invitacion_test.sh   aplica la version nueva (idempotente)"
  echo "     ssh <host> 'bash -s -- --estado'  < politica_invitacion_test.sh   solo lee"
}

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=aplicar
  case "${1:-}" in
    "") ;;
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
  pgu="${PG_USUARIO:-}"; [ -n "$pgu" ] || pgu="$(valor_env POSTGRES_USER)"; [ -n "$pgu" ] || pgu="$(valor_env BD_USUARIO_ADMIN)"
  [ -n "$pgu" ] || pgu="$(docker exec "$PG" printenv POSTGRES_USER 2>/dev/null </dev/null)"
  pgp="${PG_CLAVE:-}"; [ -n "$pgp" ] || pgp="$(valor_env POSTGRES_PASSWORD)"; [ -n "$pgp" ] || pgp="$(valor_env BD_CLAVE_ADMIN)"
  [ -n "$pgu" ] || { echo "ERROR: no pude determinar el usuario administrador de la base" >&2; return 3; }
  # psql_pg lee de la entrada que le pasen (un heredoc o un pipe): nunca de la del script.
  psql_pg() { PGPASSWORD="$pgp" docker exec -i -e PGPASSWORD "$PG" psql -X -q -v ON_ERROR_STOP=1 -U "$pgu" -d "$BASE" "$@"; }
  psql_pg -tA -c 'SELECT 1' </dev/null >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; return 3; }

  # El SQL de la politica (generado). De el sale tambien el search_path: las sentencias nombran la tabla sin esquema.
# >>> SQL-GENERADO · no editar: lo escribe scripts/generar_semillas.py desde seeders/minimos/13-politicas-de-token.json
# search_path como el de los orquestadores: este guion corre en su propia sesion de psql.
IFS= read -r -d '' SEMILLA_SQL <<'SQL_SEMILLA_POLITICA' || true
SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;
-- Parámetros de emisión y validación de tokens, uno por propósito. Sin la fila, el propósito no puede emitir token: el vencimiento, el largo del código y el tope de intentos son dato, no constante en el código.
-- GENERADO desde seeders/minimos/13-politicas-de-token.json — no editar a mano.

-- OTP de seis digitos para confirmar el correo durante el alta mediante Gmail API.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('VERIFICACION_CORREO', 600, 6, 5, 2, 60, 5, 'CORREO', FALSE, TRUE, '2026-10-07T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- OTP numéricos: código corto, vida corta y pocos intentos. La ventana de 5 minutos y los 3 intentos son el estándar de segundo factor en banca móvil.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('SEGUNDO_FACTOR', 300, 6, 3, 3, 60, 10, 'SMS,WHATSAPP,APP_AUTENTICADORA', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('INICIO_SESION_SIN_CONTRASENA', 300, 6, 3, 3, 60, 10, 'SMS,WHATSAPP', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('VERIFICACION_TELEFONO', 600, 6, 5, 3, 60, 8, 'SMS,WHATSAPP,LLAMADA_VOZ', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('RECUPERACION_CONTRASENA', 900, 8, 3, 2, 120, 5, 'SMS,CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CAMBIO_TELEFONO', 300, 6, 3, 2, 120, 3, 'CORREO', TRUE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CAMBIO_CORREO', 300, 6, 3, 2, 120, 3, 'SMS,WHATSAPP', TRUE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('AUTORIZACION_DISPOSITIVO', 300, 6, 3, 2, 120, 5, 'SMS,WHATSAPP,CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CONFIRMACION_ENTREGA', 3600, 6, 3, 2, 60, 5, 'SMS,WHATSAPP,PUSH_APP', TRUE, TRUE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- Enlaces firmados: no son adivinables, así que la vida es más larga, pero `longitud_codigo` es la del secreto aleatorio y no un código que alguien tipea.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('VERIFICACION_CORREO', 86400, 32, 5, 2, 300, 5, 'CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('INVITACION_GRUPO', 604800, 32, 5, 2, 300, 20, 'SMS,WHATSAPP,CORREO', FALSE, FALSE, '2026-01-01T00:00:00-04:00'),
  ('INVITACION_GRUPO', 604800, 32, 5, 2, 300, 20, 'ENLACE,SMS,WHATSAPP,CORREO', FALSE, FALSE, '2026-10-11T00:00:00-04:00'),
  ('ENLACE_PAGO', 172800, 32, 10, 3, 120, 20, 'SMS,WHATSAPP,PUSH_APP', FALSE, FALSE, '2026-01-01T00:00:00-04:00'),
  ('FIRMA_REGLAMENTO', 259200, 32, 5, 2, 300, 5, 'WHATSAPP,CORREO,PUSH_APP', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('EXPORTACION_DATOS', 604800, 32, 3, 1, 600, 2, 'CORREO', TRUE, TRUE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- Refresh de sesión del backoffice (ADR-010): viaja en cookie httpOnly + Secure + SameSite estricto, se rota en cada uso y su reuso revoca la familia (trigger R-SEG-09). Vive lo mismo que la sesión (aportaya.acceso.vigencia-sesion, 12 h). Cada uso emite uno nuevo, por eso el tope diario es alto.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('REFRESCO_SESION', 43200, 64, 1, 0, 0, 200, 'COOKIE_HTTPONLY', FALSE, FALSE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;
SQL_SEMILLA_POLITICA
# <<< SQL-GENERADO
  local SP; SP="$(printf '%s\n' "$SEMILLA_SQL" | grep -m1 '^SET search_path')"
  [ -n "$SP" ] || { echo "ERROR: el SQL generado no trae el search_path; correr scripts/generar_semillas.py" >&2; return 2; }

  conteos() {   # una linea con lo que este guion maneja; sirve para probar «cambios: 0»
    psql_pg -tA -F ' ' <<SQL
SELECT 'politicas_total=' || count(*) FROM identidad.politica_token
UNION ALL SELECT 'invitacion_versiones=' || count(*) FROM identidad.politica_token WHERE proposito = '$PROPOSITO'
UNION ALL SELECT 'invitacion_vigente_desde=' || coalesce((SELECT to_char(vigente_desde AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI"Z"')
   FROM identidad.politica_token WHERE proposito = '$PROPOSITO' AND vigente_desde <= now() ORDER BY vigente_desde DESC LIMIT 1), 'ninguna')
UNION ALL SELECT 'invitacion_vigente_canales=' || coalesce((SELECT canales_permitidos
   FROM identidad.politica_token WHERE proposito = '$PROPOSITO' AND vigente_desde <= now() ORDER BY vigente_desde DESC LIMIT 1), 'ninguna')
UNION ALL SELECT 'invitacion_vigente_permite_$CANAL=' || coalesce((SELECT (string_to_array(replace(canales_permitidos, ' ', ''), ',') @> ARRAY['$CANAL'])::text
   FROM identidad.politica_token WHERE proposito = '$PROPOSITO' AND vigente_desde <= now() ORDER BY vigente_desde DESC LIMIT 1), 'false');
SQL
  }
  linea_de_conteos() { conteos | tr '\n' ' '; }

  if [ "$modo" = estado ]; then
    echo "[estado] $(linea_de_conteos)"
    return 0
  fi

  local antes; antes="$(linea_de_conteos)"
  echo "[politica] antes:   $antes"

  # Una sola transaccion: o entra todo o no entra nada.
  local salida_sql rc_sql
  salida_sql="$({ echo 'BEGIN;'; printf '%s\n' "$SEMILLA_SQL"; echo 'COMMIT;'; } | psql_pg 2>&1)"; rc_sql=$?
  printf '%s\n' "$salida_sql" | grep -v -e '^NOTICE' -e '^$' | sed -E 's/^/[politica] /' || true
  [ "$rc_sql" = 0 ] || { echo "ERROR: la siembra fallo (una sola transaccion: no quedo nada a medias)" >&2; return 4; }

  local despues; despues="$(linea_de_conteos)"
  echo "[politica] despues: $despues"
  case "$despues" in
    *"invitacion_vigente_permite_$CANAL=true"*) ;;
    *) echo "ERROR: despues de aplicar, la politica vigente de $PROPOSITO NO permite $CANAL (¿hay otra version con vigente_desde posterior?)" >&2; return 6 ;;
  esac
  if [ "$antes" = "$despues" ]; then echo "[politica] cambios: 0 (idempotente)"; else echo "[politica] cambios: si (antes != despues)"; fi
  echo "[listo] $PROPOSITO permite $CANAL. Probar: POST /grupos/{id}/invitaciones con canal=$CANAL debe dar 201."
}

main "$@"
