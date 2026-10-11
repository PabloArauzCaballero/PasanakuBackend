#!/usr/bin/env bash
# Siembra en TEST dos cuentas SINTETICAS de prueba (A: organizador/invitante, B: invitado) para recorrer
# «A organiza e invita, B acepta o rechaza» con login real. Alta normal no sirve en TEST: exige un codigo
# de 6 digitos que llega por correo a un buzon que nadie lee (evidencia/carril-H2).
#
# QUE HACE (y solo eso)
#   · Aplica seeders/dev/17-usuarios-e2e-sinteticos.json (el SQL de abajo lo genera scripts/generar_semillas.py;
#     no se edita): 3 usuarios sinteticos, sus roles, su segundo factor y la habilitacion de A como organizador.
#   · Les pone credencial: Argon2id de `contrasena + pimienta`, igual que Argon2Hasheador. La pimienta se lee
#     del entorno del contenedor de identidad (docker exec ... printenv): no pasa por la linea de comandos,
#     no se imprime y no se guarda. Las contrasenas vienen de un archivo que vive FUERA del repositorio.
#   · No toca otra tabla, no corre nada de las semillas de dev y NO imprime credenciales.
#
# COMO SE CORRE (desde tu maquina; `cuentas-test.txt` ya trae las contrasenas como asignaciones de shell)
#   cat "$TEMP/aportaya-s/cuentas-test.txt" despliegue/ensayos/sembrar_test.sh | ssh root@161.97.85.216 'bash -s'
#   ssh root@161.97.85.216 'bash -s -- --estado'  < despliegue/ensayos/sembrar_test.sh    # solo cuenta, no escribe
#   ssh root@161.97.85.216 'bash -s -- --retirar' < despliegue/ensayos/sembrar_test.sh    # las da de baja
#   (con `bash -s` el guion llega por la entrada estandar: todo el cuerpo esta dentro de main() para que ningun
#    comando se coma lo que falta leer.)
#
# IDEMPOTENTE: ids estables + ON CONFLICT DO NOTHING; la credencial se pone solo si falta. Volver a correrlo
# no cambia nada («cambios: 0»). `--retirar` es una baja logica (revoca roles y sesiones, quita credencial y
# factor, deshabilita al organizador y marca a los usuarios eliminados); correr el guion de nuevo las reactiva.
#
# VARIABLES (todas opcionales, para otros hosts o ensayos locales)
#   PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   IDENTIDAD_CONTENEDOR=<nombre>    (por omision: el unico contenedor cuyo nombre empieza con «identidad-»)
#   PYTHON_BIN=python3  ARGON2_PYLIB=<dir con argon2-cffi>
#
# Fuera de TEST no sirve ni debe correrse: aborta si identidad no corre con el perfil `local` (el que trae
# el segundo factor de desarrollo) y no existen en produccion.

CODIGOS=(SINT-E2E-A SINT-E2E-B)
LISTA_SQL="'SINT-E2E-A','SINT-E2E-B'"
MOTIVO_RETIRO="retiro de cuentas sinteticas de prueba"

uso() {
  echo "uso: cat cuentas-test.txt sembrar_test.sh | ssh <host> 'bash -s'     siembra (idempotente)"
  echo "     ssh <host> 'bash -s -- --estado'  < sembrar_test.sh             cuenta, no escribe"
  echo "     ssh <host> 'bash -s -- --retirar' < sembrar_test.sh             baja logica de las cuentas"
}

# El hash lo calcula Python (argon2-cffi): la pimienta y las claves llegan por entorno, nunca por argumentos.
read -r -d '' PY_HASH <<'PYEOF' || true
import os, sys
from argon2 import PasswordHasher
# Los mismos parametros que Argon2Hasheador (3 iteraciones, 64 MiB, paralelismo 2, hash de 32 bytes).
ph = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=2, hash_len=32, salt_len=16)
pimienta = os.environ["ENTRADA_PIMIENTA"]
for cod in sys.argv[1:]:
    clave = os.environ["ENTRADA_CLAVE_" + cod.replace("-", "_")]
    h = ph.hash(clave + pimienta)
    ph.verify(h, clave + pimienta)
    print(cod + "\t" + h)
