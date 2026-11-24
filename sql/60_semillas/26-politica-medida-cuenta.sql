-- Política de plataforma de las medidas sobre la cuenta del participante (CU-134). Valores aprobados por Pablo el 2026-11-24 SIN fuente normativa (D-3): son una decisión de negocio, editable publicando una versión nueva, nunca una cifra regulatoria.
-- GENERADO desde seeders/minimos/26-politica-medida-cuenta.json — no editar a mano.

-- Descargo 5 días hábiles, apelación 5 días hábiles, prescripción 12 meses, suspensión máxima 90 días.
INSERT INTO politica_medida_cuenta (version, plazo_descargo_dias_habiles, plazo_apelacion_dias_habiles, plazo_resolucion_apelacion_dias_habiles, prescribe_en_meses, duracion_maxima_suspension_dias, fuente, vigente_desde) VALUES
  ('2026-11-24', 5, 5, NULL, 12, 90, 'Decisión de negocio de Pablo del 2026-11-24 (CU-134, D-3); sin fuente normativa', '2026-11-24T00:00:00-04:00')
ON CONFLICT (version) DO NOTHING;
