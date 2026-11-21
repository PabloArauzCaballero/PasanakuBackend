#!/usr/bin/env bash
# Arranque del PRIMER segundo factor TOTP en TEST (F1.S3.M2, PR #412).
#
# POR QUE EXISTE
#   TEST entraba al backoffice con el codigo fijo `000000` del perfil `local`. Ese codigo ya no existe en ningun
#   perfil. Un operador sin TOTP no puede abrir sesion (R-SEG-10) y enrolar el TOTP exige una sesion: huevo y
#   gallina. Este guion rompe el ciclo UNA vez por cuenta, en TEST, sembrando un TOTP ya confirmado.
#
# QUE HACE (y solo eso)
#   · Para cada codigo publico pedido (por omision: SINT-E2E-A y SINT-E2E-B), si la cuenta NO tiene un TOTP activo y
#     confirmado: genera una semilla de 160 bits, la cifra con la llave vigente de identidad (AES-256-GCM, el mismo
#     formato `enc:v1:` que CifradorSimetrico) e inserta el factor TOTP activo y principal. Los otros factores de esa
#     cuenta dejan de ser principales (no se desactivan).
#   · Las llaves (MFA_LLAVES, MFA_LLAVE_VIGENTE) se leen del contenedor de identidad con `docker exec ... printenv`:
#     no pasan por la linea de comandos, no se imprimen y no se guardan.
#   · La semilla y el URI otpauth se muestran UNA vez, solo en la terminal de quien corre el guion, para cargarlos en
#     la app autenticadora. No van a ningun log ni a ningun archivo. No los pegues en un chat ni en un ticket.
#   · Si la cuenta ya tiene TOTP, no se toca: no se reemplaza un factor ajeno.
#
# COMO SE CORRE (desde tu maquina)
#   ssh root@161.97.85.216 'bash -s'                         < despliegue/ensayos/arrancar_totp_test.sh   # SINT-E2E-A y B
#   ssh root@161.97.85.216 'bash -s -- <CODIGO_PUBLICO> ...' < despliegue/ensayos/arrancar_totp_test.sh   # el operador
#   ssh root@161.97.85.216 'bash -s -- --estado'             < despliegue/ensayos/arrancar_totp_test.sh   # solo cuenta
#   El codigo publico del operador esta en /opt/aportaya/credenciales-admin-test.txt del VPS (nunca en el repositorio).
#
# IDEMPOTENTE: una cuenta con TOTP activo y confirmado se salta; la segunda corrida dice «sembrados: 0».
#
# VARIABLES (opcionales)
#   PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   IDENTIDAD_CONTENEDOR=<nombre>    (por omision: el unico contenedor cuyo nombre empieza con «identidad-»)
#   PYTHON_BIN=python3  CRYPTO_PYLIB=<dir con cryptography>
#
# Fuera de TEST no sirve ni debe correrse: aborta si identidad no corre con el perfil `local`.

POR_OMISION=(SINT-E2E-A SINT-E2E-B)

uso() {
  echo "uso: ssh <host> 'bash -s [-- CODIGO_PUBLICO ...]' < arrancar_totp_test.sh   siembra el TOTP (idempotente)"
  echo "     ssh <host> 'bash -s -- --estado'                < arrancar_totp_test.sh   cuenta, no escribe"
}

