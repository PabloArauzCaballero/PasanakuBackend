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

echo "esquema: aplicando sql/aplicar.sql"
psql -v ON_ERROR_STOP=1 -q -f /sql/aplicar.sql

echo "esquema: catalogos minimos (sql/60_semillas)"
psql -v ON_ERROR_STOP=1 -q -f /sql/60_semillas/sembrar.sql

# Lo que se afirma se mide: un conteo de cero significa que algo se aplico contra
# otra base, y sin esto el trabajo terminaria en verde igual.
tablas=$(psql -tAc "select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')")
echo "esquema: ${tablas} tablas en ${PGDATABASE}"
[ "${tablas}" -ge 300 ] || { echo "esquema: se esperaban 300+ tablas, hay ${tablas}"; exit 1; }
echo "esquema: listo"