PYEOF

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=sembrar
  case "${1:-}" in
    "") ;;
    --retirar) modo=retirar ;;
    --estado) modo=estado ;;
    -h|--help) uso; return 0 ;;
    *) echo "argumento desconocido: $1" >&2; return 2 ;;
  esac

  local PG="${PG_CONTENEDOR:-aportaya-postgres}" ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}" BASE="${PG_BASE:-pasanaku}"
  local PY="${PYTHON_BIN:-python3}"
  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "${tmp:-}"' RETURN

  # ---- la base: usuario y clave desde el .env del servidor, sin imprimirlos --------------------------
  valor_env() {   # KEY=valor, con o sin comillas; sin ejecutar nada del archivo
    [ -r "$ENV_PG" ] || return 0
    grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ENV_PG" | head -1 | sed -E "s/^[^=]*=//; s/^[\"']//; s/[\"']\$//"
  }
  docker inspect -f '{{.State.Running}}' "$PG" 2>/dev/null | grep -q true || { echo "ERROR: no encuentro el contenedor de Postgres «$PG» en marcha" >&2; return 3; }
  local pgu pgp
  pgu="$(valor_env POSTGRES_USER)"; [ -n "$pgu" ] || pgu="$(valor_env BD_USUARIO_ADMIN)"
  [ -n "$pgu" ] || pgu="$(docker exec "$PG" printenv POSTGRES_USER 2>/dev/null)"
  pgp="$(valor_env POSTGRES_PASSWORD)"; [ -n "$pgp" ] || pgp="$(valor_env BD_CLAVE_ADMIN)"
  [ -n "$pgu" ] || { echo "ERROR: no pude determinar el usuario administrador de la base" >&2; return 3; }
  psql_pg() { PGPASSWORD="$pgp" docker exec -i -e PGPASSWORD "$PG" psql -X -q -v ON_ERROR_STOP=1 -U "$pgu" -d "$BASE" "$@"; }
  psql_pg -tA -c 'SELECT 1' >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; return 3; }

  conteos() {   # una linea con lo que esta guion maneja; sirve para probar «cambios: 0»
    psql_pg -tA -F ' ' <<SQL
SELECT 'usuarios=' || count(*) FROM identidad.usuario WHERE codigo_publico LIKE 'SINT-E2E-%'
UNION ALL SELECT 'activos=' || count(*) FROM identidad.usuario WHERE codigo_publico LIKE 'SINT-E2E-%' AND eliminado_en IS NULL
UNION ALL SELECT 'roles_vigentes=' || count(*) FROM identidad.asignacion_rol a JOIN identidad.usuario u ON u.id = a.usuario_id WHERE u.codigo_publico LIKE 'SINT-E2E-%' AND a.revocada_en IS NULL
UNION ALL SELECT 'factores_activos=' || count(*) FROM identidad.factor_mfa f JOIN identidad.usuario u ON u.id = f.usuario_id WHERE u.codigo_publico LIKE 'SINT-E2E-%' AND f.activo
UNION ALL SELECT 'organizador_habilitado=' || count(*) FROM organizador.organizador o JOIN identidad.usuario u ON u.id = o.usuario_id WHERE u.codigo_publico LIKE 'SINT-E2E-%' AND o.estado = 'HABILITADO'
UNION ALL SELECT 'credenciales=' || count(*) FROM identidad.credencial_acceso c JOIN identidad.usuario u ON u.id = c.usuario_id WHERE u.codigo_publico LIKE 'SINT-E2E-%';
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
UPDATE identidad.sesion SET revocada_en = now(), motivo_revocacion = '$MOTIVO_RETIRO'
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND revocada_en IS NULL;
UPDATE identidad.asignacion_rol SET revocada_en = now(), motivo_revocacion = '$MOTIVO_RETIRO'
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND revocada_en IS NULL;
UPDATE identidad.factor_mfa SET activo = false
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND activo;
DELETE FROM identidad.credencial_acceso
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL));
UPDATE organizador.organizador SET estado = 'DESHABILITADO', fecha_suspension = now()
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND estado <> 'DESHABILITADO';
UPDATE identidad.usuario SET estado = 'ELIMINADO', eliminado_en = now()
 WHERE codigo_publico IN ($LISTA_SQL) AND eliminado_en IS NULL;
