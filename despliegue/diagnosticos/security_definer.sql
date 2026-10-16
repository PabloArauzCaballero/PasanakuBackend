-- Diagnostico de SECURITY DEFINER sobre el esquema `aportes` (deuda de seguridad de sql/00_base/03_permisos.sql).
-- Solo lectura. Uso:  psql -tA -F'|' -f security_definer.sql
--
-- El bucle de 03_permisos.sql recorre TODAS las funciones del esquema `aportes` (donde viven
-- las funciones de regla) y les aplica `SECURITY DEFINER`, salvo las fn_seg_*. Eso es mas de lo
-- que hace falta: una funcion DEFINER corre con los privilegios de su duenio, y si ademas es
-- ejecutable por PUBLIC cualquier rol de la base (cada svc_*) puede invocarla con SELECT.
--
-- Cada fila: seccion | clave | valor

-- 1 · Totales
SELECT 'D1', 'funciones_en_aportes', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'aportes' AND p.prokind = 'f';
SELECT 'D1', 'security_definer', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef;
SELECT 'D1', 'definer_que_son_trigger', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef AND p.prorettype = 'trigger'::regtype;
SELECT 'D1', 'definer_invocables_por_SQL_(no_trigger)', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef AND p.prorettype <> 'trigger'::regtype;

-- 2 · El riesgo concreto: DEFINER y ejecutable por PUBLIC (cualquier rol de servicio la llama)
SELECT 'D2', 'definer_ejecutable_por_PUBLIC', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
   AND has_function_privilege('public', p.oid, 'EXECUTE');
SELECT 'D2', 'invocable_por_svc_nucleo_financiero_(no_trigger)', p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')'
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef AND p.prorettype <> 'trigger'::regtype
   AND has_function_privilege('svc_nucleo_financiero', p.oid, 'EXECUTE')
 ORDER BY p.proname;

-- 3 · search_path
SELECT 'D3', 'definer_sin_search_path_fijo', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
   AND NOT EXISTS (SELECT 1 FROM unnest(coalesce(p.proconfig, '{}')) c WHERE c LIKE 'search_path=%');
SELECT 'D3', 'definer_con_public_o_sin_pg_catalog_en_search_path', count(*)
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
   AND EXISTS (SELECT 1 FROM unnest(coalesce(p.proconfig, '{}')) c
                WHERE c LIKE 'search_path=%' AND (c ~ '(^|[=, ])public([, ]|$)' OR c !~ 'pg_catalog'));

-- 4 · Lista explicita de las DEFINER, para decidir una por una (nombre | es_trigger | ejecutable_por_PUBLIC)
SELECT 'D4', p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')',
       (p.prorettype = 'trigger'::regtype)::text || '|' || has_function_privilege('public', p.oid, 'EXECUTE')::text
  FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
 ORDER BY p.proname;

-- 5 · Propuesta basada en medicion: una DEFINER solo se justifica si su cuerpo toca tablas de un esquema
--     distinto del de la tabla que la dispara (el rol que dispara no tiene acceso: invariante 11).
--     Las que no cruzan esquema no necesitan DEFINER. Los nombres de tabla son unicos (307), y la busqueda es
--     por palabra en el cuerpo: es una lista de CANDIDATAS para revision humana, no una sentencia.
--     Columnas: seccion | funcion | es_trigger | dispara_en | referencia | cruza_esquema
WITH f AS (
    SELECT p.oid, p.proname, p.prosrc, (p.prorettype = 'trigger'::regtype) AS es_trigger
      FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
     WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
), disparo AS (
    SELECT t.tgfoid AS oid, array_agg(DISTINCT ns.nspname ORDER BY ns.nspname) AS esquemas
      FROM pg_trigger t JOIN pg_class c ON c.oid = t.tgrelid JOIN pg_namespace ns ON ns.oid = c.relnamespace
     WHERE NOT t.tgisinternal GROUP BY t.tgfoid
), referencias AS (
    SELECT f.oid, array_agg(DISTINCT ns.nspname ORDER BY ns.nspname) AS esquemas
      FROM f JOIN pg_class c ON c.relkind IN ('r', 'p')
             JOIN pg_namespace ns ON ns.oid = c.relnamespace
     WHERE ns.nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast') AND ns.nspname NOT LIKE 'pg\_%'
       AND f.prosrc ~* ('\m' || c.relname || '\M')
     GROUP BY f.oid
)
SELECT 'D5', f.proname, f.es_trigger, coalesce(array_to_string(d.esquemas, ','), '-'),
       coalesce(array_to_string(r.esquemas, ','), '-'),
       EXISTS (SELECT 1 FROM unnest(coalesce(r.esquemas, '{}')) e WHERE e <> ALL (coalesce(d.esquemas, '{}')))
  FROM f LEFT JOIN disparo d ON d.oid = f.oid LEFT JOIN referencias r ON r.oid = f.oid
 ORDER BY 6 DESC, f.proname;
