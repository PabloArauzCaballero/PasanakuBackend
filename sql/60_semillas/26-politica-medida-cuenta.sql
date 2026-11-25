-- Política de plataforma de las medidas sobre la cuenta del participante (CU-134). Valores aprobados por Pablo como decisión de negocio sin fuente normativa. Un cambio de plazo se publica como versión nueva.
-- GENERADO desde seeders/minimos/26-politica-medida-cuenta.json — no editar a mano.

-- Descargo 5 días hábiles, presentación de apelación 5 días hábiles, resolución de apelación 10 días hábiles desde la versión 2026-11-25, prescripción 12 meses, suspensión máxima 90 días.
INSERT INTO politica_medida_cuenta (version, plazo_descargo_dias_habiles, plazo_apelacion_dias_habiles, plazo_resolucion_apelacion_dias_habiles, prescribe_en_meses, duracion_maxima_suspension_dias, fuente, vigente_desde) VALUES
  ('2026-11-24', 5, 5, NULL, 12, 90, 'Decisión de negocio de Pablo del 2026-11-24 (CU-134, D-3); sin fuente normativa', '2026-11-24T00:00:00-04:00'),
  ('2026-11-25', 5, 5, 10, 12, 90, 'Decisión de negocio de Pablo del 2026-11-25 (CU-134, A-3); sin fuente normativa', '2026-11-25T00:00:00-04:00')
ON CONFLICT (version) DO NOTHING;