COMMIT;
SQL
    echo "[retirar] antes:   $antes"
    echo "[retirar] despues: $(linea_de_conteos)"
    echo "[retirar] listo. Los grupos e invitaciones que se hayan creado en la prueba quedan como historial (no se borran)."
    return 0
  fi

  # ---- sembrar --------------------------------------------------------------------------------------
  # Guarda 1: el perfil. Estas cuentas dependen del segundo factor de desarrollo; donde no corre el perfil
  # `local` no deben existir.
  local idc="${IDENTIDAD_CONTENEDOR:-}"
  if [ -z "$idc" ]; then
    local candidatos; candidatos="$(docker ps --format '{{.Names}}' | grep -E '^identidad-' || true)"
    [ "$(printf '%s\n' "$candidatos" | grep -c .)" = 1 ] || { echo "ERROR: esperaba UN contenedor «identidad-…» en marcha y hay: $(printf '%s' "$candidatos" | tr '\n' ' ')" >&2; return 3; }
    idc="$candidatos"
  fi
  local perfil; perfil="$(docker exec "$idc" printenv SPRING_PROFILES_ACTIVE 2>/dev/null)"
  case ",$perfil," in *,local,*) ;; *) echo "ERROR: identidad corre con el perfil «${perfil:-<vacio>}», no con «local»: aca no se siembran cuentas sinteticas" >&2; return 5 ;; esac
  # Guarda 2: las contrasenas (fuera del repositorio). Solo se valida el largo: nunca se imprimen.
  local clave_a="${CUENTA_A_CLAVE:-}" clave_b="${CUENTA_B_CLAVE:-}"
  if [ "${#clave_a}" -lt 16 ] || [ "${#clave_b}" -lt 16 ]; then
    echo "ERROR: faltan CUENTA_A_CLAVE / CUENTA_B_CLAVE (16 caracteres o mas). Se pasan asi:" >&2
    echo "  cat \"\$TEMP/aportaya-s/cuentas-test.txt\" despliegue/ensayos/sembrar_test.sh | ssh root@161.97.85.216 'bash -s'" >&2
    return 2
  fi

  local antes; antes="$(linea_de_conteos)"
  echo "[sembrar] antes:   $antes"

  # 1) usuarios, roles, factor y organizador (SQL generado)
# >>> SQL-GENERADO · no editar: lo escribe scripts/generar_semillas.py desde seeders/dev/17-usuarios-e2e-sinteticos.json
# search_path como el de los orquestadores: este guion corre en su propia sesion de psql.
IFS= read -r -d '' SEMILLA_SQL <<'SQL_SEMILLA_E2E' || true
SET search_path TO aportes, auditoria, cumplimiento, entregas, erp, garantia, grupos, identidad, inversiones, notificaciones, nucleo_financiero, organizador, publicidad, tarifas, transparencia, catalogo, comun, public;
-- Dos cuentas SINTETICAS de prueba para recorrer de punta a punta «A organiza e invita, B acepta o rechaza» contra TEST: A (organizador habilitado) y B (invitado), mas un otorgante sin acceso que solo existe para firmar las asignaciones de rol.
-- GENERADO desde seeders/dev/17-usuarios-e2e-sinteticos.json — no editar a mano.

