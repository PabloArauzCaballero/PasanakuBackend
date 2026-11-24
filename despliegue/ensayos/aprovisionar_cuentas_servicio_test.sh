#!/usr/bin/env bash
# Aprovisiona en TEST las CUENTAS DE SERVICIO con que grupos, entregas, inversiones y garantia entran a identidad.
#
# POR QUE EXISTE
#   Cada uno de esos servicios pide su credencial a identidad con POST /sesiones (CredencialPorLogin, comun-web):
#   telefono, clave y un segundo factor. Los roles SERVICIO_* son de ambito GLOBAL, asi que identidad los trata como
#   operador: exige un TOTP enrolado y confirmado (R-SEG-10). Desde #412 el codigo fijo `000000` ya no existe en ningun
#   perfil, asi que *_SERVICIO_FACTOR_FIJO no sirve mas. La unica forma es *_SERVICIO_TOTP_SEMILLA. Nadie aprovisionaba
#   esas cuentas en TEST: este guion las crea una vez, sin datos de personas.
#
# QUE HACE (y solo eso)
#   Para cada servicio pedido (por omision: grupos entregas inversiones garantia):
#   · Si el rol SERVICIO_<SERVICIO> no existe en identidad.rol, se salta (garantia llega con L1-M07).
#   · Si NO existe la cuenta (codigo publico fijo, abajo) ni su archivo de secretos: genera una clave de 256 bits y una
#     semilla TOTP de 160 bits, escribe el archivo y crea, en UNA transaccion: el usuario (ACTIVO, telefono sintetico NO
#     ASIGNABLE), su credencial argon2id con la pimienta de identidad, la asignacion GLOBAL del rol y el TOTP ya
#     confirmado (cifrado con la llave vigente de identidad, formato `enc:v1:` de CifradorSimetrico).
#   · Si el archivo ya existe y falta algo en la base, lo completa CON LOS VALORES DEL ARCHIVO. Nunca pisa el archivo ni
#     reemplaza una credencial, un rol o un TOTP que ya esten.
#   · Si la cuenta existe y el archivo NO: no la toca (la clave no se puede recuperar de un hash). Lo informa.
#   La asignacion del rol la otorga una cuenta de aprovisionamiento propia (SVCAPROVISIO): SUSPENDIDA y sin credencial,
#   nunca puede abrir sesion; existe para que `otorgada_por` nombre al guion y no a una persona (la base exige que quien
#   otorga no sea quien recibe).
#
# LOS SECRETOS
#   Quedan SOLO en /opt/aportaya/secretos/<servicio>-cuenta-servicio-test.env (permiso 600, carpeta 700):
#     <PREFIJO>_SERVICIO_TELEFONO, <PREFIJO>_SERVICIO_CLAVE, <PREFIJO>_SERVICIO_TOTP_SEMILLA
#   El guion no los imprime nunca. Las llaves de identidad (MFA_LLAVES, MFA_LLAVE_VIGENTE, SEGURIDAD_PIMIENTA) se leen
#   del contenedor con `docker exec … printenv`, viajan por entorno (no por argumentos) y no se guardan.
#
# DESPUES DE CORRERLO (lo dice tambien al final)
#   En Coolify, por servicio, cargar las TRES variables del archivo (cat del archivo en el VPS, sin copiarlo a otro lado).
#   El servicio solo acepta TOTP; no se configura ningún factor fijo. Si el compose declara las variables de cuenta con
#   valor literal vacío, lo cargado en Coolify NO llega al contenedor: hay que pasarlas como ${VAR:-} en
#   scripts/generar_compose.py. El guion lo detecta y lo avisa.
#
# COMO SE CORRE (desde tu maquina)
#   ssh root@161.97.85.216 'bash -s'                            < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
#   ssh root@161.97.85.216 'bash -s -- grupos entregas'         < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
#   ssh root@161.97.85.216 'bash -s -- --estado'                < despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh
#
# IDEMPOTENTE: la segunda corrida dice «creadas: 0 · completadas: 0».
#
# VARIABLES (opcionales)
#   PG_CONTENEDOR=aportaya-postgres  PG_ENV=/opt/aportaya/postgres/.env  PG_BASE=pasanaku
#   IDENTIDAD_CONTENEDOR=<nombre>    (por omision: el unico contenedor cuyo nombre empieza con «identidad-»)
#   SECRETOS_DIR=/opt/aportaya/secretos   PYTHON_BIN=python3   CRYPTO_PYLIB=<dir con cryptography y argon2-cffi>
#
# Fuera de TEST no sirve ni debe correrse: aborta si identidad no corre con el perfil `local`.

