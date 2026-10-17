#!/usr/bin/env bash
# Siembra usuarios SINTETICOS con credencial real (Argon2id) en la base del ensayo, para que un agente
# de aceptacion pueda iniciar sesion. Las contrasenas se generan AHORA y se guardan solo en el
# archivo de salida, FUERA del repositorio (por omision $D3_DIR/credenciales-e2e.txt, modo 600).
# Nada de esto toca sql/ ni seeders/: no son datos del producto.
#
#   bash despliegue/ensayos/arranque/sembrar_usuarios.sh [archivo-de-salida]
#
# Crea: admin (ADMIN_PLATAFORMA), backoffice (OFICIAL_CUMPLIMIENTO, el que decide expedientes),
# participante A y participante B (PARTICIPANTE) para el ensayo de «aceptar y rechazar», y tesoreria (TESORERIA) y
# comercial (COMERCIAL) para los tableros por rol.
# Requiere el stack arriba (postgres del ensayo) y, para el hash, argon2-cffi (se instala solo en
# $D3_DIR/pylib). El segundo factor en perfil `local` es `000000` para cualquiera (DesafioDeDesarrollo).
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
SALIDA_CRED="${1:-$D3_DIR/credenciales-e2e.txt}"
case "$(cd "$(dirname "$SALIDA_CRED")" && pwd)/" in "$RAIZ"/*) echo "ERROR: la salida no puede estar dentro del repositorio"; exit 2;; esac
PG="$PROYECTO_INFRA-postgres-1"
docker inspect "$PG" >/dev/null 2>&1 || { echo "ERROR: el postgres del ensayo no esta arriba"; exit 3; }
PGPW="$(grep ^BD_CLAVE_ADMIN= "$ENVF" | cut -d= -f2)"
[ -d "$D3_DIR/pylib/argon2" ] || python3 -m pip install -q --target "$D3_DIR/pylib" argon2-cffi >/dev/null 2>&1
umask 077
SQL="$D3_DIR/sembrar-usuarios.sql"
PIMIENTA="$(grep ^SEGURIDAD_PIMIENTA= "$ENVF" | cut -d= -f2)"
# identidad hashea `credencial + pimienta` (Argon2Hasheador.conPimienta): el hash sembrado tiene que llevarla.
PIMIENTA="$PIMIENTA" PYTHONPATH="$D3_DIR/pylib" python3 - "$SALIDA_CRED" "$SQL" <<'PY'
import os, secrets, sys
from argon2 import PasswordHasher
ph = PasswordHasher(time_cost=3, memory_cost=65536, parallelism=4)   # los mismos parametros que identidad
cred, sql = sys.argv[1], sys.argv[2]
usuarios = [  # codigo, rol, nombre, telefono
    ("USRE2E01", "ADMIN_PLATAFORMA", "Admin", "+59172000001"),
    ("USRE2E02", "OFICIAL_CUMPLIMIENTO", "Backoffice", "+59172000002"),
    ("USRE2E03", "PARTICIPANTE", "ParticipanteA", "+59172000003"),
    ("USRE2E04", "PARTICIPANTE", "ParticipanteB", "+59172000004"),
    # Tableros por rol: los duenos del tablero financiero (TESORERIA) y del comercial (COMERCIAL). El de sistemas es
    # ADMIN_PLATAFORMA (USRE2E01). Ver sql/60_semillas/10-roles-y-permisos.sql.
    ("USRE2E05", "TESORERIA", "Tesoreria", "+59172000005"),
    ("USRE2E06", "COMERCIAL", "Comercial", "+59172000006"),
]
# R-ROL: nadie se asigna un rol a si mismo (ck_asignacion_no_autoasignada): un otorgante sintetico sin rol.
out = ["""INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('USRE2E00', 'Otorgante', 'Sintetico', '+59172000000', 'usre2e00@ejemplo.invalid', '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;"""]
lineas = ["# Credenciales SINTETICAS del ensayo E2E (generadas ahora). No reusar. Segundo factor en perfil local: 000000"]
for cod, rol, nombre, tel in usuarios:
    clave = secrets.token_urlsafe(18)
    h = ph.hash(clave + os.environ["PIMIENTA"])
    lineas.append(f"{rol:22s} telefono={tel} credencial={clave} codigo={cod}")
    out.append(f"""
INSERT INTO identidad.usuario (codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro)
VALUES ('{cod}', '{nombre}', 'Sintetico', '{tel}', '{cod.lower()}@ejemplo.invalid', '1990-01-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', now())
ON CONFLICT (codigo_publico) DO NOTHING;
INSERT INTO identidad.credencial_acceso (usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en)
SELECT id, '{h}', 'argon2id', '{{"m": 65536, "t": 3, "p": 4}}'::jsonb, FALSE, now() FROM identidad.usuario WHERE codigo_publico = '{cod}'
ON CONFLICT (usuario_id) DO UPDATE SET hash_contrasena = EXCLUDED.hash_contrasena;
INSERT INTO identidad.asignacion_rol (usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en)
SELECT u.id, r.id, 'GLOBAL', NULL, (SELECT id FROM identidad.usuario WHERE codigo_publico = 'USRE2E00'), now() FROM identidad.usuario u, identidad.rol r
 WHERE u.codigo_publico = '{cod}' AND r.codigo = '{rol}'
   AND NOT EXISTS (SELECT 1 FROM identidad.asignacion_rol a WHERE a.usuario_id = u.id AND a.rol_id = r.id);
""")
    # R-SEG-10: backoffice exige TOTP confirmado; el participante necesita un factor enrolado (SMS) o
    # el segundo paso del ingreso responde AP-CU04-04. En perfil local el codigo es 000000 para ambos.
    tipo_factor = "SMS" if rol == "PARTICIPANTE" else "TOTP"
    if True:
        out.append(f"""
INSERT INTO identidad.factor_mfa (usuario_id, tipo, secreto_cifrado, activo, es_principal, confirmado_en, version_llave)
SELECT id, '{tipo_factor}', 'enc:v1:e2e-sintetico', TRUE, TRUE, now(), 1 FROM identidad.usuario WHERE codigo_publico = '{cod}'
 AND NOT EXISTS (SELECT 1 FROM identidad.factor_mfa f WHERE f.usuario_id = identidad.usuario.id);
""")
open(sql, "w", encoding="utf-8").write("BEGIN;\n" + "".join(out) + "COMMIT;\n")
open(cred, "w", encoding="utf-8").write("\n".join(lineas) + "\n")
PY
chmod 600 "$SALIDA_CRED" 2>/dev/null
docker exec -i -e PGPASSWORD="$PGPW" "$PG" psql -v ON_ERROR_STOP=1 -q -U pasanaku -d pasanaku < "$SQL" && rm -f "$SQL"
echo "usuarios sembrados: $(docker exec -e PGPASSWORD="$PGPW" "$PG" psql -tA -U pasanaku -d pasanaku -c "select count(*) from identidad.usuario where codigo_publico like 'USRE2E%'")"
echo "credenciales (fuera del repo): $SALIDA_CRED"
