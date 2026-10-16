#!/usr/bin/env bash
# Aplica el esquema y los catalogos minimos sobre la base del entorno.
#
# Se conecta con PGHOST/PGUSER/PGPASSWORD/PGDATABASE, que inyecta el entorno. NUNCA
# lleva una credencial escrita: el dia que la lleve, esta imagen es el secreto.
#
# `ON_ERROR_STOP=1` en los dos pasos: un esquema aplicado a medias es peor que uno
# no aplicado, porque el servicio arranca y falla recien al tocar la tabla que falto.
set -euo pipefail

: "${PGHOST:?falta PGHOST}"
: "${PGUSER:?falta PGUSER}"
: "${PGDATABASE:?falta PGDATABASE}"

echo "esquema: esperando a ${PGHOST}"
for intento in $(seq 1 60); do
  pg_isready -q && break
  [ "$intento" = "60" ] && { echo "esquema: la base no respondio en 120 s"; exit 1; }
  sleep 2
done

# EXPANDIR antes de aplicar (regla 97.3: expandir, migrar, contraer).
#
# Los CREATE TABLE son `IF NOT EXISTS`: sobre una base que ya tiene la tabla NO agregan las
# columnas que el modelo sumo despues, y el primer `COMMENT ON COLUMN` / indice / restriccion que
# las nombra aborta TODA la transaccion de aplicar.sql. Medido (evidencia/carril-D3/FASE1.md):
#   grupos.invitacion.consumidor_id does not exist -> esquema sale con 3 -> ningun servicio arranca.
#
# Sin tocar el generador: se aplica aplicar.sql en una base de REFERENCIA descartable (la misma
# imagen, el mismo sql/) y, por cada columna de la referencia que la tabla real no tiene, se
# emite ADD COLUMN IF NOT EXISTS. Derivado y aditivo: no cambia tipos, no aprieta nada, no borra.
# Una columna NOT NULL sin DEFAULT entra opcional (no se puede imponer sobre filas existentes).
# Sin permiso para CREATE DATABASE solo avisa y sigue (el comportamiento anterior).
# Sobre una base vacia no hace nada.
expandir_columnas() {
  local ref="${PGDATABASE}_ref" hay
  hay=$(psql -tAc "select count(*) from information_schema.tables where table_schema = 'identidad'" 2>/dev/null || echo 0)
  if [ "${hay:-0}" -eq 0 ]; then echo "esquema: base vacia, nada que expandir"; return 0; fi
  psql -qc "DROP DATABASE IF EXISTS \"${ref}\"" >/dev/null 2>&1
  if ! psql -qc "CREATE DATABASE \"${ref}\"" >/dev/null 2>&1; then
    echo "esquema: AVISO sin permiso para crear la base de referencia: no se expanden columnas"; return 0
  fi
  if ! psql -d "${ref}" -v ON_ERROR_STOP=1 -q -f /sql/aplicar.sql >/dev/null 2>&1; then
    echo "esquema: AVISO la referencia no aplico: no se expanden columnas"
    psql -qc "DROP DATABASE IF EXISTS \"${ref}\"" >/dev/null 2>&1; return 0
  fi
  psql -d "${ref}" -tA -v ON_ERROR_STOP=1 > /tmp/expandir.sql <<'SQL'
SELECT format($f$DO $q$ BEGIN
  IF to_regclass(%L) IS NOT NULL AND NOT EXISTS (SELECT 1 FROM information_schema.columns
       WHERE table_schema = %L AND table_name = %L AND column_name = %L) THEN
    ALTER TABLE %I.%I ADD COLUMN %I %s%s%s;
    RAISE NOTICE 'esquema: columna agregada %%', %L;
  END IF; END $q$;$f$,
  n.nspname || '.' || c.relname, n.nspname, c.relname, a.attname,
  n.nspname, c.relname, a.attname, format_type(a.atttypid, a.atttypmod),
  CASE WHEN d.adbin IS NOT NULL THEN ' DEFAULT ' || pg_get_expr(d.adbin, d.adrelid) ELSE '' END,
  CASE WHEN a.attnotnull AND d.adbin IS NOT NULL THEN ' NOT NULL' ELSE '' END,
  n.nspname || '.' || c.relname || '.' || a.attname)
  FROM pg_attribute a
  JOIN pg_class c ON c.oid = a.attrelid AND c.relkind = 'r'
  JOIN pg_namespace n ON n.oid = c.relnamespace
  LEFT JOIN pg_attrdef d ON d.adrelid = a.attrelid AND d.adnum = a.attnum
 WHERE a.attnum > 0 AND NOT a.attisdropped AND a.attgenerated = ''
   AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('information_schema', 'public')
 ORDER BY n.nspname, c.relname, a.attnum;
-- Lo mismo para los CHECK en linea del CREATE TABLE (listas de valores que crecieron): el
-- CREATE TABLE IF NOT EXISTS no los refresca y la semilla nueva (p. ej. una cuenta de tipo
-- CUSTODIA_INVERSION) los viola. Se reemplaza el CHECK cuya definicion difiere; ensanchar una
-- lista no puede fallar por datos, y si estrecha una y hay filas fuera, FALLA y es lo correcto.
SELECT format($f$DO $q$ DECLARE d text; BEGIN
  IF to_regclass(%L) IS NOT NULL THEN
    SELECT pg_get_constraintdef(k.oid) INTO d FROM pg_constraint k
     WHERE k.conrelid = %L::regclass AND k.conname = %L;
    -- PostgreSQL reescribe la lista de un IN en el CREATE TABLE y en un ADD CONSTRAINT con
    -- casts y parentesis distintos: se comparan sin ellos para que la segunda corrida no
    -- recree lo que no cambio. Un cambio de valores o de numeros si difiere.
    IF regexp_replace(d, '::(character varying|text)(\[\])?|[()]', '', 'g')
       IS DISTINCT FROM regexp_replace(%L, '::(character varying|text)(\[\])?|[()]', '', 'g') THEN
      IF d IS NOT NULL THEN ALTER TABLE %I.%I DROP CONSTRAINT %I; END IF;
      ALTER TABLE %I.%I ADD CONSTRAINT %I %s;
      RAISE NOTICE 'esquema: restriccion actualizada %%', %L;
    END IF;
  END IF; END $q$;$f$,
  n.nspname || '.' || c.relname, n.nspname || '.' || c.relname, k.conname,
  pg_get_constraintdef(k.oid),
  n.nspname, c.relname, k.conname,
  n.nspname, c.relname, k.conname, pg_get_constraintdef(k.oid),
  n.nspname || '.' || c.relname || '.' || k.conname)
  FROM pg_constraint k
  JOIN pg_class c ON c.oid = k.conrelid AND c.relkind = 'r'
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE k.contype = 'c' AND n.nspname !~ '^pg_' AND n.nspname NOT IN ('information_schema', 'public')
 ORDER BY n.nspname, c.relname, k.conname;
SQL
  psql -qc "DROP DATABASE IF EXISTS \"${ref}\"" >/dev/null 2>&1
  psql -1 -v ON_ERROR_STOP=1 -q -f /tmp/expandir.sql > /tmp/expandir.log 2>&1
  if grep -q 'ERROR' /tmp/expandir.log; then grep 'ERROR' /tmp/expandir.log | head -5; echo "esquema: la expansion fallo"; return 1; fi
  echo "esquema: columnas expandidas: $(grep -c 'columna agregada' /tmp/expandir.log)"
  echo "esquema: restricciones CHECK actualizadas: $(grep -c 'restriccion actualizada' /tmp/expandir.log)"
  grep -E 'columna agregada|restriccion actualizada' /tmp/expandir.log | sed 's/^.*esquema: /esquema:   + /' | head -80 || true
  return 0
}
expandir_columnas || exit 1