POR_OMISION=(grupos entregas inversiones garantia)

# servicio -> codigo publico (<= 12) · telefono SINTETICO NO ASIGNABLE (en Bolivia ningun numero empieza con 0 despues
# del +591; cumple el patron ^\+591\d{8}$ del login) · prefijo de variables · rol.
cuenta_de() {
  case "$1" in
    grupos)      echo "SVCGRUPOS +59100009001 GRUPOS SERVICIO_GRUPOS" ;;
    entregas)    echo "SVCENTREGAS +59100009002 ENTREGAS SERVICIO_ENTREGAS" ;;
    inversiones) echo "SVCINVERSION +59100009003 INVERSIONES SERVICIO_INVERSIONES" ;;
    garantia)    echo "SVCGARANTIA +59100009004 GARANTIA SERVICIO_GARANTIA" ;;
    *) return 1 ;;
  esac
}
OTORGANTE_CODIGO=SVCAPROVISIO
OTORGANTE_TELEFONO=+59100009000

uso() {
  echo "uso: ssh <host> 'bash -s [-- SERVICIO ...]' < aprovisionar_cuentas_servicio_test.sh   crea o completa (idempotente)"
  echo "     ssh <host> 'bash -s -- --estado'      < aprovisionar_cuentas_servicio_test.sh   informa, no escribe"
  echo "     SERVICIO: grupos | entregas | inversiones | garantia"
}

# Genera o reusa clave y semilla, y produce el hash y el cifrado. Todo entra por ENTORNO y sale por stdout como
# <clave>\t<semilla>\t<hash>\t<cifrado>. Comprueba la ida y vuelta (descifra y verifica) antes de devolver.
#   Hash: argon2id(clave + pimienta), t=3, m=65536 KiB, p=2: lo mismo que Argon2Hasheador de identidad.
#   Cifrado: llave AES = SHA-256(llave), IV de 12 bytes, GCM, "enc:v1:" + base64url sin relleno de (iv||cifrado||tag).
read -r -d '' PY_CUENTA <<'PYEOF' || true
import base64, hashlib, os, secrets
from argon2 import PasswordHasher
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
clave = os.environ.get("ENTRADA_CLAVE") or secrets.token_urlsafe(32)
semilla = os.environ.get("ENTRADA_SEMILLA") or base64.b32encode(secrets.token_bytes(20)).decode("ascii").rstrip("=")
pimienta = os.environ["ENTRADA_PIMIENTA"]
hasheador = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=2)
hash_ = hasheador.hash(clave + pimienta)
assert hasheador.verify(hash_, clave + pimienta)
llave = hashlib.sha256(os.environ["ENTRADA_LLAVE"].encode("utf-8")).digest()
iv = secrets.token_bytes(12)
cifrado = "enc:v1:" + base64.urlsafe_b64encode(iv + AESGCM(llave).encrypt(iv, semilla.encode("utf-8"), None)).decode("ascii").rstrip("=")
crudo = base64.urlsafe_b64decode(cifrado[7:] + "=" * (-len(cifrado[7:]) % 4))
assert AESGCM(llave).decrypt(crudo[:12], crudo[12:], None).decode("utf-8") == semilla
print("\t".join([clave, semilla, hash_, cifrado]))
PYEOF