# Semilla y cifrado en Python. La llave llega por ENTORNO, nunca por argumentos. Salida: <semilla>\t<cifrado>.
# Formato de CifradorSimetrico: llave AES = SHA-256(llave), IV de 12 bytes, GCM con etiqueta de 128 bits,
# "enc:v1:" + base64url sin relleno de (iv || cifrado || etiqueta). Se descifra antes de devolver (ida y vuelta).
read -r -d '' PY_TOTP <<'PYEOF' || true
import base64, hashlib, os, secrets
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
llave = hashlib.sha256(os.environ["ENTRADA_LLAVE"].encode("utf-8")).digest()
semilla = base64.b32encode(secrets.token_bytes(20)).decode("ascii").rstrip("=")
iv = secrets.token_bytes(12)
carga = iv + AESGCM(llave).encrypt(iv, semilla.encode("utf-8"), None)
cifrado = "enc:v1:" + base64.urlsafe_b64encode(carga).decode("ascii").rstrip("=")
crudo = base64.urlsafe_b64decode(cifrado[7:] + "=" * (-len(cifrado[7:]) % 4))
assert AESGCM(llave).decrypt(crudo[:12], crudo[12:], None).decode("utf-8") == semilla
print(semilla + "\t" + cifrado)
PYEOF

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=sembrar
  local -a codigos=()
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --estado) modo=estado ;;
      -h|--help) uso; return 0 ;;
      -*) echo "argumento desconocido: $1" >&2; return 2 ;;
      *)
        # Codigo publico: mayusculas, digitos y guiones (USR000091, SINT-E2E-A). Nada que pueda romper el SQL.
        [[ "$1" =~ ^[A-Z0-9-]{3,12}$ ]] || { echo "ERROR: «$1» no es un codigo publico valido" >&2; return 2; }
        codigos+=("$1") ;;
    esac
    shift
  done
  [ "${#codigos[@]}" -gt 0 ] || codigos=("${POR_OMISION[@]}")

  local PG="${PG_CONTENEDOR:-aportaya-postgres}" ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}" BASE="${PG_BASE:-pasanaku}"
  local PY="${PYTHON_BIN:-python3}"
  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "${tmp:-}"' RETURN

  # ---- la base: usuario y clave desde el .env del servidor, sin imprimirlos --------------------------
  valor_env() {
    [ -r "$ENV_PG" ] || return 0
    grep -E "^[[:space:]]*(export[[:space:]]+)?$1=" "$ENV_PG" | head -1 | sed -E "s/^[^=]*=//; s/^[\"']//; s/[\"']\$//"
  }
  docker inspect -f '{{.State.Running}}' "$PG" 2>/dev/null | grep -q true || { echo "ERROR: no encuentro el contenedor de Postgres «$PG» en marcha" >&2; return 3; }
  local pgu pgp
  pgu="$(valor_env POSTGRES_USER)"; pgp="$(valor_env POSTGRES_PASSWORD)"
  [ -n "$pgu" ] || pgu="$(docker exec "$PG" printenv POSTGRES_USER 2>/dev/null)"
  [ -n "$pgp" ] || pgp="$(docker exec "$PG" printenv POSTGRES_PASSWORD 2>/dev/null)"
  psql_pg() { PGPASSWORD="$pgp" docker exec -i -e PGPASSWORD "$PG" psql -X -q -v ON_ERROR_STOP=1 -U "$pgu" -d "$BASE" "$@"; }
  psql_pg -tA -c 'SELECT 1' >/dev/null 2>&1 || { echo "ERROR: no pude conectarme a la base «$BASE»" >&2; return 3; }

  local lista_sql; lista_sql="$(printf "'%s'," "${codigos[@]}")"; lista_sql="${lista_sql%,}"
  estado() {
    psql_pg -tA -F ' ' <<SQL
SELECT u.codigo_publico,
       CASE WHEN EXISTS (SELECT 1 FROM identidad.factor_mfa f
                          WHERE f.usuario_id = u.id AND f.tipo = 'TOTP' AND f.activo AND f.confirmado_en IS NOT NULL)
            THEN 'totp=si' ELSE 'totp=no' END
  FROM identidad.usuario u
 WHERE u.codigo_publico IN ($lista_sql) AND u.eliminado_en IS NULL
 ORDER BY u.codigo_publico;
SQL
  }

  if [ "$modo" = estado ]; then
    estado | sed 's/^/[estado] /'
    return 0
  fi

  # Guarda 1: el perfil. Solo en TEST.
  local idc="${IDENTIDAD_CONTENEDOR:-}"
  if [ -z "$idc" ]; then
    local candidatos; candidatos="$(docker ps --format '{{.Names}}' | grep -E '^identidad-' || true)"
    [ "$(printf '%s\n' "$candidatos" | grep -c .)" = 1 ] || { echo "ERROR: esperaba UN contenedor «identidad-…» en marcha y hay: $(printf '%s' "$candidatos" | tr '\n' ' ')" >&2; return 3; }
    idc="$candidatos"
  fi
  local perfil; perfil="$(docker exec "$idc" printenv SPRING_PROFILES_ACTIVE 2>/dev/null)"
  case ",$perfil," in *,local,*) ;; *) echo "ERROR: identidad corre con el perfil «${perfil:-<vacio>}», no con «local»: este arranque es solo para TEST" >&2; return 5 ;; esac

  # Guarda 2: las llaves, leidas del contenedor y validadas igual que LlavesDeFactor. Nunca se imprimen.
  local llaves vigente llave=""
  llaves="$(docker exec "$idc" printenv MFA_LLAVES 2>/dev/null)"
  vigente="$(docker exec "$idc" printenv MFA_LLAVE_VIGENTE 2>/dev/null)"
  [[ "$vigente" =~ ^[1-9][0-9]*$ ]] || { echo "ERROR: MFA_LLAVE_VIGENTE falta o no es un entero >= 1 en el contenedor de identidad (cargala en Coolify)" >&2; return 5; }
  local par
  IFS=',' read -r -a pares <<<"$llaves"
  for par in "${pares[@]}"; do
    par="${par#"${par%%[![:space:]]*}"}"
    if [ "${par%%:*}" = "$vigente" ]; then llave="${par#*:}"; fi
  done
  llaves=""
  [ "${#llave}" -ge 32 ] || { llave=""; echo "ERROR: MFA_LLAVES no trae una llave de 32+ caracteres para la version vigente $vigente (cargala en Coolify)" >&2; return 5; }

  echo "[totp] antes:"; estado | sed 's/^/[totp]   /'
  local sembrados=0 cod id semilla cifrado salida
  for cod in "${codigos[@]}"; do
    id="$(psql_pg -tA -c "SELECT id FROM identidad.usuario WHERE codigo_publico = '$cod' AND eliminado_en IS NULL")"
    if [ -z "$id" ]; then echo "[totp] $cod: no existe o esta dado de baja; se salta" >&2; continue; fi
    if [ "$(psql_pg -tA -c "SELECT count(*) FROM identidad.factor_mfa WHERE usuario_id = '$id' AND tipo = 'TOTP' AND activo AND confirmado_en IS NOT NULL")" != 0 ]; then
      echo "[totp] $cod: ya tiene TOTP activo; no se reemplaza"; continue
    fi
    salida="$(cifrar "$tmp" "$PY" "$llave")" || { llave=""; echo "ERROR: no pude cifrar la semilla (falta python3 con cryptography y docker no esta disponible)" >&2; return 6; }
    semilla="${salida%%$'\t'*}"; cifrado="${salida#*$'\t'}"; salida=""
    # Una transaccion por cuenta. La precondicion va en la escritura: si otro sembro un TOTP entre la consulta y aca,
    # el INSERT no inserta nada y la semilla generada se descarta sin mostrarse.
    local filas
    filas="$(psql_pg -tA <<SQL 2>/dev/null
