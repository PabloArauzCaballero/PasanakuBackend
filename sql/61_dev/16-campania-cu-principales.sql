-- Datos de desarrollo para la campaña E2E de los casos de uso principales (docs/trabajo/2026-10-03-campania-cu-principales). Todo es sintético y solo vive en seeders/dev: nada de esto va a producción.
-- GENERADO desde seeders/dev/16-campania-cu-principales.json — no editar a mano.

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO grupo (codigo_publico, nombre, moneda, periodicidad, dia_cobro, num_periodos, cupos_totales, cupos_ocupados, monto_aporte, fecha_inicio, fecha_fin_estimada, estado, tipo_conformacion, modalidad_turnos, visibilidad, requiere_kyc_minimo, reputacion_minima, dias_gracia, porcentaje_fondo_garantia, quorum_decisiones, organizador_id, es_autogestionado, aplica_recargo_mora, usa_fondo_garantia) VALUES
      ('GRP-DEMO-02', 'Pasanaku de campania sintetico', 'BOB', 'MENSUAL', 10, 3, 3, 1, 100.0, (current_date + interval '20 days'), (current_date + interval '110 days'), 'ABIERTO_A_INSCRIPCION', 'MANUAL_POR_INVITACION', 'SORTEO_ALEATORIO', 'PRIVADO', 'BASICO', 0, 3, 5.0, 0.667, (SELECT id FROM organizador WHERE usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USR000001')), FALSE, TRUE, TRUE)
    ON CONFLICT (codigo_publico) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO configuracion_grupo (grupo_id, max_cupos_por_persona, hora_limite_pago, tolerancia_monto_parcial, politica_mora_id, permite_cupos_multiples, permite_permuta_turnos, requiere_avalista, permite_ingreso_tardio) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 1, '20:00:00', 10.0, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1), FALSE, TRUE, FALSE, FALSE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO reglamento_grupo (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde, redactado_por) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 1, 'Reglamento sintetico de la campania de pruebas. Aporte mensual de Bs 100 con vencimiento el dia 10. Tres dias de gracia. El orden de turnos se define por sorteo con semilla verificable.', encode(digest('reglamento-grupo-demo-02-v1', 'sha256'), 'hex'), 'Recargo sobre el aporte impago con tope del 100 % del aporte. La gracia es de 3 dias corridos desde el vencimiento.', 'El cupo se libera y se busca reemplazo. La deuda no se extingue con la salida del grupo.', now() - interval '1 day', (SELECT id FROM usuario WHERE codigo_publico = 'USR000001'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO tarifa_congelada_grupo (grupo_id, tarifario_id, snapshot_conceptos, hash_snapshot, congelada_en, vigente_hasta_ciclo_nro) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1), '{"COM_ENTREGA": {"metodo": "PORCENTUAL", "valor": 0.35, "piso": 10, "techo": 50, "tramo": "0 a 3000"}}'::jsonb, encode(digest('tarifa-congelada-grupo-demo-02-v1', 'sha256'), 'hex'), now() - interval '1 day', 3)
    ON CONFLICT (grupo_id) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO participante (grupo_id, usuario_id, alias, estado, reputacion_al_ingresar, aportes_realizados, aportes_en_mora, fecha_ingreso, es_organizador) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), (SELECT id FROM usuario WHERE codigo_publico = 'USR000001'), 'Organizador demo', 'ACTIVO', 700, 0, 0, now() - interval '1 day', TRUE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cupo (grupo_id, numero, estado, fraccion, asignado_en, participante_id) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 1, 'OCUPADO', 1.0, now() - interval '1 day', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USR000001'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 2, 'LIBRE', 1.0, NULL, NULL),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 3, 'LIBRE', 1.0, NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO aceptacion_reglamento (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen) VALUES
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USR000001')), now() - interval '1 day', encode(digest('reglamento-grupo-demo-02-v1', 'sha256'), 'hex'), '190.129.0.11')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Un periodo por cupo: sin ellos el sorteo no puede repartir turnos (CU-60). Un grupo creado por CU-20 los trae.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO periodo (grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia, fecha_entrega_prevista, estado, monto_objetivo, monto_recaudado, cupos_morosos) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 1, (current_date + interval '20 days'), (current_date + interval '30 days'), (current_date + interval '33 days'), (current_date + interval '35 days'), 'PROGRAMADO', 300, 0, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 2, (current_date + interval '50 days'), (current_date + interval '60 days'), (current_date + interval '63 days'), (current_date + interval '65 days'), 'PROGRAMADO', 300, 0, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-02'), 3, (current_date + interval '80 days'), (current_date + interval '90 days'), (current_date + interval '93 days'), (current_date + interval '95 days'), 'PROGRAMADO', 300, 0, 0)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Destrabe 2: una obligación PENDIENTE para que CU-21 tenga qué cobrar.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO obligacion_aporte (grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, dias_mora, version, politica_mora_id) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01') AND numero = 3), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01') AND numero = 2), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-DEMO-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USR000002')), 'APORTE_PERIODICO', 500.0, 'BOB', 0, 0, 0, 0, 'PENDIENTE', (current_date + interval '25 days'), (current_date + interval '28 days'), 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Destrabe de la campaña: CU-74 (evaluar insignias) exige el permiso SOPORTE y nadie lo tenía. Se le suma el rol SOPORTE al operador USR000091 (permisos = unión de roles, EmitirAcceso). Otorgado por USR000007 porque `ck_asignacion_no_autoasignada` impide el autootorgamiento. Dato sintético de desarrollo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO asignacion_rol (usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en, vigente_hasta) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000091'), (SELECT id FROM rol WHERE codigo = 'SOPORTE'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), now() - interval '30 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;