main() {
  set -uo pipefail
  export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'
  local modo=aprovisionar
  local -a servicios=()
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --estado) modo=estado ;;
      -h|--help) uso; return 0 ;;
      -*) echo "argumento desconocido: $1" >&2; return 2 ;;
      *) cuenta_de "$1" >/dev/null || { echo "ERROR: «$1» no es un servicio conocido" >&2; uso >&2; return 2; }
         servicios+=("$1") ;;
    esac
    shift
  done
  [ "${#servicios[@]}" -gt 0 ] || servicios=("${POR_OMISION[@]}")

  local PG="${PG_CONTENEDOR:-aportaya-postgres}" ENV_PG="${PG_ENV:-/opt/aportaya/postgres/.env}" BASE="${PG_BASE:-pasanaku}"
  local PY="${PYTHON_BIN:-python3}" DIR="${SECRETOS_DIR:-/opt/aportaya/secretos}"
  local tmp; tmp="$(mktemp -d)"; trap 'rm -rf "${tmp:-}"' RETURN

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

  # Guarda: solo TEST (identidad con el perfil `local`).
  local idc="${IDENTIDAD_CONTENEDOR:-}"
  if [ -z "$idc" ]; then
    local candidatos; candidatos="$(docker ps --format '{{.Names}}' | grep -E '^identidad-' || true)"
    [ "$(printf '%s\n' "$candidatos" | grep -c .)" = 1 ] || { echo "ERROR: esperaba UN contenedor «identidad-…» en marcha y hay: $(printf '%s' "$candidatos" | tr '\n' ' ')" >&2; return 3; }
    idc="$candidatos"
  fi
  local perfil; perfil="$(docker exec "$idc" printenv SPRING_PROFILES_ACTIVE 2>/dev/null)"
  case ",$perfil," in *,local,*) ;; *) echo "ERROR: identidad corre con el perfil «${perfil:-<vacio>}», no con «local»: este guion es solo para TEST" >&2; return 5 ;; esac

  estado_de() { # servicio -> una linea, sin secretos
    local s="$1" cod tel pre rol
    read -r cod tel pre rol <<<"$(cuenta_de "$s")"
    local fila; fila="$(psql_pg -tA -F ' ' <<SQL
SELECT CASE WHEN EXISTS (SELECT 1 FROM identidad.rol WHERE codigo = '$rol') THEN 'rol=existe' ELSE 'rol=falta' END,
       CASE WHEN u.id IS NULL THEN 'cuenta=no' ELSE 'cuenta=si' END,
       CASE WHEN EXISTS (SELECT 1 FROM identidad.credencial_acceso c WHERE c.usuario_id = u.id) THEN 'clave=si' ELSE 'clave=no' END,
       CASE WHEN EXISTS (SELECT 1 FROM identidad.asignacion_rol a JOIN identidad.rol r ON r.id = a.rol_id
                          WHERE a.usuario_id = u.id AND r.codigo = '$rol' AND a.revocada_en IS NULL) THEN 'asignado=si' ELSE 'asignado=no' END,
       CASE WHEN EXISTS (SELECT 1 FROM identidad.factor_mfa f WHERE f.usuario_id = u.id AND f.tipo = 'TOTP'
                          AND f.activo AND f.confirmado_en IS NOT NULL) THEN 'totp=si' ELSE 'totp=no' END
  FROM (SELECT 1) uno LEFT JOIN identidad.usuario u ON u.codigo_publico = '$cod' AND u.eliminado_en IS NULL;
SQL
)"
    local arch=no; [ -f "$DIR/$s-cuenta-servicio-test.env" ] && arch=si
    echo "$s $cod $fila archivo=$arch"
  }

  if [ "$modo" = estado ]; then
    local s; for s in "${servicios[@]}"; do estado_de "$s" | sed 's/^/[estado] /'; done
    avisar_compose "$PG"
    return 0
  fi

  # Llaves de identidad: validadas como LlavesDeFactor; nunca se imprimen.
  local llaves vigente llave="" pimienta par
  llaves="$(docker exec "$idc" printenv MFA_LLAVES 2>/dev/null)"
  vigente="$(docker exec "$idc" printenv MFA_LLAVE_VIGENTE 2>/dev/null)"
  pimienta="$(docker exec "$idc" printenv SEGURIDAD_PIMIENTA 2>/dev/null)"
  [[ "$vigente" =~ ^[1-9][0-9]*$ ]] || { echo "ERROR: MFA_LLAVE_VIGENTE falta o no es un entero >= 1 en identidad" >&2; return 5; }
  local -a pares; IFS=',' read -r -a pares <<<"$llaves"
  for par in "${pares[@]}"; do
    par="${par#"${par%%[![:space:]]*}"}"
    [ "${par%%:*}" = "$vigente" ] && llave="${par#*:}"
  done
  llaves=""
  [ "${#llave}" -ge 32 ] || { llave=""; echo "ERROR: MFA_LLAVES no trae una llave de 32+ caracteres para la version vigente $vigente" >&2; return 5; }
  [ -n "$pimienta" ] || { llave=""; echo "ERROR: SEGURIDAD_PIMIENTA falta en identidad" >&2; return 5; }

  if ! ( umask 077; mkdir -p "$DIR" ) || ! chmod 700 "$DIR"; then echo "ERROR: no pude preparar $DIR" >&2; return 4; fi

  echo "[cuentas] antes:"; local s; for s in "${servicios[@]}"; do estado_de "$s" | sed 's/^/[cuentas]   /'; done
  local creadas=0 completadas=0
  for s in "${servicios[@]}"; do
    local cod tel pre rol archivo
    read -r cod tel pre rol <<<"$(cuenta_de "$s")"
    archivo="$DIR/$s-cuenta-servicio-test.env"
    if [ "$(psql_pg -tA -c "SELECT count(*) FROM identidad.rol WHERE codigo = '$rol'")" != 1 ]; then
      echo "[cuentas] $s: el rol $rol no existe todavia en identidad.rol; se salta"; continue
    fi
    local existe; existe="$(psql_pg -tA -c "SELECT count(*) FROM identidad.usuario WHERE codigo_publico = '$cod'")"
    local clave_ant="" semilla_ant=""
    if [ -f "$archivo" ]; then
      clave_ant="$(sed -n "s/^${pre}_SERVICIO_CLAVE=//p" "$archivo")"
      semilla_ant="$(sed -n "s/^${pre}_SERVICIO_TOTP_SEMILLA=//p" "$archivo")"
      [ -n "$clave_ant" ] && [ -n "$semilla_ant" ] || { echo "ERROR: $s: $archivo esta incompleto; no se toca" >&2; continue; }
    elif [ "$existe" != 0 ]; then
      echo "ERROR: $s: la cuenta $cod existe pero no hay $archivo: la clave no se puede recuperar. No se toca;" \
           "para rehacerla, revocarla a mano y volver a correr." >&2
      continue
    fi
    local salida clave semilla hash cifrado
    salida="$(calcular "$tmp" "$PY" "$llave" "$pimienta" "$clave_ant" "$semilla_ant")" \
      || { echo "ERROR: $s: no pude calcular hash y cifrado (falta python3 con cryptography y argon2-cffi, y docker)" >&2; continue; }
    IFS=$'\t' read -r clave semilla hash cifrado <<<"$salida"; salida=""
    if [ ! -f "$archivo" ]; then
      # Archivo nuevo, sin pisar nada: noclobber + umask 077 en un subshell.
      if ! ( set -o noclobber; umask 077
             printf '# Cuenta de servicio de %s en TEST (aprovisionar_cuentas_servicio_test.sh). NO copiar fuera del VPS.\n%s_SERVICIO_TELEFONO=%s\n%s_SERVICIO_CLAVE=%s\n%s_SERVICIO_TOTP_SEMILLA=%s\n' \
               "$s" "$pre" "$tel" "$pre" "$clave" "$pre" "$semilla" > "$archivo" ) 2>/dev/null; then
        clave=""; semilla=""; echo "ERROR: $s: no pude escribir $archivo (¿lo creo otro a la vez?); no se toca la base" >&2; continue
      fi
      chmod 600 "$archivo"
    fi
    clave=""; semilla=""
    local antes despues
    antes="$(estado_de "$s")"
    psql_pg -tA >/dev/null <<SQL || { hash=""; cifrado=""; echo "ERROR: $s: la base rechazo el aprovisionamiento; no quedo nada a medias (una transaccion)" >&2; continue; }