-- SINTETICO · apto_produccion=false. Tres personas inventadas. nivel_kyc BASICO en A y B es una asignacion de la semilla, no el resultado de una verificacion (ver advertencia).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO usuario (id, codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro) VALUES
      ('e2e00000-0000-4000-8000-000000000000', 'SINT-E2E-0', 'Otorgante', 'Sintetico E2E (sin acceso)', '+59170009100', 'sint-e2e-0@example.invalid', '1990-01-01', 'SUSPENDIDO', 'NINGUNO', 'es-BO', 'America/La_Paz', now()),
      ('e2e00000-0000-4000-8000-000000000001', 'SINT-E2E-A', 'Cuenta A', 'Sintetica E2E (organizador)', '+59170009101', 'sint-e2e-a@example.invalid', '1990-01-01', 'ACTIVO', 'BASICO', 'es-BO', 'America/La_Paz', now()),
      ('e2e00000-0000-4000-8000-000000000002', 'SINT-E2E-B', 'Cuenta B', 'Sintetica E2E (invitado)', '+59170009102', 'sint-e2e-b@example.invalid', '1990-01-01', 'ACTIVO', 'BASICO', 'es-BO', 'America/La_Paz', now())
    ON CONFLICT (codigo_publico) DO NOTHING;
  END IF;
END $siembra$;

