#!/usr/bin/env bash
# Compara la base ACTUALIZADA (esquema viejo + job de esquema nuevo, con datos) contra una base
# NUEVA (solo el esquema actual) y lista la deriva: columnas, restricciones CHECK, indices,
# triggers, funciones y roles. Es la verificacion de deriva de la regla 97.2 aplicada a la
# actualizacion. Requiere el contenedor postgres del ensayo arriba y la imagen aportaya/esquema.
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
PG="$PROYECTO_INFRA-postgres-1"
PGPW="$(grep ^BD_CLAVE_ADMIN= "$ENVF" | cut -d= -f2)"
pq() { docker exec -i -e PGPASSWORD="$PGPW" "$PG" psql -v ON_ERROR_STOP=1 -q -tA -U pasanaku -d "$1" "${@:2}"; }
pq postgres -c "DROP DATABASE IF EXISTS pasanaku_fresco" -c "CREATE DATABASE pasanaku_fresco" >/dev/null
docker run --rm --network aportaya-d3-interna -e PGHOST=postgres -e PGUSER=pasanaku -e PGPASSWORD="$PGPW" -e PGDATABASE=pasanaku_fresco aportaya/esquema:test 2>&1 | tail -2
huella() {
  pq "$1" <<'SQL'
select 'COL '||table_schema||'.'||table_name||'.'||column_name||' '||data_type||' null='||is_nullable||' def='||coalesce(regexp_replace(column_default,'[0-9a-f-]{36}','<uuid>','g'),'-')
  from information_schema.columns where table_schema not in ('pg_catalog','information_schema')
union all
select 'CON '||n.nspname||'.'||c.relname||' '||k.conname||' '||pg_get_constraintdef(k.oid)
  from pg_constraint k join pg_class c on c.oid=k.conrelid join pg_namespace n on n.oid=c.relnamespace where n.nspname not in ('pg_catalog','information_schema')
union all
select 'IDX '||schemaname||'.'||indexname||' '||indexdef from pg_indexes where schemaname not in ('pg_catalog','information_schema')
union all
select 'TRG '||event_object_schema||'.'||event_object_table||' '||trigger_name from information_schema.triggers
union all
select 'FUN '||n.nspname||'.'||p.proname||'('||pg_get_function_identity_arguments(p.oid)||') definer='||p.prosecdef from pg_proc p join pg_namespace n on n.oid=p.pronamespace where n.nspname not in ('pg_catalog','information_schema')
union all
select 'ROL '||rolname||' login='||rolcanlogin from pg_roles where rolname like 'svc\_%' or rolname like 'rol\_%'
order by 1;
SQL
}
huella pasanaku > "$D3_DIR/huella-actualizada.txt"
huella pasanaku_fresco > "$D3_DIR/huella-fresca.txt"
echo "huellas: actualizada=$(wc -l < "$D3_DIR/huella-actualizada.txt") fresca=$(wc -l < "$D3_DIR/huella-fresca.txt")"
echo "=== SOLO en la base fresca (el esquema actual lo tiene, la actualizada NO) ==="
comm -13 <(sort "$D3_DIR/huella-actualizada.txt") <(sort "$D3_DIR/huella-fresca.txt") > "$D3_DIR/deriva-faltan.txt"
cut -c1-260 "$D3_DIR/deriva-faltan.txt" | head -"${D3_DERIVA_MAX:-80}"
echo "(total: $(wc -l < "$D3_DIR/deriva-faltan.txt"))"
echo "=== SOLO en la actualizada (sobra respecto del esquema actual) ==="
comm -23 <(sort "$D3_DIR/huella-actualizada.txt") <(sort "$D3_DIR/huella-fresca.txt") > "$D3_DIR/deriva-sobran.txt"
cut -c1-260 "$D3_DIR/deriva-sobran.txt" | head -"${D3_DERIVA_MAX:-80}"
echo "(total: $(wc -l < "$D3_DIR/deriva-sobran.txt"))"
cp "$D3_DIR/deriva-faltan.txt" "$EVID/deriva-faltan.txt"; cp "$D3_DIR/deriva-sobran.txt" "$EVID/deriva-sobran.txt"
pq postgres -c "DROP DATABASE IF EXISTS pasanaku_fresco" >/dev/null
