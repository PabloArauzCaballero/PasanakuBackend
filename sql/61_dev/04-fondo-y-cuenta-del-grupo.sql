-- Fondo de garantía del grupo demo.
-- GENERADO desde seeders/dev/04-fondo-y-cuenta-del-grupo.json — no editar a mano.

-- SINTETICO y SOLO DEV: valores sin procedencia que antes sembraba minimos/02 (regla 97.4.1, F3.L2.M14). Existe solo porque fondo_garantia.politica_cobertura_id es NOT NULL; se va con la contraccion de politica_cobertura (DECISION_REQUIRED de Pablo). Ningun codigo lee sus umbrales: la cobertura manual se retiro (F1.L2.M02).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM politica_cobertura) THEN
  INSERT INTO politica_cobertura (porcentaje_constitucion, dias_mora_para_activar, porcentaje_maximo_cobertura_por_aporte, tope_cobertura_por_participante, tope_cobertura_por_periodo, max_coberturas_por_participante, requiere_aprobacion_manual_desde, plazo_recuperacion_dias, tasa_recargo_recuperacion, vigente_desde, exige_aval_previo) VALUES
    (5.0, 5, 100.0, 3000.0, 10000.0, 3, 2000.0, 90, 1.5, now(), FALSE);
  END IF;
END $$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO fondo_garantia (ambito, grupo_id, politica_cobertura_id, cuenta_contable_id, moneda, saldo_disponible, saldo_comprometido, total_aportado, total_cubierto, total_recuperado, estado, total_castigado, version) VALUES
      ('POR_GRUPO', (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01'), (SELECT id FROM politica_cobertura LIMIT 1), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 'BOB', 0, 0, 0, 0, 0, 'ACTIVO', 0, 0)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La bolsa del grupo tiene cuenta propia: el titular es el grupo, nunca el organizador (R-GRP-04)
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cuenta_billetera (numero_cuenta, tipo, grupo_id, moneda, estado, nivel_debida_diligencia, fecha_apertura, cuenta_contable_id) VALUES
      ('GRP-0000001', 'GRUPO', (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01'), 'BOB', 'ACTIVA', 'ESTANDAR', now(), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'))
    ON CONFLICT (numero_cuenta) DO NOTHING;
  END IF;
END $siembra$;