-- A: ORGANIZADOR (trae GRUPO_CREAR y GRUPO_ADMINISTRAR) y PARTICIPANTE; B: PARTICIPANTE. Ambito GLOBAL como en 15-usuarios-dev.json. Nadie se asigna un rol a si mismo (ck_asignacion_no_autoasignada): firma el otorgante sintetico.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO asignacion_rol (id, usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en, vigente_hasta) VALUES
      ('e2e00000-0000-4000-8000-0000000000a1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), (SELECT id FROM rol WHERE codigo = 'ORGANIZADOR'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL),
      ('e2e00000-0000-4000-8000-0000000000a2', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), (SELECT id FROM rol WHERE codigo = 'PARTICIPANTE'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL),
      ('e2e00000-0000-4000-8000-0000000000a3', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-B'), (SELECT id FROM rol WHERE codigo = 'PARTICIPANTE'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;

-- Segundo factor SMS enrolado y confirmado: sin factor, el ingreso de un dispositivo nuevo termina en AP-CU04-04. El secreto es un marcador, no un secreto: el codigo lo decide el perfil local de TEST.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO factor_mfa (id, usuario_id, tipo, secreto_cifrado, activo, es_principal, confirmado_en, version_llave) VALUES
      ('e2e00000-0000-4000-8000-0000000000b1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), 'SMS', 'enc:v1:sintetico-e2e-sms-a', TRUE, TRUE, now() - interval '1 hour', 1),
      ('e2e00000-0000-4000-8000-0000000000b2', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-B'), 'SMS', 'enc:v1:sintetico-e2e-sms-b', TRUE, TRUE, now() - interval '1 hour', 1)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;

-- SINTETICO · A figura HABILITADO, nivel APRENDIZ, con los limites iniciales de aportaya.organizador (2 grupos, 30000.00): es lo unico que consulta grupos antes de crear un grupo. No hubo postulacion, capacitacion ni contrato (ver advertencia).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO organizador (id, usuario_id, estado, nivel, limite_grupos_simultaneos, limite_monto_administrado, grupos_activos, grupos_historicos, monto_administrado_actual, calificacion_promedio, indice_morosidad_cartera, fecha_postulacion, fecha_habilitacion) VALUES
      ('e2e00000-0000-4000-8000-0000000000c1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), 'HABILITADO', 'APRENDIZ', 2, 30000.0, 0, 0, 0, 0, 0, now(), now())
    ON CONFLICT (usuario_id) DO NOTHING;
  END IF;
END $siembra$;
SQL_SEMILLA_E2E
# <<< SQL-GENERADO
  local salida_sql rc_sql
  salida_sql="$({ echo 'BEGIN;'; printf '%s\n' "$SEMILLA_SQL"; echo 'COMMIT;'; } | psql_pg 2>&1)"; rc_sql=$?
  printf '%s\n' "$salida_sql" | grep -v -e '^NOTICE' -e '^$' | sed -E 's/^/[sembrar] /' || true
  [ "$rc_sql" = 0 ] || { echo "ERROR: la siembra fallo (una sola transaccion: no quedo nada a medias)" >&2; return 4; }

  # 1b) si una corrida anterior las retiro, esta las reactiva (solo toca lo que el retiro cambio)
  psql_pg <<SQL || { echo "ERROR: no pude reactivar" >&2; return 4; }
BEGIN;
UPDATE identidad.usuario SET estado = 'ACTIVO', eliminado_en = NULL WHERE codigo_publico IN ($LISTA_SQL) AND eliminado_en IS NOT NULL;
UPDATE identidad.asignacion_rol SET revocada_en = NULL, motivo_revocacion = NULL
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND motivo_revocacion = '$MOTIVO_RETIRO';
UPDATE identidad.factor_mfa SET activo = true
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND NOT activo;
UPDATE organizador.organizador SET estado = 'HABILITADO', fecha_suspension = NULL
 WHERE usuario_id IN (SELECT id FROM identidad.usuario WHERE codigo_publico IN ($LISTA_SQL)) AND estado = 'DESHABILITADO';
COMMIT;
SQL

  # 2) credencial, solo a quien no la tiene: la pimienta sale del contenedor de identidad
  local faltan; faltan="$(psql_pg -tA <<SQL
SELECT u.codigo_publico FROM identidad.usuario u LEFT JOIN identidad.credencial_acceso c ON c.usuario_id = u.id
 WHERE u.codigo_publico IN ($LISTA_SQL) AND c.id IS NULL ORDER BY 1;
SQL
)"
  if [ -n "$faltan" ]; then
    local pimienta; pimienta="$(docker exec "$idc" printenv SEGURIDAD_PIMIENTA 2>/dev/null)"
    [ "${#pimienta}" -ge 8 ] || { echo "ERROR: no pude leer SEGURIDAD_PIMIENTA del contenedor de identidad" >&2; return 5; }
    local -a pendientes; mapfile -t pendientes <<<"$faltan"
    local salida; salida="$(hashear "$tmp" "$PY" "$pimienta" "$clave_a" "$clave_b" "${pendientes[@]}")" \
      || { echo "ERROR: no pude calcular el hash Argon2id (ni con python3+argon2-cffi, ni con pip, ni con un contenedor python)" >&2; return 6; }
    local cod h
    while IFS=$'\t' read -r cod h; do
      # Python en Windows imprime CRLF y un \r pegado al hash lo invalida para Java (se midio: AP-CU04-01 a la cuenta A).
      cod="${cod%$'\r'}"; h="${h%$'\r'}"
      [ -n "$cod" ] || continue
      # Solo se guarda lo que tiene la forma exacta de un hash de Argon2Hasheador (id, v19, 64 MiB, 3 pasadas, p=2).
      [[ "$h" =~ ^\$argon2id\$v=19\$m=65536,t=3,p=2\$[A-Za-z0-9+/]+\$[A-Za-z0-9+/]+$ ]] \
        || { echo "ERROR: el hash calculado para $cod no tiene la forma esperada; no se guarda nada" >&2; return 6; }
      salida_sql="$(printf '%s\n' "BEGIN;" \
        "INSERT INTO identidad.credencial_acceso (usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en)" \
        "SELECT id, '$h', 'ARGON2ID', '{\"iteraciones\":3,\"memoriaKiB\":65536,\"paralelismo\":2}'::jsonb, FALSE, now()" \
        "  FROM identidad.usuario WHERE codigo_publico = '$cod' ON CONFLICT (usuario_id) DO NOTHING;" \
        "COMMIT;" | psql_pg 2>&1)"; rc_sql=$?
      if [ "$rc_sql" != 0 ]; then
        # Nada de lo que psql repita del SQL puede llevar el hash a la pantalla.
        printf '%s\n' "$salida_sql" | sed -E 's/\$argon2[^ ,'"'"']*/<hash>/g' >&2
        echo "ERROR: no pude guardar la credencial de $cod" >&2; return 4
      fi
    done <<<"$salida"
    echo "[sembrar] credencial puesta a: $(printf '%s ' "${pendientes[@]}")"
    pimienta=""; salida=""
  else
    echo "[sembrar] las dos cuentas ya tenian credencial: no se tocaron (cambia la contrasena con el flujo normal si hace falta)"
  fi

  local despues; despues="$(linea_de_conteos)"
  echo "[sembrar] despues: $despues"
  if [ "$antes" = "$despues" ]; then echo "[sembrar] cambios: 0 (idempotente)"; else echo "[sembrar] cambios: si (antes != despues)"; fi

  # 3) prueba de punta a punta contra identidad (no abre sesion: sin segundo factor responde AP-CU04-03
  #    cuando la credencial es correcta, AP-CU04-01 cuando no)
  sondear() {   # sondear <telefono> <clave>
    local json req resp
    json="$(printf '{"telefonoE164":"%s","credencial":"%s","huellaDispositivo":"sembrar-test-sonda","plataforma":"WEB"}' "$1" "$2")"
    req="$(printf 'POST /sesiones HTTP/1.0\r\nHost: localhost\r\nContent-Type: application/json\r\nContent-Length: %s\r\nConnection: close\r\n\r\n%s' "${#json}" "$json")"
    resp="$(printf '%s' "$req" | docker exec -i "$idc" bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080; cat >&3; timeout 20 cat <&3' 2>/dev/null)"
    printf '%s' "$resp" | head -1 | tr -d '\r' | awk '{printf "http=%s ", $2}'
    printf '%s' "$resp" | grep -o '"codigo":"[^"]*"' | head -1
  }
  local tel_a="+59170009101" tel_b="+59170009102"
  echo "[sonda] A: $(sondear "$tel_a" "$clave_a")   (esperado: http=422 y AP-CU04-03 = credencial correcta, falta el segundo factor)"
  echo "[sonda] B: $(sondear "$tel_b" "$clave_b")   (esperado: igual)"
  echo "[listo] cuentas SINTETICAS sembradas (apto_produccion=false). Telefonos, contrasenas y segundo factor: tu archivo cuentas-test.txt."
}

# Reparte el calculo del hash: python local con argon2-cffi → pip a un directorio temporal → contenedor python.
# La pimienta y las claves viajan por entorno (builtin export): nunca por la linea de comandos.
hashear() {
  local tmp="$1" py="$2" pim="$3" ca="$4" cb="$5"; shift 5
  local -a cods=("$@")
  local out n="${#cods[@]}"
  corre_local() {
    ( export ENTRADA_PIMIENTA="$pim" ENTRADA_CLAVE_SINT_E2E_A="$ca" ENTRADA_CLAVE_SINT_E2E_B="$cb"
      [ -n "${ARGON2_PYLIB:-}" ] && export PYTHONPATH="$ARGON2_PYLIB${PYTHONPATH:+:$PYTHONPATH}"
      [ -n "${1:-}" ] && export PYTHONPATH="$1${PYTHONPATH:+:$PYTHONPATH}"
      "$py" -c "$PY_HASH" "${cods[@]}" )
  }
  if command -v "$py" >/dev/null 2>&1; then
    if out="$(corre_local 2>/dev/null)" && [ "$(printf '%s\n' "$out" | grep -c .)" = "$n" ]; then printf '%s\n' "$out"; return 0; fi
    if "$py" -m pip --version >/dev/null 2>&1 \
       && "$py" -m pip install -q --target "$tmp/pylib" argon2-cffi >/dev/null 2>&1 \
       && out="$(corre_local "$tmp/pylib" 2>/dev/null)" && [ "$(printf '%s\n' "$out" | grep -c .)" = "$n" ]; then
      printf '%s\n' "$out"; return 0
    fi
  fi
  command -v docker >/dev/null 2>&1 || return 1
  out="$( export ENTRADA_PIMIENTA="$pim" ENTRADA_CLAVE_SINT_E2E_A="$ca" ENTRADA_CLAVE_SINT_E2E_B="$cb"
          docker run --rm -e ENTRADA_PIMIENTA -e ENTRADA_CLAVE_SINT_E2E_A -e ENTRADA_CLAVE_SINT_E2E_B python:3.12-slim \
            sh -c 'pip install -q argon2-cffi >/dev/null 2>&1; codigo="$1"; shift; exec python -c "$codigo" "$@"' sh "$PY_HASH" "${cods[@]}" 2>/dev/null )" || return 1
  [ "$(printf '%s\n' "$out" | grep -c .)" = "$n" ] || return 1
  printf '%s\n' "$out"
}

main "$@"
