-- F1.S1.M2 · Recorre TODA tabla sellada como append-only (trigger *_append_only) e intenta
-- UPDATE y DELETE con cada rol de aplicacion que puede escribir en ella:
--   * el svc_* dueño del esquema (tablas propias), y
--   * todos los svc_* (tablas compartidas de `comun`, donde todos insertan).
-- Se prueba con SET ROLE desde el superusuario local: los privilegios efectivos son los
-- del rol de la aplicacion (application.yml de cada servicio conecta como svc_*).
-- El UPDATE/DELETE va con WHERE false: el chequeo de permisos ocurre antes de ejecutar, asi
-- que no hace falta ninguna fila ni se modifica nada. Resultado esperado: 0 filas PERMITIDO.
\set QUIET on
CREATE TEMP TABLE resultado(tabla text, rol text, op text, resultado text);
GRANT ALL ON resultado TO PUBLIC;
DO $$
DECLARE t RECORD; r TEXT; op TEXT; roles TEXT[];
BEGIN
  FOR t IN SELECT DISTINCT n.nspname AS esquema, c.relname AS tabla
             FROM pg_trigger g JOIN pg_class c ON c.oid = g.tgrelid
             JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE g.tgname LIKE 'tg\_%\_append\_only' AND NOT g.tgisinternal
              -- incluye las particiones: tambien se pueden direccionar directamente
            ORDER BY 1, 2 LOOP
    IF t.esquema = 'comun' THEN
      SELECT array_agg(rolname ORDER BY rolname) INTO roles FROM pg_roles WHERE rolname LIKE 'svc\_%';
    ELSE
      roles := ARRAY['svc_' || t.esquema];
    END IF;
    FOREACH r IN ARRAY roles LOOP
      FOREACH op IN ARRAY ARRAY['UPDATE', 'DELETE'] LOOP
        BEGIN
          EXECUTE format('SET LOCAL ROLE %I', r);
          IF op = 'UPDATE' THEN
            EXECUTE format('UPDATE %I.%I SET %I = %I WHERE false', t.esquema, t.tabla,
                           (SELECT attname FROM pg_attribute WHERE attrelid = format('%I.%I', t.esquema, t.tabla)::regclass AND attnum = 1),
                           (SELECT attname FROM pg_attribute WHERE attrelid = format('%I.%I', t.esquema, t.tabla)::regclass AND attnum = 1));
          ELSE
            EXECUTE format('DELETE FROM %I.%I WHERE false', t.esquema, t.tabla);
          END IF;
          INSERT INTO resultado VALUES (t.esquema || '.' || t.tabla, r, op, 'PERMITIDO');
        EXCEPTION WHEN insufficient_privilege THEN
          INSERT INTO resultado VALUES (t.esquema || '.' || t.tabla, r, op, 'permission denied');
        END;
        RESET ROLE;
      END LOOP;
    END LOOP;
  END LOOP;
END $$;
\set QUIET off
\echo '--- resumen por resultado (esperado: solo permission denied)'
SELECT resultado, op, count(*) AS intentos, count(DISTINCT tabla) AS tablas FROM resultado GROUP BY 1, 2 ORDER BY 1, 2;
\echo '--- combinaciones PERMITIDAS (esperado: 0 filas)'
SELECT * FROM resultado WHERE resultado <> 'permission denied' ORDER BY 1, 2, 3;
\echo '--- detalle por tabla (rol dueño)'
SELECT tabla, string_agg(DISTINCT rol, ',') FILTER (WHERE tabla NOT LIKE 'comun.%') AS rol,
       string_agg(DISTINCT op || '=' || resultado, ' · ') AS resultado
  FROM resultado GROUP BY tabla ORDER BY tabla;
\echo '--- regla 91.1.3: categorias que deben estar selladas (esperado: todas true)'
SELECT cat, tabla, EXISTS (SELECT 1 FROM resultado WHERE resultado.tabla = x.tabla) AS sellada
  FROM (VALUES ('asiento', 'nucleo_financiero.asiento_contable'),
               ('movimiento contable', 'nucleo_financiero.movimiento_contable'),
               ('bitacora', 'comun.bitacora_evento'),
               ('evento de reputacion', 'transparencia.evento_reputacion'),
               ('movimiento de fondo', 'garantia.movimiento_fondo'),
               ('abono de recuperacion', 'garantia.abono_recuperacion')) AS x(cat, tabla);