echo "esquema: aplicando sql/aplicar.sql"
psql -v ON_ERROR_STOP=1 -q -f /sql/aplicar.sql

echo "esquema: catalogos minimos (sql/60_semillas)"
psql -v ON_ERROR_STOP=1 -q -f /sql/60_semillas/sembrar.sql

# Credencial de los roles de servicio que aun no la tienen.
#
# Los roles `svc_*` nacen NOLOGIN (sql/00_base/02_esquemas.sql) y la credencial la entrega el
# entorno. Cada servicio NUEVO trae su rol nuevo: `svc_inversiones` aparecio con este trabajo,
# nace NOLOGIN y sin clave, y el servicio no puede conectarse (PgBouncer: «no such user» /
# «password authentication failed») ni queda sano; con las olas del compose, ademas, arrastra a
# todos los que dependen de el. Un script a mano con la lista de los catorce roles viejos no lo
# cubre. Si el entorno define ROLES_CLAVE, aca se le da esa clave SOLO a los `svc_*` que todavia
# no tienen login o no tienen clave; los que ya la tienen no se tocan (no se rota nada). Sin
# ROLES_CLAVE no se hace nada: produccion sigue entregando la credencial por su gestor de secretos.
if [ -n "${ROLES_CLAVE:-}" ]; then
  psql -v ON_ERROR_STOP=1 -q -v clave="${ROLES_CLAVE}" <<'SQL' | sed 's/^/esquema: /'
SELECT format('ALTER ROLE %I LOGIN PASSWORD %L', rolname, :'clave')
  FROM pg_roles
 WHERE rolname LIKE 'svc\_%' AND NOT (rolcanlogin AND rolpassword IS NOT NULL)
 ORDER BY rolname
\gexec
SELECT 'roles svc_* con credencial: ' || count(*) FILTER (WHERE rolcanlogin AND rolpassword IS NOT NULL)
       || ' de ' || count(*) FROM pg_roles WHERE rolname LIKE 'svc\_%';
SQL
fi

# Lo que se afirma se mide: un conteo de cero significa que algo se aplico contra
# otra base, y sin esto el trabajo terminaria en verde igual.
tablas=$(psql -tAc "select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')")
echo "esquema: ${tablas} tablas en ${PGDATABASE}"
[ "${tablas}" -ge 300 ] || { echo "esquema: se esperaban 300+ tablas, hay ${tablas}"; exit 1; }
echo "esquema: listo"
