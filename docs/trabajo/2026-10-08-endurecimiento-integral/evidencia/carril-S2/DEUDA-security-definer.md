# Deuda de seguridad · `SECURITY DEFINER` aplicado a todas las funciones de regla

Carril S2. Estado: **DIAGNÓSTICO + PROPUESTA + PRUEBA DE DERIVA PROPUESTA**. No se cambió `sql/00_base/03_permisos.sql`
(altera permisos ya desplegados; y `sql/` no se edita a mano: la fuente es `scripts/generar_ddl.py`/`scripts/modelo.py`).

## 1 · Diagnóstico

`sql/00_base/03_permisos.sql` (bloque `DO $busqueda$`, ~líneas 235-257) recorre **todas** las funciones del esquema
`aportes` (donde caen las funciones de regla porque `aportes` es el primero del `search_path` de `aplicar.sql`) y:

1. les fija `search_path` = **todos** los esquemas no `pg_*` en orden alfabético, **incluido `public`**, sin `pg_catalog` explícito;
2. las vuelve `SECURITY DEFINER` salvo las `fn_seg_*` (políticas de fila, que deben leer el contexto de quien consulta).

Medido sobre las fuentes que aplica `sql/aplicar.sql` (`despliegue/diagnosticos/definer_estatico.py`, salida literal en
`definer-estatico.log`; análisis estático, sin base):

| Medida | Valor |
|---|---|
| Funciones sin esquema (caen en `aportes`) | 71 |
| `fn_seg_*` (quedan INVOKER, bien) | 10 |
| **DEFINER** (todas las demás) | **61** |
| …de las cuales son funciones de trigger | 51 |
| …de las cuales son **invocables con `SELECT`** (no trigger) | **10**: `fn_bil_recalcular_saldos`, `fn_con_exigir_contrato`, `fn_ctb_recalcular_saldo`, `fn_lic_servicio_habilitado`, `fn_lim_evaluar`, `fn_not_puede_enviar`, `fn_uif_concepto`, `fn_uif_exigir_ddd`, `fn_uif_registrar_operacion`, `fn_uif_titular` |
| DEFINER cuyo cuerpo toca tablas de **más de un esquema** (única razón que lo justifica) | **11** |
| DEFINER sin cruce aparente (candidatas a INVOKER) | 50 |

### Por qué importa

- Una función `DEFINER` corre con los privilegios de su dueño (el superusuario de arranque o el dueño del esquema). Si además PostgreSQL
  la deja ejecutable por `PUBLIC` (el valor por omisión para funciones), **cualquiera de los 15 roles `svc_*` puede llamarla con
  `SELECT fn_uif_registrar_operacion(...)`** saltándose la frontera entre servicios (invariante 11) y los `GRANT` de tabla. Las 10
  invocables son las que importan: `fn_uif_registrar_operacion` escribe en el esquema de cumplimiento; `fn_ctb_recalcular_saldo`
  reescribe saldos del mayor.
- `search_path` con `public` y sin `pg_catalog` primero es la receta clásica de secuestro de `search_path` en una función `DEFINER`
  si algún rol puede crear objetos en un esquema que aparece antes. (Hoy `public` no admite `CREATE` a `PUBLIC` desde PostgreSQL 15;
  es defensa en profundidad, no un agujero confirmado.)
- **No se ejercitó el abuso** contra una base (el diagnóstico runtime está escrito y se corre con
  `despliegue/diagnosticos/diagnostico_definer.sh`; ver «Estado de la evidencia»).

## 2 · Propuesta de corrección (verificable)

1. **Lista explícita**, revisada una por una por una persona (`despliegue/diagnosticos/definer_permitidas.propuesta.txt`, 11
   candidatas por cruce de esquemas). Todo lo demás pasa a `SECURITY INVOKER`.
2. **`REVOKE EXECUTE … FROM PUBLIC`** sobre toda función `DEFINER` y `GRANT EXECUTE` solo a quien la necesita (los triggers no
   necesitan `EXECUTE` del rol que dispara; las 10 invocables, solo si algún servicio las llama por SQL — hoy ninguna lo hace por
   contrato).
3. **`search_path` fijo y mínimo**: `pg_catalog, <esquemas que el cuerpo usa>, pg_temp` (con `pg_temp` al final), sin `public`.
4. Aplicarlo en la **fuente** (`scripts/generar_ddl.py`) y regenerar `sql/`, no editando `03_permisos.sql`.
5. **Migración de lo desplegado** (aditiva y reversible): `ALTER FUNCTION … SECURITY INVOKER` para las 50 no listadas;
   `REVOKE EXECUTE … FROM PUBLIC` para las listadas; `ALTER FUNCTION … SET search_path` nuevo. Reversa: el bucle actual.
6. **Riesgo a vigilar** al pasar a INVOKER: un trigger INVOKER corre con los privilegios de quien hizo el DML; si su cuerpo toca una tabla
   que ese rol no puede leer, falla con «permission denied». Por eso la lista sale del cruce de esquemas y por eso la suite de
   integración **de los 15 servicios** es la prueba de aceptación del cambio (cada `integrationTest` ejercita los triggers).

## 3 · Prueba de deriva propuesta

`despliegue/diagnosticos/deriva_security_definer.sql` (aún no es un gate del CI; describe el estado deseado y **hoy debe fallar**):

| Id | Comprueba |
|---|---|
| X1 | toda función `DEFINER` de `aportes` está en la lista explícita |
| X2 | la lista no tiene funciones que ya no existen |
| X3 | ninguna `DEFINER` es ejecutable por `PUBLIC` |
| X4 | toda `DEFINER` fija `search_path`, con `pg_catalog` y **sin** `public` |
| X5 | las `fn_seg_*` siguen siendo INVOKER |
| X6 | ninguna `DEFINER` no-trigger es invocable por un rol de servicio |

Se corre con `psql -tA -F'|' -v permitidas='fn_a,fn_b,…' -f deriva_security_definer.sql` y sale una fila por comprobación. Para
volverlo gate: envolverlo como `*RepositorioTest`/`Aislamiento*Test` (Testcontainers) en `plataforma/comun-pruebas`, con la lista leída
del archivo versionado, una vez adoptada la corrección.

## 4 · Estado de la evidencia

| Pieza | Estado |
|---|---|
| Análisis estático (61 DEFINER, 10 invocables, 11 con cruce) | HECHO — `definer-estatico.log` (peldaño RUNS: el script corre; es heurística por palabra, no prueba) |
| Diagnóstico y deriva contra una base real | ver `definer-corrida.log` si existe; si no, **BLOQUEADO por RAM** (el guardián no dio margen para Docker) |
| Corrección en `generar_ddl.py` y `sql/` | **NO hecha** (decisión del dueño del esquema; cambia permisos desplegados) |
