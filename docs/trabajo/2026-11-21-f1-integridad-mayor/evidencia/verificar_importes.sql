-- F1.S1.M3 · Importes de aporte, pago, reembolso y disputa nunca negativos (ANF-DINERO-001).
-- Cada caso corre con el rol de la aplicacion (svc_aportes) y se deshace siempre: un caso
-- aceptado se revierte lanzando P0099 a proposito. Niveles: invalido (negativo), limite (0 y
-- 0.01) y valido. Requiere una base con las semillas de desarrollo (pago, obligacion y orden).
\set QUIET on
CREATE TEMP TABLE caso(n serial, tabla text, columna text, valor text, esperado text, obtenido text);
GRANT ALL ON caso TO PUBLIC; GRANT ALL ON SEQUENCE caso_n_seq TO PUBLIC;
CREATE FUNCTION pg_temp.probar(p_tabla text, p_col text, p_valor text, p_esperado text, p_sql text) RETURNS void AS $f$
DECLARE v_c text;
BEGIN
  BEGIN
    SET LOCAL ROLE svc_aportes;
    EXECUTE p_sql;
    RAISE EXCEPTION USING ERRCODE = 'P0099';
  EXCEPTION
    WHEN check_violation THEN
      GET STACKED DIAGNOSTICS v_c = CONSTRAINT_NAME;
      RESET ROLE; INSERT INTO caso(tabla, columna, valor, esperado, obtenido) VALUES (p_tabla, p_col, p_valor, p_esperado, 'RECHAZADO ' || v_c);
      RETURN;
    WHEN SQLSTATE 'P0099' THEN
      RESET ROLE; INSERT INTO caso(tabla, columna, valor, esperado, obtenido) VALUES (p_tabla, p_col, p_valor, p_esperado, 'ACEPTADO');
      RETURN;
    WHEN OTHERS THEN
      RESET ROLE; INSERT INTO caso(tabla, columna, valor, esperado, obtenido) VALUES (p_tabla, p_col, p_valor, p_esperado, 'ERROR ' || SQLSTATE || ' ' || SQLERRM);
      RETURN;
  END;
END $f$ LANGUAGE plpgsql;

DO $$
DECLARE c RECORD; v TEXT; v_pago uuid; v_usuario uuid;
BEGIN
  -- UPDATE sobre una fila existente, columna por columna
  FOR c IN SELECT * FROM (VALUES
      ('obligacion_aporte', 'monto_pagado'), ('obligacion_aporte', 'monto_recargo'),
      ('obligacion_aporte', 'monto_condonado'), ('obligacion_aporte', 'monto_cubierto_garantia'),
      ('orden_cobro', 'monto_exacto'),
      ('pago', 'monto_comision_proveedor'), ('pago', 'monto_neto_acreditado')) AS x(tabla, col) LOOP
    FOREACH v IN ARRAY ARRAY['-0.01', '0.00', '0.01'] LOOP
      PERFORM pg_temp.probar(c.tabla, c.col, v, CASE WHEN v::numeric < 0 THEN 'RECHAZADO' ELSE 'ACEPTADO' END,
        format('UPDATE aportes.%I SET %I = %s WHERE id = (SELECT id FROM aportes.%I ORDER BY id LIMIT 1)',
               c.tabla, c.col, v, c.tabla));
    END LOOP;
  END LOOP;
  -- INSERT de un reembolso y de una disputa sobre un pago real de las semillas
  SELECT id INTO v_pago FROM aportes.pago ORDER BY id LIMIT 1;
  SELECT id INTO v_usuario FROM identidad.usuario ORDER BY id LIMIT 1;
  FOREACH v IN ARRAY ARRAY['-50.00', '-0.01', '0.00', '0.01', '100.00'] LOOP
    PERFORM pg_temp.probar('reembolso', 'monto', v, CASE WHEN v::numeric > 0 THEN 'ACEPTADO' ELSE 'RECHAZADO' END,
      format($q$INSERT INTO aportes.reembolso(pago_id, monto, motivo, estado, solicitado_por, fecha_solicitud)
                VALUES (%L, %s, 'DUPLICADO', 'SOLICITADO', %L, now())$q$, v_pago, v, v_usuario));
  END LOOP;
  FOREACH v IN ARRAY ARRAY['-0.01', '0.00', '10.00'] LOOP
    PERFORM pg_temp.probar('disputa_pago', 'monto_disputado', v, CASE WHEN v::numeric < 0 THEN 'RECHAZADO' ELSE 'ACEPTADO' END,
      format($q$INSERT INTO aportes.disputa_pago(pago_id, tipo, descripcion, monto_disputado, estado, evidencias, fecha_limite_respuesta)
                VALUES (%L, (SELECT substring(pg_get_constraintdef(oid) from '''([A-Z_]+)''') FROM pg_constraint WHERE conname = 'ck_disputa_pago_tipo'),
                        'caso sintetico', %s,
                        (SELECT substring(pg_get_constraintdef(oid) from '''([A-Z_]+)''') FROM pg_constraint WHERE conname = 'ck_disputa_pago_estado'),
                        '[]', now() + interval '5 days')$q$, v_pago, v));
  END LOOP;
END $$;
\set QUIET off
SELECT n, tabla, columna, valor, esperado, obtenido,
       CASE WHEN obtenido LIKE esperado || '%' THEN 'PASS' ELSE 'FAIL' END AS veredicto
  FROM caso ORDER BY n;
SELECT count(*) FILTER (WHERE obtenido LIKE esperado || '%') AS pass,
       count(*) FILTER (WHERE obtenido NOT LIKE esperado || '%') AS fail FROM caso;