BEGIN;
WITH nuevo AS (
  INSERT INTO identidad.factor_mfa (usuario_id, tipo, secreto_cifrado, version_llave, activo, es_principal, confirmado_en)
  SELECT '$id', 'TOTP', '$cifrado', $vigente, TRUE, TRUE, now()
   WHERE NOT EXISTS (SELECT 1 FROM identidad.factor_mfa
                      WHERE usuario_id = '$id' AND tipo = 'TOTP' AND activo AND confirmado_en IS NOT NULL)
  RETURNING id)
SELECT count(*) FROM nuevo;
UPDATE identidad.factor_mfa SET es_principal = FALSE
 WHERE usuario_id = '$id' AND tipo <> 'TOTP' AND es_principal;
COMMIT;
SQL
)"
    cifrado=""
    if [ "$(printf '%s\n' "$filas" | grep -E '^[0-9]+$' | head -1)" != 1 ]; then
      semilla=""; echo "ERROR: $cod: no se inserto el TOTP (otro lo sembro antes o la base lo rechazo); no se muestra ninguna semilla" >&2; continue
    fi
    sembrados=$((sembrados + 1))
    echo "[totp] $cod: TOTP sembrado. Cargalo AHORA en la app autenticadora (se muestra una sola vez):"
    echo "         semilla:  $semilla"
    echo "         otpauth:  otpauth://totp/AportaYa%3A$cod?secret=$semilla&issuer=AportaYa&algorithm=SHA1&digits=6&period=30"
    semilla=""
  done
  llave=""
  echo "[totp] despues:"; estado | sed 's/^/[totp]   /'
  echo "[totp] sembrados: $sembrados"
}

# Cifra en Python: local con cryptography → pip a un directorio temporal → contenedor python.
# La llave viaja por entorno (export dentro de un subshell), nunca por la linea de comandos.
cifrar() {
  local tmp="$1" py="$2" ll="$3" out
  corre_local() {
    ( export ENTRADA_LLAVE="$ll"
      [ -n "${CRYPTO_PYLIB:-}" ] && export PYTHONPATH="$CRYPTO_PYLIB${PYTHONPATH:+:$PYTHONPATH}"
      [ -n "${1:-}" ] && export PYTHONPATH="$1${PYTHONPATH:+:$PYTHONPATH}"
      "$py" -c "$PY_TOTP" )
  }
  if command -v "$py" >/dev/null 2>&1; then
    if out="$(corre_local 2>/dev/null)" && [ -n "$out" ]; then printf '%s' "$out"; return 0; fi
    if "$py" -m pip --version >/dev/null 2>&1 \
       && "$py" -m pip install -q --target "$tmp/pylib" cryptography >/dev/null 2>&1 \
       && out="$(corre_local "$tmp/pylib" 2>/dev/null)" && [ -n "$out" ]; then
      printf '%s' "$out"; return 0
    fi
  fi
  command -v docker >/dev/null 2>&1 || return 1
  out="$( export ENTRADA_LLAVE="$ll"
          docker run --rm -e ENTRADA_LLAVE python:3.12-slim \
            sh -c 'pip install -q cryptography >/dev/null 2>&1; exec python -c "$1"' sh "$PY_TOTP" 2>/dev/null )" || return 1
  [ -n "$out" ] || return 1
  printf '%s' "$out"
}

main "$@"
