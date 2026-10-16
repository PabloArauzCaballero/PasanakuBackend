-- PRUEBA DE DERIVA PROPUESTA para SECURITY DEFINER en el esquema `aportes` (aun no es un gate del CI).
-- Describe el estado DESEADO, de modo que FALLA mientras 03_permisos.sql siga aplicando DEFINER a todo.
-- Salida: id | ok (t/f) | detalle. Si una sola es `f`, la deriva existe.
--
--   psql -tA -F'|' -v permitidas='fn_a,fn_b,...' -f deriva_security_definer.sql
--
-- `permitidas` es la lista EXPLICITA de funciones que justifican DEFINER (cruzan esquemas a proposito),
-- una por una y revisada por una persona; vive en el repo junto con la correccion de 03_permisos.sql.
-- Aca no se deriva de la base: una lista que se deduce del estado actual aprobaria cualquier estado.

WITH definer AS (
    SELECT p.oid, p.proname, p.proconfig
      FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
     WHERE n.nspname = 'aportes' AND p.prokind = 'f' AND p.prosecdef
), permitidas AS (
    SELECT unnest(string_to_array(:'permitidas', ',')) AS nombre
)
SELECT 'X1 toda funcion DEFINER esta en la lista explicita',
       NOT EXISTS (SELECT 1 FROM definer d WHERE d.proname NOT IN (SELECT nombre FROM permitidas)),
       (SELECT count(*) FROM definer d WHERE d.proname NOT IN (SELECT nombre FROM permitidas)) || ' DEFINER fuera de la lista'
UNION ALL SELECT 'X2 la lista no tiene funciones que ya no existen',
       NOT EXISTS (SELECT 1 FROM permitidas p WHERE p.nombre <> '' AND p.nombre NOT IN (SELECT proname FROM definer)),
       (SELECT count(*) FROM permitidas p WHERE p.nombre <> '' AND p.nombre NOT IN (SELECT proname FROM definer)) || ' sobrantes'
UNION ALL SELECT 'X3 ninguna DEFINER es ejecutable por PUBLIC (REVOKE ... FROM PUBLIC)',
       NOT EXISTS (SELECT 1 FROM definer d WHERE has_function_privilege('public', d.oid, 'EXECUTE')),
       (SELECT count(*) FROM definer d WHERE has_function_privilege('public', d.oid, 'EXECUTE')) || ' ejecutables por PUBLIC'
UNION ALL SELECT 'X4 toda DEFINER fija search_path, con pg_catalog y SIN public',
       NOT EXISTS (SELECT 1 FROM definer d
                    WHERE NOT EXISTS (SELECT 1 FROM unnest(coalesce(d.proconfig, '{}')) c
                                       WHERE c LIKE 'search_path=%' AND c ~ 'pg_catalog' AND c !~ '(^|[=, ])public([, ]|$)')),
       (SELECT count(*) FROM definer d
         WHERE NOT EXISTS (SELECT 1 FROM unnest(coalesce(d.proconfig, '{}')) c
                            WHERE c LIKE 'search_path=%' AND c ~ 'pg_catalog' AND c !~ '(^|[=, ])public([, ]|$)')) || ' sin search_path seguro'
UNION ALL SELECT 'X5 las fn_seg_* (politicas de fila) siguen siendo INVOKER: leen el contexto de quien consulta',
       NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
                    WHERE n.nspname = 'aportes' AND p.proname LIKE 'fn\_seg\_%' AND p.prosecdef),
       'fn_seg_* en INVOKER'
UNION ALL SELECT 'X6 ninguna DEFINER no-trigger es invocable por un rol de servicio (no se llama con SELECT)',
       NOT EXISTS (SELECT 1 FROM pg_proc p JOIN definer d ON d.oid = p.oid
                    WHERE p.prorettype <> 'trigger'::regtype AND has_function_privilege('svc_nucleo_financiero', p.oid, 'EXECUTE')),
       (SELECT count(*) FROM pg_proc p JOIN definer d ON d.oid = p.oid
         WHERE p.prorettype <> 'trigger'::regtype AND has_function_privilege('svc_nucleo_financiero', p.oid, 'EXECUTE'))
           || ' DEFINER invocables por svc_nucleo_financiero';
