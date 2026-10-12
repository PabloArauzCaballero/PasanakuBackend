#!/usr/bin/env bash
# Esquema ADITIVO sobre base poblada: aplica el sql/ de HEAD (anterior), siembra un pago legado sin clave,
# corre despliegue/aplicar-esquema.sh con el sql/ nuevo y verifica columna, indice y datos. Postgres desechable 5455.
REPO="C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-cierre"
EV="$REPO/evidencia/carril-R"
S="C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-Pasanaku/c468f89e-ca7f-4172-b7d5-74080b60e5a8/scratchpad"
OLD="$S/r_sql_old"
cd "$REPO" || exit 2
rm -rf "$OLD" && mkdir -p "$OLD" && git archive HEAD sql | tar -x -C "$OLD"
INICIE_DOCKER=0
if ! docker info >/dev/null 2>&1; then
  INICIE_DOCKER=1
  ("/c/Program Files/Docker/Docker/Docker Desktop.exe" >/dev/null 2>&1 &)
  for i in $(seq 1 60); do docker info >/dev/null 2>&1 && break; sleep 5; done
  docker info >/dev/null 2>&1 || { echo "Docker no levanto"; exit 3; }
fi
CT=aportaya-pg-r-up
docker rm -f $CT >/dev/null 2>&1
export MSYS_NO_PATHCONV=1
docker run -d --name $CT -e POSTGRES_DB=pasanaku -e POSTGRES_USER=pasanaku -e POSTGRES_PASSWORD=pasanaku \
  -e POSTGRES_INITDB_ARGS="--encoding=UTF8 --locale=C" -p 127.0.0.1:5455:5432 \
  -v "$REPO/sql:/sql:ro" -v "$OLD/sql:/sqlold:ro" -v "$REPO/despliegue/aplicar-esquema.sh:/aplicar-esquema.sh:ro" \
  -v "$REPO/despliegue/compose/init:/docker-entrypoint-initdb.d:ro" \
  postgres:16 -c max_connections=100 -c shared_buffers=128MB >/dev/null || { echo "no pude crear $CT"; exit 4; }
for i in $(seq 1 60); do docker exec $CT pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && sleep 3 && docker exec $CT pg_isready -U pasanaku -d pasanaku >/dev/null 2>&1 && break; sleep 3; done
P="docker exec -i $CT psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku"
echo "== 1. esquema ANTERIOR (HEAD) aplicado"
$P -q -f /sqlold/aplicar.sql >/dev/null 2>"$EV/upgrade-1-anterior.err"; echo "RC=$?"; tail -2 "$EV/upgrade-1-anterior.err"
echo "== 2. columna ausente en la base anterior:"
$P -tAc "select count(*) from information_schema.columns where table_schema='erp' and table_name='pago_a_proveedor' and column_name='clave_idempotencia'"
echo "== 3. se siembra un pago LEGADO (sin clave) con FK/triggers apagados (replica), como los que ya hay en TEST"
$P -q <<'SQL'
SET session_replication_role = replica;
INSERT INTO erp.pago_a_proveedor (id, factura_proveedor_id, monto, moneda, fecha_pago, forma_pago, autorizado_por)
VALUES ('a0000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-0000000000f1', 100, 'BOB', now(), 'TRANSFERENCIA','a0000000-0000-4000-8000-0000000000b1'),
       ('a0000000-0000-4000-8000-000000000002','a0000000-0000-4000-8000-0000000000f1', 50, 'BOB', now(), 'EFECTIVO','a0000000-0000-4000-8000-0000000000b1');
SQL
$P -tAc "select 'pagos legados: ' || count(*) from erp.pago_a_proveedor"
echo "== 4. despliegue/aplicar-esquema.sh con el sql NUEVO sobre esa base poblada (primera corrida)"
docker exec -e PGHOST=localhost -e PGUSER=pasanaku -e PGPASSWORD=pasanaku -e PGDATABASE=pasanaku $CT bash /aplicar-esquema.sh > "$EV/upgrade-2-aplicar-esquema-1.log" 2>&1; echo "RC=$?"
grep -E "expandidas|clave_idempotencia|listo|ERROR|tablas" "$EV/upgrade-2-aplicar-esquema-1.log"
echo "== 5. verificacion"
$P -tAc "select 'columna: ' || data_type || ' nullable=' || is_nullable from information_schema.columns where table_schema='erp' and table_name='pago_a_proveedor' and column_name='clave_idempotencia'"
$P -tAc "select 'indice: ' || indexdef from pg_indexes where indexname='uq_pago_a_proveedor_clave'"
$P -tAc "select 'pagos legados intactos: ' || count(*) || ', con clave NULL: ' || count(*) filter (where clave_idempotencia is null) from erp.pago_a_proveedor"
echo "== 6. segunda corrida (idempotente: no agrega nada)"
docker exec -e PGHOST=localhost -e PGUSER=pasanaku -e PGPASSWORD=pasanaku -e PGDATABASE=pasanaku $CT bash /aplicar-esquema.sh > "$EV/upgrade-3-aplicar-esquema-2.log" 2>&1; echo "RC=$?"
grep -E "expandidas|listo|ERROR" "$EV/upgrade-3-aplicar-esquema-2.log"
echo "== 7. el UNIQUE corta el duplicado y no estorba a los NULL"
$P -tAc "select 'tras 2a corrida, indice: ' || count(*) from pg_indexes where indexname='uq_pago_a_proveedor_clave'"
$P -q <<'SQL' 2>&1 | sed 's/^/  /'
SET session_replication_role = replica;
INSERT INTO erp.pago_a_proveedor (factura_proveedor_id, monto, moneda, fecha_pago, forma_pago, autorizado_por, clave_idempotencia)
VALUES ('a0000000-0000-4000-8000-0000000000f1', 10, 'BOB', now(), 'CHEQUE','a0000000-0000-4000-8000-0000000000b1','c0000000-0000-4000-8000-000000000001');
INSERT INTO erp.pago_a_proveedor (factura_proveedor_id, monto, moneda, fecha_pago, forma_pago, autorizado_por, clave_idempotencia)
VALUES ('a0000000-0000-4000-8000-0000000000f2', 10, 'BOB', now(), 'CHEQUE','a0000000-0000-4000-8000-0000000000b1','c0000000-0000-4000-8000-000000000001');
INSERT INTO erp.pago_a_proveedor (factura_proveedor_id, monto, moneda, fecha_pago, forma_pago, autorizado_por, clave_idempotencia)
VALUES ('a0000000-0000-4000-8000-0000000000f1', 10, 'BOB', now(), 'CHEQUE','a0000000-0000-4000-8000-0000000000b1','c0000000-0000-4000-8000-000000000001');
SQL
echo "(arriba: el 1.o y 2.o INSERT entran —misma clave en OTRA factura—; el 3.o debe fallar con uq_pago_a_proveedor_clave)"
docker rm -f $CT >/dev/null 2>&1
if [ "$INICIE_DOCKER" = 1 ]; then docker desktop stop >/dev/null 2>&1 || taskkill //IM "Docker Desktop.exe" //F >/dev/null 2>&1; fi
echo "== fin"
