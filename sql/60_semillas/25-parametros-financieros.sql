-- Catálogo cerrado de parámetros financieros del modelo A + D (F3.S8.M2, CU-132). Solo códigos y unidades: SIN VALORES.
-- GENERADO desde seeders/minimos/25-parametros-financieros.json — no editar a mano.

-- Parámetros financieros: código, unidad y si van por nivel. Sin valores.
INSERT INTO parametro_financiero (codigo, descripcion, unidad, por_nivel) VALUES
  ('APORTE_FONDO_GRUPO', 'Aporte al fondo del grupo con cada cuota (modelo A, A1), por nivel de puntualidad', 'PORCENTAJE', TRUE),
  ('REDUCCION_APORTE_GRUPO_PERFECTO', 'Reducción del aporte al fondo cuando el grupo repite tras un «Grupo perfecto» (D4)', 'PORCENTAJE', FALSE),
  ('DIAS_DE_GRACIA', 'Días de gracia antes de la cobertura del fondo y del recargo (A3, R1)', 'DIAS', FALSE),
  ('TASA_DE_RECARGO', 'Tasa del recargo por mora (R1)', 'PORCENTAJE', FALSE),
  ('TOPE_DEL_RECARGO', 'Tope del recargo por mora, como porcentaje del aporte en mora (R1)', 'PORCENTAJE', FALSE),
  ('UMBRAL_DE_SOLVENCIA_DEL_FONDO', 'Umbral de solvencia del fondo, como porcentaje del fondo sobre el pozo comprometido', 'PORCENTAJE', FALSE),
  ('UMBRAL_DE_DISOLUCION_POR_MORA', 'Umbral de disolución automática, como cantidad de cuotas impagas del grupo (A8)', 'CONTEO', FALSE)
ON CONFLICT (codigo) DO NOTHING;
