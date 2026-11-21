-- F1.S1.M4a · Unicidad real de clave_idempotencia (regla 91.2.4).
-- (1) Inventario: toda columna clave_idempotencia esta cubierta por un indice UNIQUE con
--     nombre explicito (simple o compuesto). Esperado: 0 filas "SIN UNIQUE".
-- (2) Demostracion: en las tablas de operacion de dinero con filas en las semillas, se duplica
--     una fila existente con el rol de la aplicacion y se espera unique_violation (23505).
--     Cada intento se deshace siempre.
\set QUIET on
\echo '--- (1) columnas clave_idempotencia y su indice UNIQUE'
SELECT c.table_schema || '.' || c.table_name AS tabla,
       COALESCE((SELECT string_agg(ic.relname || ' (' || pg_get_indexdef(i.indexrelid) ~ ',' || ')', ', ')
                   FROM pg_index i JOIN pg_class ic ON ic.oid = i.indexrelid
                  WHERE i.indrelid = format('%I.%I', c.table_schema, c.table_name)::regclass
                    AND i.indisunique
                    AND EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid = i.indrelid
                                   AND a.attnum = ANY (i.indkey) AND a.attname = 'clave_idempotencia')),
                'SIN UNIQUE') AS indice_unique_compuesto
  FROM information_schema.columns c
  JOIN pg_class t ON t.relname = c.table_name
  JOIN pg_namespace n ON n.oid = t.relnamespace AND n.nspname = c.table_schema
 WHERE c.column_name = 'clave_idempotencia' AND NOT t.relispartition
 ORDER BY 1;
\echo '--- (1b) esperado 0'
SELECT count(*) AS columnas_sin_unique
  FROM information_schema.columns c
 WHERE c.column_name = 'clave_idempotencia'
   AND NOT EXISTS (SELECT 1 FROM pg_index i
                    WHERE i.indrelid = format('%I.%I', c.table_schema, c.table_name)::regclass
                      AND i.indisunique
                      AND EXISTS (SELECT 1 FROM pg_attribute a WHERE a.attrelid = i.indrelid
                                     AND a.attnum = ANY (i.indkey) AND a.attname = 'clave_idempotencia'));

