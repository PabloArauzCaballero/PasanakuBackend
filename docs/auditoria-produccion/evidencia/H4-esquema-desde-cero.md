# H4.S2.M1/M2 — esquema desde cero y re-aplicación con datos

> Corrido contra un contenedor PostgreSQL 16 **aislado y descartable**
> (`marcelo-h4-esquema-desde-cero`, puerto `55432`, creado y destruido solo para
> esta verificación), NUNCA contra `aportaya-postgres` (el compose compartido por
> los demás carriles) ni contra ningún dato real. `sql/` se copió al contenedor
> con `docker cp` para que los `\ir` relativos de `aplicar.sql`/`sembrar.sql`/
> `sembrar_dev.sql` resuelvan igual que en un `psql -f` local.

## H4.S2.M1 — `empty → latest`

```text
$ docker exec ... psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku -f aplicar.sql
...
COMMIT
EXIT=0

$ psql -t -c "SELECT count(*) FROM information_schema.tables
              WHERE table_schema NOT IN ('pg_catalog','information_schema')
                AND table_type='BASE TABLE'"
 401
```

**401 tablas** sobre una base vacía, `EXIT=0`. (El plan madre original citaba
"305"/"307" tablas — ese número es de una versión anterior del esquema; 401 es
el conteo REAL de hoy, con todo lo que los demás carriles fueron agregando
desde entonces. Se reporta el número real, no el del plan.)

## H4.S2.M2 — `latest → latest` con datos (AMB-7: re-aplicar no borra filas)

Procedimiento, sobre la misma base ya con el esquema de M1:

1. `ALTER DATABASE pasanaku SET app.entorno = 'dev';` (para destrabar la
   GUARDA 1 de `sembrar_dev.sql`, que exige `app.entorno = 'dev'` o aborta con
   `RAISE EXCEPTION` — la misma guarda que pide H4.S2.M4).
2. `psql -f 60_semillas/sembrar.sql` → `EXIT=0` (catálogos mínimos: plan de
   cuentas, políticas, límites, tarifario, impuestos, umbrales UIF, etc.)
3. `psql -f 61_dev/sembrar_dev.sql` → `EXIT=0` (usuarios, billeteras, grupo
   demo, aportes, cobros QR — datos sintéticos completos de desarrollo).
4. Una fila sintética propia, insertada a mano (no parte de ningún seeder):
   `identidad.usuario` con `codigo_publico = 'H4SINT0001'`.
5. Snapshot EXACTO (no `n_live_tup` estimado — `SELECT count(*)` real, tabla
   por tabla, las 401) → **1543 filas totales**, guardado en
   `conteo-exacto-antes.csv`.
6. Re-aplicar `aplicar.sql` completo (el mismo DDL de M1) sobre la base YA
   poblada → `EXIT=0`.
7. Mismo snapshot exacto → **1543 filas totales**, `conteo-exacto-despues.csv`.
8. `diff conteo-exacto-antes.csv conteo-exacto-despues.csv` → **sin
   diferencias, byte a byte** (incluye la fila sintética `H4SINT0001`,
   confirmada explícitamente después del re-aplicar).

```text
$ diff conteo-exacto-antes.csv conteo-exacto-despues.csv; echo $?
0

$ psql -t -c "SELECT codigo_publico, nombres FROM identidad.usuario
              WHERE codigo_publico = 'H4SINT0001'"
 H4SINT0001     | Fila
```

**Conclusión, con evidencia y no por diseño supuesto**: `aplicar.sql` es
verdaderamente aditivo/idempotente sobre datos ya existentes — cero pérdida de
filas al re-aplicar el DDL completo, en las 401 tablas del esquema, con datos
reales de los tres niveles (catálogos, semillas dev, y una fila insertada por
fuera de cualquier seeder). Esto confirma en la práctica la política Q-01/AMB-7
ya decidida (`docs/operacion/schema-changes.md`): migraciones aditivas, sin
Flyway/Liquibase este turno.

## H4.S2.M4 — la guarda de semillas dev, probada en los dos sentidos

No alcanza con citar el paso de `ci.yml`: se probó la guarda misma, en un
segundo contenedor descartable (`marcelo-h4-guarda-check`), con el esquema ya
aplicado pero SIN el `ALTER DATABASE ... SET app.entorno = 'dev'`:

```text
$ psql -v ON_ERROR_STOP=1 -f sembrar_dev.sql; echo EXIT=$?
SET
BEGIN
psql:sembrar_dev.sql:24: ERROR:  SEMILLAS DE DEV BLOQUEADAS: app.entorno = <sin definir>, se exige 'dev'
CONTEXT:  PL/pgSQL function inline_code_block line 4 at RAISE
EXIT=3

$ psql -t -c "SELECT count(*) FROM identidad.usuario"
 0
```

La guarda rechaza el intento con el mensaje exacto y `EXIT=3` (no `0`), y no
inserta ni una fila (`count(*) = 0`). Con `app.entorno = 'dev'` puesto (ver
H4.S2.M2 arriba), el mismo archivo sí siembra. Los dos caminos, probados de
verdad — no solo el camino feliz.

## Limpieza

Ambos contenedores (`marcelo-h4-esquema-desde-cero`,
`marcelo-h4-guarda-check`) se destruyeron al terminar cada verificación
(`docker rm -f`); no queda ningún recurso residual ni dato de prueba en ningún
entorno compartido.