BEGIN;
INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('$OTORGANTE_CODIGO', 'Aprovisionamiento', 'Cuentas de servicio', '$OTORGANTE_TELEFONO', '2000-01-01', 'SUSPENDIDO', 'NINGUNO', 'es', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;
INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('$cod', 'Servicio', '$s', '$tel', '2000-01-01', 'ACTIVO', 'NINGUNO', 'es', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;
INSERT INTO identidad.credencial_acceso (usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en)
SELECT id, '$hash', 'argon2id', '{"m": 65536, "t": 3, "p": 2}'::jsonb, FALSE, now() FROM identidad.usuario WHERE codigo_publico = '$cod'
ON CONFLICT (usuario_id) DO NOTHING;
INSERT INTO identidad.asignacion_rol (usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en)
SELECT u.id, r.id, 'GLOBAL', NULL, o.id, now()
  FROM identidad.usuario u, identidad.rol r, identidad.usuario o
 WHERE u.codigo_publico = '$cod' AND r.codigo = '$rol' AND o.codigo_publico = '$OTORGANTE_CODIGO'
   AND NOT EXISTS (SELECT 1 FROM identidad.asignacion_rol a WHERE a.usuario_id = u.id AND a.rol_id = r.id AND a.revocada_en IS NULL);
INSERT INTO identidad.factor_mfa (usuario_id, tipo, secreto_cifrado, version_llave, activo, es_principal, confirmado_en)
SELECT u.id, 'TOTP', '$cifrado', $vigente, TRUE, TRUE, now() FROM identidad.usuario u
 WHERE u.codigo_publico = '$cod'
   AND NOT EXISTS (SELECT 1 FROM identidad.factor_mfa f WHERE f.usuario_id = u.id AND f.tipo = 'TOTP' AND f.activo AND f.confirmado_en IS NOT NULL);
COMMIT;
SQL
    hash=""; cifrado=""
    despues="$(estado_de "$s")"
    if [ "$antes" = "$despues" ]; then continue; fi
    if [ "$existe" = 0 ]; then creadas=$((creadas + 1)); else completadas=$((completadas + 1)); fi
  done
  llave=""; pimienta=""
  echo "[cuentas] despues:"; for s in "${servicios[@]}"; do estado_de "$s" | sed 's/^/[cuentas]   /'; done
  echo "[cuentas] creadas: $creadas · completadas: $completadas"
  echo "[cuentas] En Coolify, por servicio: cargar las 3 variables de $DIR/<servicio>-cuenta-servicio-test.env"
  echo "[cuentas]   (<PREFIJO>_SERVICIO_TELEFONO, _CLAVE y _TOTP_SEMILLA)."
  avisar_compose "$PG"
}

# El compose de Coolify puede declarar las variables con valor literal vacio: entonces lo cargado en Coolify no llega.
avisar_compose() {
  local c pre
  for c in $(docker ps --format '{{.Names}}' | grep -E '^(grupos|entregas|inversiones|garantia)-' || true); do
    pre="$(printf '%s' "${c%%-*}" | tr '[:lower:]' '[:upper:]')"
    if [ -z "$(docker exec "$c" printenv "${pre}_SERVICIO_TELEFONO" 2>/dev/null)" ]; then
      echo "[aviso] ${c%%-*}: el contenedor NO recibe ${pre}_SERVICIO_TELEFONO. Si docker-compose.coolify.yml la declara vacia," \
           "cargarla en Coolify no alcanza: hay que pasarla como \${${pre}_SERVICIO_TELEFONO:-} en scripts/generar_compose.py."
    fi
  done
}

# Python local con las dos librerias → pip a un directorio temporal → contenedor python. Todo por entorno.
# SC2030/SC2031: el export vive A PROPOSITO solo dentro del subshell: los secretos no quedan en el entorno del guion.
# shellcheck disable=SC2030,SC2031
calcular() {
  local tmp="$1" py="$2" ll="$3" pim="$4" cl="$5" se="$6" out
  corre_local() {
    ( export ENTRADA_LLAVE="$ll" ENTRADA_PIMIENTA="$pim" ENTRADA_CLAVE="$cl" ENTRADA_SEMILLA="$se"
      [ -n "${CRYPTO_PYLIB:-}" ] && export PYTHONPATH="$CRYPTO_PYLIB${PYTHONPATH:+:$PYTHONPATH}"
      [ -n "${1:-}" ] && export PYTHONPATH="$1${PYTHONPATH:+:$PYTHONPATH}"
      "$py" -c "$PY_CUENTA" )
  }
  if command -v "$py" >/dev/null 2>&1; then
    if out="$(corre_local 2>/dev/null)" && [ -n "$out" ]; then printf '%s' "$out"; return 0; fi
    if "$py" -m pip --version >/dev/null 2>&1 \
       && "$py" -m pip install -q --target "$tmp/pylib" cryptography argon2-cffi >/dev/null 2>&1 \
       && out="$(corre_local "$tmp/pylib" 2>/dev/null)" && [ -n "$out" ]; then
      printf '%s' "$out"; return 0
    fi
  fi
  command -v docker >/dev/null 2>&1 || return 1
  out="$( export ENTRADA_LLAVE="$ll" ENTRADA_PIMIENTA="$pim" ENTRADA_CLAVE="$cl" ENTRADA_SEMILLA="$se"
          docker run --rm -e ENTRADA_LLAVE -e ENTRADA_PIMIENTA -e ENTRADA_CLAVE -e ENTRADA_SEMILLA python:3.12-slim \
            sh -c 'pip install -q cryptography argon2-cffi >/dev/null 2>&1; exec python -c "$1"' sh "$PY_CUENTA" 2>/dev/null )" || return 1
  [ -n "$out" ] || return 1
  printf '%s' "$out"
}

main "$@"
