-- Políticas operativas: billetera, redondeo y mora.
-- GENERADO desde seeders/minimos/02-politicas.json — no editar a mano.

INSERT INTO politica_billetera (codigo, moneda, dias_inactividad_para_limitar, permite_transferencia_p2p, requiere_mfa_desde, ventana_enfriamiento_retiro_horas, dias_vigencia_retencion, permite_saldo_negativo, vigente_desde) VALUES
  ('GENERAL_BOB', 'BOB', 365, TRUE, 500.0, 24, 30, FALSE, now())
ON CONFLICT (codigo) DO NOTHING;

-- Bolivia: el circulante mínimo práctico es Bs 0,10
INSERT INTO politica_redondeo (codigo, moneda, unidad_minima, modo, aplica_a) VALUES
  ('BOB_COMISION', 'BOB', 0.1, 'MAS_CERCANO', 'COMISION'),
  ('BOB_IMPUESTO', 'BOB', 0.01, 'MAS_CERCANO', 'IMPUESTO'),
  ('BOB_TOTAL', 'BOB', 0.1, 'MAS_CERCANO', 'TOTAL')
ON CONFLICT (codigo) DO NOTHING;

-- Política global por defecto (grupo_id nulo). Un recargo sin tope vuelve impagable la deuda.
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM politica_mora) THEN
  INSERT INTO politica_mora (dias_gracia, tipo_recargo, valor_recargo, tope_recargo, dias_para_mora_grave, dias_para_incumplimiento, aplica_automatico, vigente_desde) VALUES
    (3, 'PORCENTUAL', 2.0, 100.0, 15, 30, TRUE, now());
  END IF;
END $$;