CREATE TEMP TABLE caso(tabla text, filas_con_clave int, obtenido text);
GRANT ALL ON caso TO PUBLIC;
DO $$
DECLARE t RECORD; v_cols text; v_sel text; v_n int; v_c text; v_filas int; v_titular uuid;
BEGIN
  FOR t IN SELECT * FROM (VALUES
      ('aportes', 'pago'), ('aportes', 'orden_cobro'), ('aportes', 'intento_pago'), ('aportes', 'webhook_pasarela'),
      ('nucleo_financiero', 'transaccion_billetera'), ('nucleo_financiero', 'orden_recarga'),
      ('nucleo_financiero', 'orden_retiro'), ('entregas', 'fondeo_entrega'), ('entregas', 'orden_desembolso'),
      ('tarifas', 'devengo_comision'), ('tarifas', 'cotizacion_comision'), ('erp', 'pago_a_proveedor')) AS x(esq, tab) LOOP
    EXECUTE format('SELECT count(*) FROM %I.%I WHERE clave_idempotencia IS NOT NULL', t.esq, t.tab) INTO v_n;
    IF v_n = 0 THEN
      INSERT INTO caso VALUES (t.esq || '.' || t.tab, 0, 'SIN FILAS CON CLAVE EN LAS SEMILLAS: no se ejercita');
      CONTINUE;
    END IF;
    -- La fila de origen se lee como superusuario (las politicas RLS de billetera la ocultan
    -- sin contexto de sesion). Se copia entera salvo: la PK, y toda columna que integre OTRO
    -- indice UNIQUE que no contenga clave_idempotencia, que recibe un valor nuevo. Asi el unico
    -- choque posible es el de la clave de idempotencia.
    EXECUTE 'DROP TABLE IF EXISTS pg_temp.origen';
    EXECUTE format('CREATE TEMP TABLE origen AS SELECT * FROM %I.%I WHERE clave_idempotencia IS NOT NULL ORDER BY clave_idempotencia LIMIT 1', t.esq, t.tab);
    EXECUTE 'GRANT SELECT ON pg_temp.origen TO PUBLIC';
    SELECT string_agg(quote_ident(a.attname), ', ' ORDER BY a.attnum),
           string_agg(CASE
             WHEN a.attname = 'id' OR (EXISTS (SELECT 1 FROM pg_index i WHERE i.indrelid = a.attrelid AND i.indisunique
                                              AND a.attnum = ANY (i.indkey)
                                              AND NOT EXISTS (SELECT 1 FROM pg_attribute k WHERE k.attrelid = i.indrelid
                                                                 AND k.attnum = ANY (i.indkey) AND k.attname = 'clave_idempotencia'))
                                  -- las columnas del propio indice de la clave se copian tal cual
                                  AND NOT EXISTS (SELECT 1 FROM pg_index j WHERE j.indrelid = a.attrelid AND j.indisunique
                                                     AND a.attnum = ANY (j.indkey)
                                                     AND EXISTS (SELECT 1 FROM pg_attribute k WHERE k.attrelid = j.indrelid
                                                                    AND k.attnum = ANY (j.indkey) AND k.attname = 'clave_idempotencia')))
             THEN CASE
                    WHEN format_type(a.atttypid, NULL) = 'uuid' THEN 'gen_random_uuid()'
                    WHEN format_type(a.atttypid, NULL) IN ('bigint', 'integer', 'smallint')
                      THEN format('(SELECT COALESCE(max(%I), 0) + 1 FROM %I.%I)', a.attname, t.esq, t.tab)
                    -- texto: solo si la columna no tiene un CHECK propio (enumerado), que rechazaria el valor nuevo
                    WHEN EXISTS (SELECT 1 FROM pg_constraint co WHERE co.conrelid = a.attrelid AND co.contype = 'c'
                                    AND co.conkey = ARRAY[a.attnum]) THEN quote_ident(a.attname)
                    ELSE format('left(md5(random()::text), %s)', GREATEST(LEAST(a.atttypmod - 4, 32), 8))
                  END
             ELSE quote_ident(a.attname) END, ', ' ORDER BY a.attnum)
      INTO v_cols, v_sel
      FROM pg_attribute a WHERE a.attrelid = format('%I.%I', t.esq, t.tab)::regclass
       AND a.attnum > 0 AND NOT a.attisdropped AND a.attgenerated = '' AND a.attidentity = '';
    -- contexto de sesion del titular, para que la politica RLS deje insertar (billetera)
    v_titular := NULL;
    IF t.tab IN ('orden_recarga', 'orden_retiro') THEN
      EXECUTE 'SELECT c.usuario_id FROM nucleo_financiero.cuenta_billetera c JOIN pg_temp.origen o ON o.cuenta_billetera_id = c.id' INTO v_titular;
      PERFORM set_config('app.usuario_id', v_titular::text, true);
    END IF;
    BEGIN
      EXECUTE format('SET LOCAL ROLE %I', 'svc_' || t.esq);
      EXECUTE format('INSERT INTO %I.%I (%s) SELECT %s FROM pg_temp.origen', t.esq, t.tab, v_cols, v_sel);
      GET DIAGNOSTICS v_filas = ROW_COUNT;
      RAISE EXCEPTION USING ERRCODE = 'P0099', MESSAGE = v_filas::text;
    EXCEPTION
      WHEN unique_violation THEN
        GET STACKED DIAGNOSTICS v_c = CONSTRAINT_NAME;
        RESET ROLE; INSERT INTO caso VALUES (t.esq || '.' || t.tab, v_n, 'RECHAZADO 23505 ' || v_c);
      WHEN SQLSTATE 'P0099' THEN
        GET STACKED DIAGNOSTICS v_c = MESSAGE_TEXT;
        RESET ROLE; INSERT INTO caso VALUES (t.esq || '.' || t.tab, v_n, 'ACEPTADO: entraron ' || v_c || ' filas con la clave repetida');
      WHEN OTHERS THEN
        RESET ROLE; INSERT INTO caso VALUES (t.esq || '.' || t.tab, v_n, 'OTRO ERROR ' || SQLSTATE || ' ' || SQLERRM);
    END;
    PERFORM set_config('app.usuario_id', '', true);
  END LOOP;
END $$;
\set QUIET off
\echo '--- (2) segundo INSERT con la misma clave, con el rol de la aplicacion (esperado: RECHAZADO 23505 por el indice de la clave)'
SELECT * FROM caso ORDER BY 1;
