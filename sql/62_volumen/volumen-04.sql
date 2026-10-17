-- VOLUMEN SINTETICO, clon 04 (02+03+04+06+07+08+09+11+12+14; 95 dias atras). Generado por scripts/generar_volumen_dev.py con semilla 20261016; no editar a mano.
-- GENERADO desde seeders/volumen/volumen-04.json — no editar a mano.

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO usuario (codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro) VALUES
      ('USRV04001', 'Tania', 'Estévez Cano', '+59179040001', 'hinojosajose.v04727@volumen.pasanaku.test', '1992-10-01', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', (now() - interval '95 days')),
      ('USRV04002', 'Soledad', 'Valadez Soria', '+59179040002', 'ybarraza.v04606@volumen.pasanaku.test', '1988-03-08', 'ACTIVO', 'INTERMEDIO', 'es-BO', 'America/La_Paz', (now() - interval '95 days')),
      ('USRV04003', 'Hernán', 'Mesa Nieves', '+59179040003', 'mariacristin.v04598@volumen.pasanaku.test', '2002-10-20', 'ACTIVO', 'INTERMEDIO', 'es-BO', 'America/La_Paz', (now() - interval '95 days')),
      ('USRV04004', 'María José', 'Esquibel Ortiz', '+59179040004', 'curielvanesa.v04204@volumen.pasanaku.test', '1962-12-28', 'ACTIVO', 'BASICO', 'es-BO', 'America/La_Paz', (now() - interval '95 days')),
      ('USRV04005', 'Abelardo', 'Chapa del Valle', '+59179040005', 'ramonpalacio.v04940@volumen.pasanaku.test', '1977-04-18', 'ACTIVO', 'COMPLETO', 'es-BO', 'America/La_Paz', (now() - interval '95 days')),
      ('USRV04006', 'Marisela', 'Noriega Quintero', '+59179040006', 'mariacristin.v04313@volumen.pasanaku.test', '1997-05-05', 'ACTIVO', 'INTERMEDIO', 'es-BO', 'America/La_Paz', (now() - interval '95 days'))
    ON CONFLICT (codigo_publico) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO credencial_acceso (usuario_id, hash_contrasena, algoritmo, parametros_kdf, cambiada_en, requiere_cambio) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), repeat('b', 64), 'argon2id', '{"m": 65536, "t": 3, "p": 4}'::jsonb, (now() - interval '95 days'), FALSE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- `version_llave` dice con qué versión de la llave maestra se cifró el dato: rotar la llave es cifrar de nuevo y subir el número, no perder el acceso a lo viejo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO documento_identidad (usuario_id, tipo, numero_cifrado, hash_numero, pais_emision, url_anverso, hash_archivo, estado, version_llave) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'CI', 'cifrado-USRV04001', encode(digest('USRV04001','sha256'),'hex'), 'BO', 'https://v04.local/USRV04001-a.jpg', repeat('a', 64), 'APROBADA', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'CI', 'cifrado-USRV04002', encode(digest('USRV04002','sha256'),'hex'), 'BO', 'https://v04.local/USRV04002-a.jpg', repeat('a', 64), 'APROBADA', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'CI', 'cifrado-USRV04003', encode(digest('USRV04003','sha256'),'hex'), 'BO', 'https://v04.local/USRV04003-a.jpg', repeat('a', 64), 'APROBADA', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'CI', 'cifrado-USRV04004', encode(digest('USRV04004','sha256'),'hex'), 'BO', 'https://v04.local/USRV04004-a.jpg', repeat('a', 64), 'APROBADA', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'CI', 'cifrado-USRV04005', encode(digest('USRV04005','sha256'),'hex'), 'BO', 'https://v04.local/USRV04005-a.jpg', repeat('a', 64), 'APROBADA', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'CI', 'cifrado-USRV04006', encode(digest('USRV04006','sha256'),'hex'), 'BO', 'https://v04.local/USRV04006-a.jpg', repeat('a', 64), 'APROBADA', 1)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO verificacion_kyc (usuario_id, nivel_solicitado, estado, iniciada_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'INTERMEDIO', 'APROBADA', (now() - interval '95 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Se declara ANTES de la debida diligencia: el trigger tg_ddd_pep la evalúa al insertarla
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO declaracion_pep (usuario_id, es_pep, tipo_pep, cargo, institucion, pais, desde, declarada_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), TRUE, 'NACIONAL', 'Concejal municipal', 'Gobierno Autónomo Municipal', 'BO', '2024-01-01', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), FALSE, NULL, NULL, NULL, NULL, NULL, (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), FALSE, NULL, NULL, NULL, NULL, NULL, (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), FALSE, NULL, NULL, NULL, NULL, NULL, (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), FALSE, NULL, NULL, NULL, NULL, NULL, (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), FALSE, NULL, NULL, NULL, NULL, NULL, (now() - interval '95 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO debida_diligencia (usuario_id, tipo, estado, documentos_requeridos, documentos_recibidos, iniciada_en, completada_en, aprobada_por, segunda_revision_por) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'REFORZADA', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'ESTANDAR', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'ESTANDAR', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'SIMPLIFICADA', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'AMPLIADA', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'ESTANDAR', 'COMPLETA', '["CI", "SELFIE"]'::jsonb, '["CI", "SELFIE"]'::jsonb, (now() - interval '95 days'), (now() - interval '95 days'), NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO calificacion_riesgo_cliente (usuario_id, nivel, puntaje_total, nivel_dd_requerido, periodicidad_revision_meses, vigente_desde, proxima_revision, es_automatica, motivo_cambio) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'ALTO', 72, 'REFORZADA', 6, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, 'Persona expuesta políticamente'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'MEDIO', 45, 'ESTANDAR', 12, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'BAJO', 22, 'ESTANDAR', 24, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'MEDIO', 45, 'SIMPLIFICADA', 12, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'ALTO', 72, 'AMPLIADA', 6, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'BAJO', 22, 'ESTANDAR', 24, (now() - interval '95 days'), (current_date + interval '85 days'), TRUE, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO expediente_cliente (usuario_id, completitud_porcentaje, documentos, retencion_hasta, estado, ultima_actualizacion) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 100, '["CI", "SELFIE", "DOMICILIO"]'::jsonb, (current_date + interval '3555 days'), 'COMPLETO', (now() - interval '95 days'))
    ON CONFLICT (usuario_id) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO perfil_transaccional (usuario_id, tipo, monto_mensual_estimado, cantidad_operaciones_estimada, actividad_economica, origen_fondos_declarado, moneda, fuente, vigente_desde, actualizado_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'DECLARADO', 8000, 12, 'Comerciante minorista', 'Ingresos del negocio', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'DECLARADO', 4500, 12, 'Asalariado', 'Salario', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'DECLARADO', 3800, 12, 'Asalariada', 'Salario', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'DECLARADO', 2500, 12, 'Transportista', 'Ingresos del servicio', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'DECLARADO', 15000, 12, 'Comerciante mayorista', 'Ingresos del negocio', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days')),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'DECLARADO', 6000, 12, 'Profesional independiente', 'Honorarios', 'BOB', 'DECLARACION_APP', (now() - interval '95 days'), (now() - interval '95 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO aceptacion_contrato (contrato_adhesion_id, usuario_id, version_aceptada, ip, hash_evidencia, aceptado_en) VALUES
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days')),
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days')),
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days')),
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days')),
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days')),
      ((SELECT id FROM contrato_adhesion WHERE codigo = 'CTO-BILLETERA' AND version = 1), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 1, '190.129.0.1', repeat('a', 64), (now() - interval '95 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cuenta_billetera (numero_cuenta, tipo, usuario_id, moneda, estado, nivel_debida_diligencia, fecha_apertura, politica_billetera_id, cuenta_contable_id) VALUES
      ('BOB-V040001', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'BOB', 'ACTIVA', 'REFORZADA', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01')),
      ('BOB-V040002', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'BOB', 'ACTIVA', 'ESTANDAR', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01')),
      ('BOB-V040003', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'BOB', 'ACTIVA', 'ESTANDAR', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01')),
      ('BOB-V040004', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'BOB', 'ACTIVA', 'SIMPLIFICADA', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01')),
      ('BOB-V040005', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'BOB', 'ACTIVA', 'AMPLIADA', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01')),
      ('BOB-V040006', 'USUARIO', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'BOB', 'ACTIVA', 'ESTANDAR', (now() - interval '95 days'), (SELECT id FROM politica_billetera WHERE codigo = 'GENERAL_BOB'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'))
    ON CONFLICT (numero_cuenta) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO organizador (usuario_id, estado, nivel, limite_grupos_simultaneos, limite_monto_administrado, grupos_activos, grupos_historicos, monto_administrado_actual, calificacion_promedio, indice_morosidad_cartera, fecha_postulacion, fecha_habilitacion) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'HABILITADO', 'ESTANDAR', 3, 50000, 1, 0, 3000, 0, 16.67, (current_date + interval '-215 days'), (current_date + interval '-155 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Sin capacitación vigente no hay habilitación: los dos módulos del nivel ESTANDAR (archivo mínimo 17, HAB_ESTANDAR_CAPACITACION).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO capacitacion_organizador (organizador_id, modulo, completada_en, puntaje_evaluacion, aprobada, vigente_hasta) VALUES
      ((SELECT id FROM organizador WHERE usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'Reglas del pasanaku y responsabilidades del organizador', (now() - interval '95 days') - interval '70 days', 92.0, TRUE, (current_date + interval '200 days')),
      ((SELECT id FROM organizador WHERE usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'Manejo de mora, cobranza y trato al participante', (now() - interval '95 days') - interval '68 days', 88.0, TRUE, (current_date + interval '202 days'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Token de firma consumido. La restricción ck_contrato_org_firma exige que un contrato firmado apunte al token con el que se firmó: la firma tiene que poder probarse.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO token_verificacion (usuario_id, politica_id, tipo_token, proposito, hash_token, algoritmo_hash, canal_entrega, destino_enmascarado, estado, emitido_en, expira_en, enviado_en, consumido_en, intentos_fallidos, max_intentos, reenvios, ip_origen, agente_usuario, correlation_id, clave_idempotencia, uso_unico, url_destino) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), (SELECT id FROM politica_token WHERE proposito = 'FIRMA_REGLAMENTO' ORDER BY vigente_desde DESC LIMIT 1), 'ENLACE', 'FIRMA_REGLAMENTO', encode(digest('v04-firma-contrato-organizador', 'sha256'), 'hex'), 'SHA-256', 'CORREO', 'd***1@pasanaku.test', 'CONSUMIDO', (now() - interval '95 days') - interval '60 days', (now() - interval '95 days') - interval '57 days', (now() - interval '95 days') - interval '60 days', (now() - interval '95 days') - interval '60 days', 0, 5, 0, '190.129.0.11', 'AportaYa/1.0 (Android 14)', gen_random_uuid(), 'v04-token-firma-contrato-org-001', TRUE, 'https://pasanaku.bo/firma/contrato-organizador')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- R-ORG-02: sin este contrato firmado y vigente, fn_org_validar_contrato_grupo() rechaza la creación del grupo. Es la primera restricción que se topa quien intente sembrar un grupo sin habilitar al organizador.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO contrato_organizador (organizador_id, version, contenido_hash, obligaciones, causales_rescision, firmado_en, token_firma_id, vigente_desde, vigente_hasta, rescindido_en, motivo_rescision) VALUES
      ((SELECT id FROM organizador WHERE usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'v1', encode(digest('contrato-organizador-v1-v04', 'sha256'), 'hex'), 'Administrar el grupo sin custodiar dinero ni cobrar por fuera de la plataforma; convocar y registrar las decisiones del grupo; gestionar la mora dentro de la estrategia de cobranza vigente; no ocupar más de un cupo propio.', 'Cobro de dinero por fuera de la plataforma; sanción firme por incumplimiento del organizador; índice de morosidad de cartera superior al umbral por dos períodos consecutivos; inactividad mayor a 180 días.', (now() - interval '95 days') - interval '60 days', (SELECT id FROM token_verificacion WHERE clave_idempotencia = 'v04-token-firma-contrato-org-001'), (current_date + interval '-155 days'), NULL, NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO grupo (codigo_publico, nombre, moneda, periodicidad, dia_cobro, num_periodos, cupos_totales, cupos_ocupados, monto_aporte, fecha_inicio, fecha_fin_estimada, estado, tipo_conformacion, modalidad_turnos, visibilidad, requiere_kyc_minimo, reputacion_minima, dias_gracia, porcentaje_fondo_garantia, quorum_decisiones, organizador_id, es_autogestionado, aplica_recargo_mora, usa_fondo_garantia) VALUES
      ('GRP-V04-01', 'Pasanaku v04 del barrio', 'BOB', 'MENSUAL', 10, 6, 6, 6, 500.0, (current_date + interval '-140 days'), (current_date + interval '40 days'), 'EN_CURSO', 'MANUAL_POR_INVITACION', 'SORTEO_ALEATORIO', 'PRIVADO', 'BASICO', 0, 3, 5.0, 0.667, (SELECT id FROM organizador WHERE usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), FALSE, TRUE, TRUE)
    ON CONFLICT (codigo_publico) DO NOTHING;
  END IF;
END $siembra$;

-- Los booleanos van explícitos y no por omisión: este grupo aplica recargo por mora y usa fondo de garantía, permite hasta 2 cupos por persona y permuta de turnos, y no admite ingresos con el ciclo empezado.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO configuracion_grupo (grupo_id, max_cupos_por_persona, hora_limite_pago, tolerancia_monto_parcial, politica_mora_id, permite_cupos_multiples, permite_permuta_turnos, requiere_avalista, permite_ingreso_tardio) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 2, '20:00:00', 50.0, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1), TRUE, TRUE, FALSE, FALSE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO reglamento_grupo (grupo_id, version, contenido, hash_contenido, clausulas_mora, clausulas_abandono, vigente_desde, redactado_por) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 1, 'Reglamento del grupo v04. Aporte mensual de Bs 500 con vencimiento el día 10. Tres días de gracia. Recargo del 2 % sobre el aporte, con tope del 100 %. El fondo de garantía se constituye con Bs 100 por participante al inicio y cubre el aporte impago a partir del quinto día de mora, generando una deuda a nombre de quien no pagó. El orden de turnos se define por sorteo con semilla verificable.', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), 'Recargo del 2 % sobre el aporte impago, con tope del 100 % del aporte. La gracia es de 3 días corridos desde el vencimiento.', 'El cupo se libera y se busca reemplazo. La deuda no se extingue con la salida del grupo.', (now() - interval '95 days') - interval '45 days', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El precio pactado no cambia a mitad del juego (R-TAR-07)
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO tarifa_congelada_grupo (grupo_id, tarifario_id, snapshot_conceptos, hash_snapshot, congelada_en, vigente_hasta_ciclo_nro) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1), '{"COM_ENTREGA": {"metodo": "PORCENTUAL", "valor": 0.35, "piso": 10, "techo": 50, "tramo": "0 a 3000"}}'::jsonb, encode(digest('tarifa-congelada-grupo-v04-v1', 'sha256'), 'hex'), (now() - interval '95 days') - interval '45 days', 6)
    ON CONFLICT (grupo_id) DO NOTHING;
  END IF;
END $siembra$;

-- USRV04004 está EN_MORA: es el caso que ejercita mora, cobertura del fondo, deuda y debido proceso.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO participante (grupo_id, usuario_id, alias, estado, reputacion_al_ingresar, aportes_realizados, aportes_en_mora, fecha_ingreso, es_organizador) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'María', 'ACTIVO', 700, 2, 0, (now() - interval '95 days') - interval '45 days', TRUE),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'Juan', 'ACTIVO', 700, 2, 0, (now() - interval '95 days') - interval '45 days', FALSE),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'Ana', 'ACTIVO', 700, 2, 0, (now() - interval '95 days') - interval '45 days', FALSE),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'Pedro', 'EN_MORA', 700, 1, 1, (now() - interval '95 days') - interval '45 days', FALSE),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'Rosa', 'ACTIVO', 700, 2, 0, (now() - interval '95 days') - interval '45 days', FALSE),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'Luis', 'ACTIVO', 700, 2, 0, (now() - interval '95 days') - interval '45 days', FALSE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cupo (grupo_id, numero, estado, fraccion, asignado_en, participante_id) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 1, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 2, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 3, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 4, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 5, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'))),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 6, 'OCUPADO', 1.0, (now() - interval '95 days') - interval '45 days', (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Sorteo con compromiso y revelado: la semilla previa se publica antes, la del servidor después, y cualquiera puede recalcular el resultado (CU de transparencia).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO sorteo_turnos (grupo_id, algoritmo, estado, hash_semilla_previo, aportes_entropia, fecha_compromiso, fecha_revelado_prevista, semilla_servidor, semilla_publica, resultado, ejecutado_por, fecha_ejecucion) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'FISHER_YATES_HMAC', 'REVELADO', encode(digest('semilla-servidor-v04-grupo-01', 'sha256'), 'hex'), '{"participantes": ["USRV04001", "USRV04002", "USRV04003", "USRV04004", "USRV04005", "USRV04006"], "bloque_referencia": "cierre-2026-01"}'::jsonb, (now() - interval '95 days') - interval '46 days', (now() - interval '95 days') - interval '45 days', 'semilla-servidor-v04-grupo-01', 'entropia-publica-v04-grupo-01', '{"orden": [1, 2, 3, 4, 5, 6]}'::jsonb, (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), (now() - interval '95 days') - interval '45 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Los seis firmaron el reglamento antes de que arrancara el primer período: sin firma, la obligación no es oponible.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO aceptacion_reglamento (reglamento_id, participante_id, aceptado_en, hash_firmado, ip_origen) VALUES
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.11'),
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.12'),
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.13'),
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.14'),
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.15'),
      ((SELECT id FROM reglamento_grupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND version = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')), (now() - interval '95 days') - interval '45 days', encode(digest('reglamento-grupo-v04-v1', 'sha256'), 'hex'), '190.129.0.16')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Período 1 liquidado y entregado; período 2 vencido con un moroso; del 3 al 6 programados.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO periodo (grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia, fecha_entrega_prevista, estado, monto_objetivo, monto_recaudado, cupos_morosos) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 1, (current_date + interval '-140 days'), (current_date + interval '-130 days'), (current_date + interval '-127 days'), (current_date + interval '-125 days'), 'LIQUIDADO', 3000, 3000, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 2, (current_date + interval '-110 days'), (current_date + interval '-103 days'), (current_date + interval '-100 days'), (current_date + interval '-93 days'), 'ABIERTO', 3000, 3000, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 3, (current_date + interval '-80 days'), (current_date + interval '-70 days'), (current_date + interval '-67 days'), (current_date + interval '-65 days'), 'PROGRAMADO', 3000, 0, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 4, (current_date + interval '-50 days'), (current_date + interval '-40 days'), (current_date + interval '-37 days'), (current_date + interval '-35 days'), 'PROGRAMADO', 3000, 0, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 5, (current_date + interval '-20 days'), (current_date + interval '-10 days'), (current_date + interval '-7 days'), (current_date + interval '-5 days'), 'PROGRAMADO', 3000, 0, 0),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 6, (current_date + interval '10 days'), (current_date + interval '20 days'), (current_date + interval '23 days'), (current_date + interval '25 days'), 'PROGRAMADO', 3000, 0, 0)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO turno (grupo_id, periodo_id, cupo_id, orden_asignado, estado, criterio_asignacion, monto_estimado_cobro, confirmado_en) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), 1, 'COBRADO', 'SORTEO', 3000, (now() - interval '95 days') - interval '30 days'),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), 2, 'EN_CURSO', 'SORTEO', 3000, NULL),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 3), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 3), 3, 'PROGRAMADO', 'SORTEO', 3000, NULL),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), 4, 'PROGRAMADO', 'SORTEO', 3000, NULL),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 5), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 5), 5, 'PROGRAMADO', 'SORTEO', 3000, NULL),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 6), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 6), 6, 'PROGRAMADO', 'SORTEO', 3000, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Constitución del fondo de garantía: Bs 100 por participante, una sola vez, en el período 1. Es una obligación de tipo distinto al aporte, para que el reporte del grupo no las mezcle.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO obligacion_aporte (grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, fecha_pago_efectivo, dias_mora, version) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 3), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 5), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 6), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')), 'APORTE_FONDO_GARANTIA', 100.0, 'BOB', 100.0, 0, 0, 0, 'PAGADO', (current_date + interval '-138 days'), (current_date + interval '-135 days'), (now() - interval '95 days') - interval '43 days', 0, 1)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Período 1: los seis aportes pagados. Es el camino feliz completo, el que termina en entrega acreditada.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO obligacion_aporte (grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, fecha_pago_efectivo, dias_mora, version, politica_mora_id) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '36 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '36 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 3), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '36 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '34 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 5), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '36 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 6), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-130 days'), (current_date + interval '-127 days'), (now() - interval '95 days') - interval '36 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Período 2: cinco pagados y uno cubierto por el fondo. La obligación de USRV04004 tiene monto_recargo 10,00 (2 % de 500) y monto_cubierto_garantia 500,00; su saldo_pendiente derivado queda en 10,00 — el recargo NO lo cubre el fondo, lo debe el moroso.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO obligacion_aporte (grupo_id, periodo_id, cupo_id, participante_id, tipo, monto_esperado, moneda, monto_pagado, monto_recargo, monto_condonado, monto_cubierto_garantia, estado, fecha_vencimiento, fecha_fin_gracia, fecha_pago_efectivo, dias_mora, version, politica_mora_id) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-103 days'), (current_date + interval '-100 days'), (now() - interval '95 days') - interval '12 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04002')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-103 days'), (current_date + interval '-100 days'), (now() - interval '95 days') - interval '12 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 3), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04003')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-103 days'), (current_date + interval '-100 days'), (now() - interval '95 days') - interval '11 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), 'APORTE_PERIODICO', 500.0, 'BOB', 0, 10.0, 0, 500.0, 'CUBIERTO_POR_GARANTIA', (current_date + interval '-103 days'), (current_date + interval '-100 days'), NULL, 8, 3, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 5), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-103 days'), (current_date + interval '-100 days'), (now() - interval '95 days') - interval '13 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1)),
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 6), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04006')), 'APORTE_PERIODICO', 500.0, 'BOB', 500.0, 0, 0, 0, 'PAGADO', (current_date + interval '-103 days'), (current_date + interval '-100 days'), (now() - interval '95 days') - interval '10 days', 0, 1, (SELECT id FROM politica_mora WHERE grupo_id IS NULL LIMIT 1))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO fondo_garantia (ambito, grupo_id, politica_cobertura_id, cuenta_contable_id, moneda, saldo_disponible, saldo_comprometido, total_aportado, total_cubierto, total_recuperado, estado, total_castigado, version) VALUES
      ('POR_GRUPO', (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM politica_cobertura LIMIT 1), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 'BOB', 0, 0, 0, 0, 0, 'ACTIVO', 0, 0)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La bolsa del grupo tiene cuenta propia: el titular es el grupo, nunca el organizador (R-GRP-04)
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cuenta_billetera (numero_cuenta, tipo, grupo_id, moneda, estado, nivel_debida_diligencia, fecha_apertura, cuenta_contable_id) VALUES
      ('GRP-V040001', 'GRUPO', (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'BOB', 'ACTIVA', 'ESTANDAR', (now() - interval '95 days'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'))
    ON CONFLICT (numero_cuenta) DO NOTHING;
  END IF;
END $siembra$;

-- El fondo de garantía tiene cuenta propia: no es del grupo ni del organizador (ck_cuenta_titularidad exige titular nulo para este tipo).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cuenta_billetera (numero_cuenta, tipo, moneda, estado, nivel_debida_diligencia, fecha_apertura, cuenta_contable_id) VALUES
      ('FDG-V040001', 'FONDO_GARANTIA', 'BOB', 'ACTIVA', 'REFORZADA', (now() - interval '95 days') - interval '45 days', (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'))
    ON CONFLICT (numero_cuenta) DO NOTHING;
  END IF;
END $siembra$;

-- Instrumentos verificados y con titular coincidente. ck_instrumento_sin_pan impide guardar el número completo: solo enmascarado y hash.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO instrumento_fondeo (usuario_id, tipo, entidad_financiera, token_proveedor, hash_identificador, enmascarado, titular_nombre, titular_documento, titular_coincide, moneda, es_principal, estado_verificacion, metodo_verificacion, verificado_en, bloqueado_hasta) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'CUENTA_BANCARIA', 'BANCO DEMO S.A.', NULL, encode(digest('v04-instrumento-01', 'sha256'), 'hex'), '****4001-V04', 'Titular v04 USRV04001', '8040001', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'CUENTA_BANCARIA', 'BANCO DEMO S.A.', NULL, encode(digest('v04-instrumento-02', 'sha256'), 'hex'), '****4002', 'Titular v04 USRV04002', '9000002', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'CUENTA_BANCARIA', 'BANCO DEMO S.A.', NULL, encode(digest('v04-instrumento-03', 'sha256'), 'hex'), '****4003', 'Titular v04 USRV04003', '9000003', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'CUENTA_BANCARIA', 'BANCO UNION S.A.', NULL, encode(digest('v04-instrumento-04', 'sha256'), 'hex'), '****4004', 'Titular v04 USRV04004', '9000004', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'CUENTA_BANCARIA', 'BANCO DEMO S.A.', NULL, encode(digest('v04-instrumento-05', 'sha256'), 'hex'), '****4005', 'Titular v04 USRV04005', '9000005', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'CUENTA_BANCARIA', 'BANCO DEMO S.A.', NULL, encode(digest('v04-instrumento-06', 'sha256'), 'hex'), '****4006', 'Titular v04 USRV04006', '9000006', TRUE, 'BOB', TRUE, 'VERIFICADO', 'MICRODEPOSITO', (now() - interval '95 days') - interval '46 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO extracto_bancario (proveedor_id, cuenta, fecha_desde, fecha_hasta, saldo_inicial, saldo_final, archivo_url, importado_en, importado_por) VALUES
      ((SELECT id FROM proveedor_pago WHERE codigo = 'BANCO_CUSTODIO'), '****4321', (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 9900.0, 'https://almacen.pasanaku.test/extractos/v04-45d.csv', (now() - interval '95 days') - interval '1 day', (SELECT id FROM usuario WHERE codigo_publico = 'USR000008'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Lo que el banco dice que pasó. Contra esto se concilia todo lo demás.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_bancario (extracto_id, fecha_movimiento, monto, moneda, glosa, referencia_banco, cuenta_origen, conciliado) VALUES
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 1200.0, 'BOB', 'Abono recarga billetera USRV04001', 'BCO-REC-01-V04', '****4001-V04', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 1200.0, 'BOB', 'Abono recarga billetera USRV04002', 'BCO-REC-02-V04', '****4002', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 1200.0, 'BOB', 'Abono recarga billetera USRV04003', 'BCO-REC-03-V04', '****4003', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 600.0, 'BOB', 'Abono recarga billetera USRV04004', 'BCO-REC-04-V04', '****4004', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 2000.0, 'BOB', 'Abono recarga billetera USRV04005', 'BCO-REC-05-V04', '****4005', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-139 days'), 1200.0, 'BOB', 'Abono recarga billetera USRV04006', 'BCO-REC-06-V04', '****4006', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-107 days'), 500.0, 'BOB', 'Cobro QR aporte periodo 2 USRV04001', 'BCO-P2-01-V04', '****4001-V04', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-107 days'), 500.0, 'BOB', 'Cobro QR aporte periodo 2 USRV04002', 'BCO-P2-02-V04', '****4002', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-106 days'), 500.0, 'BOB', 'Cobro QR aporte periodo 2 USRV04003', 'BCO-P2-03-V04', '****4003', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-108 days'), 500.0, 'BOB', 'Cobro QR aporte periodo 2 USRV04005', 'BCO-P2-05-V04', '****4005', TRUE),
      ((SELECT id FROM extracto_bancario WHERE archivo_url = 'https://almacen.pasanaku.test/extractos/v04-45d.csv'), (current_date + interval '-105 days'), 500.0, 'BOB', 'Cobro QR aporte periodo 2 USRV04006', 'BCO-P2-06-V04', '****4006', TRUE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO orden_recarga (cuenta_billetera_id, instrumento_fondeo_id, proveedor_id, pago_id, transaccion_id, monto_bruto, costo_proveedor, monto_acreditado, moneda, estado, referencia_externa, clave_idempotencia, solicitada_en, acreditada_en, expira_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-01', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 1200.0, 0.0, 1200.0, 'BOB', 'ACREDITADA', 'QR-000001-INI-V04', 'v04-recarga-01', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-02', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 1200.0, 0.0, 1200.0, 'BOB', 'ACREDITADA', 'QR-000002-INI-V04', 'v04-recarga-02', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-03', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 1200.0, 0.0, 1200.0, 'BOB', 'ACREDITADA', 'QR-000003-INI-V04', 'v04-recarga-03', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-04', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 600.0, 0.0, 600.0, 'BOB', 'ACREDITADA', 'QR-V04-04', 'v04-recarga-04', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-05', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 2000.0, 0.0, 2000.0, 'BOB', 'ACREDITADA', 'QR-000005-INI-V04', 'v04-recarga-05', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-06', 'sha256'), 'hex')), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), NULL, NULL, 1200.0, 0.0, 1200.0, 'BOB', 'ACREDITADA', 'QR-000006-INI-V04', 'v04-recarga-06', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '43 days')
    ON CONFLICT (cuenta_billetera_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

-- hash_registro va vacío a propósito: lo calcula la base al insertar (R-AUD-03). Escribirlo desde el seeder sería fabricar la cadena.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('RECARGA', 'APLICADA', 'BOB', 1200.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-01'), 'APP', 'v04-recarga-01', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days'),
      ('RECARGA', 'APLICADA', 'BOB', 1200.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-02'), 'APP', 'v04-recarga-02', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days'),
      ('RECARGA', 'APLICADA', 'BOB', 1200.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-03'), 'APP', 'v04-recarga-03', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days'),
      ('RECARGA', 'APLICADA', 'BOB', 600.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-04'), 'APP', 'v04-recarga-04', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days'),
      ('RECARGA', 'APLICADA', 'BOB', 2000.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-05'), 'APP', 'v04-recarga-05', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days'),
      ('RECARGA', 'APLICADA', 'BOB', 1200.0, NULL, 'ORDEN_RECARGA', (SELECT id FROM orden_recarga WHERE clave_idempotencia = 'v04-recarga-06'), 'APP', 'v04-recarga-06', '', (now() - interval '95 days') - interval '44 days', (now() - interval '95 days') - interval '44 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), 1, 'CREDITO', 1200.0, 1200.0, 0.0, 'Recarga acreditada — referencia QR-000001-INI-V04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 1200.0, -1200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 01', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), 1, 'CREDITO', 1200.0, 1200.0, 0.0, 'Recarga acreditada — referencia QR-000002-INI-V04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 1200.0, -2400.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 02', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), 1, 'CREDITO', 1200.0, 1200.0, 0.0, 'Recarga acreditada — referencia QR-000003-INI-V04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 1200.0, -3600.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 03', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), 1, 'CREDITO', 600.0, 600.0, 0.0, 'Recarga acreditada — referencia QR-000004-INI', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 600.0, -4200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), 1, 'CREDITO', 2000.0, 2000.0, 0.0, 'Recarga acreditada — referencia QR-000005-INI-V04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 2000.0, -6200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 05', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), 1, 'CREDITO', 1200.0, 1200.0, 0.0, 'Recarga acreditada — referencia QR-000006-INI-V04', (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 1200.0, -7400.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 06', (now() - interval '95 days') - interval '44 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La orden y su transacción se apuntan mutuamente; el enlace se cierra después de insertar ambas
UPDATE orden_recarga o SET transaccion_id = t.id FROM transaccion_billetera t
 WHERE t.clave_idempotencia = o.clave_idempotencia AND o.transaccion_id IS NULL;

-- El lado bancario de cada recarga, ya conciliado contra el extracto
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_custodia (cuenta_custodia_id, movimiento_bancario_id, fecha_valor, tipo, sentido, monto, referencia_bancaria, glosa, conciliado, registrado_en) VALUES
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-01-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 1200.0, 'BCO-REC-01-V04', 'Abono por recarga de billetera del titular USRV04001', TRUE, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-02-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 1200.0, 'BCO-REC-02-V04', 'Abono por recarga de billetera del titular USRV04002', TRUE, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-03-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 1200.0, 'BCO-REC-03-V04', 'Abono por recarga de billetera del titular USRV04003', TRUE, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-04-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 600.0, 'BCO-REC-04-V04', 'Abono por recarga de billetera del titular USRV04004', TRUE, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-05-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 2000.0, 'BCO-REC-05-V04', 'Abono por recarga de billetera del titular USRV04005', TRUE, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-REC-06-V04'), (current_date + interval '-139 days'), 'INGRESO', 'CREDITO', 1200.0, 'BCO-REC-06-V04', 'Abono por recarga de billetera del titular USRV04006', TRUE, (now() - interval '95 days') - interval '44 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Constitución del fondo de garantía: Bs 100 por participante desde su saldo
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04001'), 'APP', 'v04-fondo-inicial-01', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days'),
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04002'), 'APP', 'v04-fondo-inicial-02', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days'),
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04003'), 'APP', 'v04-fondo-inicial-03', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days'),
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04004'), 'APP', 'v04-fondo-inicial-04', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days'),
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04005'), 'APP', 'v04-fondo-inicial-05', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days'),
      ('REPOSICION_GARANTIA', 'APLICADA', 'BOB', 100.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND u.codigo_publico = 'USRV04006'), 'APP', 'v04-fondo-inicial-06', '', (now() - interval '95 days') - interval '43 days', (now() - interval '95 days') - interval '43 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), 1, 'DEBITO', 100.0, 1100.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 100.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04001', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), 1, 'DEBITO', 100.0, 1100.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 200.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04002', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), 1, 'DEBITO', 100.0, 1100.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 300.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04003', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), 1, 'DEBITO', 100.0, 500.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 400.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04004', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), 1, 'DEBITO', 100.0, 1900.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 500.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04005', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), 1, 'DEBITO', 100.0, 1100.0, 0.0, 'Constitución del fondo de garantía del grupo', (now() - interval '95 days') - interval '43 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 2, 'CREDITO', 100.0, 600.0, 0.0, 'Aporte inicial al fondo de garantía — USRV04006', (now() - interval '95 days') - interval '43 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El libro del fondo: cada constitución deja su saldo resultante
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_fondo (fondo_id, asiento_contable_id, tipo, monto, saldo_resultante, referencia_tipo, referencia_id, descripcion, fecha, registrado_por) VALUES
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 100.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04001'), 'Constitución inicial del fondo — USRV04001', (now() - interval '95 days') - interval '43 days', NULL),
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 200.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04002'), 'Constitución inicial del fondo — USRV04002', (now() - interval '95 days') - interval '43 days', NULL),
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 300.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04003'), 'Constitución inicial del fondo — USRV04003', (now() - interval '95 days') - interval '43 days', NULL),
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 400.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04004'), 'Constitución inicial del fondo — USRV04004', (now() - interval '95 days') - interval '43 days', NULL),
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 500.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04005'), 'Constitución inicial del fondo — USRV04005', (now() - interval '95 days') - interval '43 days', NULL),
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'CONSTITUCION', 100.0, 600.0, 'obligacion_aporte', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_FONDO_GARANTIA' AND pe.numero = 1 AND u.codigo_publico = 'USRV04006'), 'Constitución inicial del fondo — USRV04006', (now() - interval '95 days') - interval '43 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Aportes del período 1 pagados con saldo de billetera: no hay proveedor externo, la contrapartida es interna
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO pago (obligacion_id, intento_pago_id, proveedor_id, monto, moneda, monto_comision_proveedor, monto_neto_acreditado, canal, estado, fecha_hora_pago, fecha_hora_acreditacion, referencia_proveedor, pagador_nombre, pagador_documento, cuenta_origen_enmascarada, registrado_por, es_manual, clave_idempotencia) VALUES
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04001'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-01-V04', NULL, NULL, '****4001-V04', NULL, FALSE, 'v04-pago-p1-01'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04002'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-02-V04', NULL, NULL, '****4002', NULL, FALSE, 'v04-pago-p1-02'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04003'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-03-V04', NULL, NULL, '****4003', NULL, FALSE, 'v04-pago-p1-03'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04004'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-04-V04', NULL, NULL, '****4004', NULL, FALSE, 'v04-pago-p1-04'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04005'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-05-V04', NULL, NULL, '****4005', NULL, FALSE, 'v04-pago-p1-05'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04006'), NULL, NULL, 500.0, 'BOB', 0.0, 500.0, 'BILLETERA_MOVIL', 'ACREDITADO', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days', 'INT-P1-06-V04', NULL, NULL, '****4006', NULL, FALSE, 'v04-pago-p1-06')
    ON CONFLICT (obligacion_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04001'), 'APP', 'v04-aporte-p1-01', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04002'), 'APP', 'v04-aporte-p1-02', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04003'), 'APP', 'v04-aporte-p1-03', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04004'), 'APP', 'v04-aporte-p1-04', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04005'), 'APP', 'v04-aporte-p1-05', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 1 AND u.codigo_publico = 'USRV04006'), 'APP', 'v04-aporte-p1-06', '', (now() - interval '95 days') - interval '36 days', (now() - interval '95 days') - interval '36 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), 1, 'DEBITO', 500.0, 600.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 500.0, 0.0, 'Aporte del período 1 — USRV04001', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), 1, 'DEBITO', 500.0, 600.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 1000.0, 0.0, 'Aporte del período 1 — USRV04002', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), 1, 'DEBITO', 500.0, 600.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 1500.0, 0.0, 'Aporte del período 1 — USRV04003', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), 1, 'DEBITO', 500.0, 0.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 2000.0, 0.0, 'Aporte del período 1 — USRV04004', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), 1, 'DEBITO', 500.0, 1400.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 2500.0, 0.0, 'Aporte del período 1 — USRV04005', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), 1, 'DEBITO', 500.0, 600.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01', (now() - interval '95 days') - interval '36 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 3000.0, 0.0, 'Aporte del período 1 — USRV04006', (now() - interval '95 days') - interval '36 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO entrega_fondo (grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id, cuenta_destino_id, monto_bolsa_bruto, total_deducciones, monto_neto_a_entregar, monto_efectivamente_entregado, moneda, estado, metodo_desembolso, fecha_programada, fecha_autorizacion, fecha_entrega, autorizada_por, ejecutada_por, comprobante_url, hash_comprobante, observaciones, version) VALUES
      ((SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT t.id FROM turno t JOIN grupo g ON g.id = t.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND t.orden_asignado = 1), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 1), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), NULL, 3000.0, 10.5, 2989.5, 2989.5, 'BOB', 'CONFIRMADA', 'COMPENSACION_INTERNA', (current_date + interval '-125 days'), (now() - interval '95 days') - interval '30 days', (now() - interval '95 days') - interval '30 days', (SELECT id FROM usuario WHERE codigo_publico = 'USR000009'), (SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), 'https://almacen.pasanaku.test/entregas/v04-p1.pdf', encode(digest('entrega-v04-p1', 'sha256'), 'hex'), 'Acreditación en la billetera del beneficiario, sin salida de custodia.', 3)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Las ocho reglas del catálogo mínimo 09 evaluadas, siete bloqueantes y una informativa, todas aprobadas
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO validacion_pre_entrega (entrega_id, regla_id, resultado, valor_esperado, valor_obtenido, es_bloqueante, omitida_por, justificacion_omision, evaluada_en) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'BOLSA_COMPLETA'), 'APROBADA', '3000.00', '3000.00', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'BENEFICIARIO_KYC_VIGENTE'), 'APROBADA', 'VIGENTE', 'VIGENTE', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'CUENTA_DESTINO_VERIFICADA'), 'APROBADA', 'VERIFICADA', 'BILLETERA PROPIA', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'SIN_BLOQUEO_DE_AUTORIDAD'), 'APROBADA', '0 bloqueos', '0 bloqueos', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'DEUDA_PROPIA_DEDUCIDA'), 'APROBADA', '0.00', '0.00', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'COMISION_DEVENGADA'), 'APROBADA', '10.50', '10.50', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'ENCAJE_CUMPLIDO'), 'APROBADA', 'ratio >= 1', 'ratio = 1.000000', TRUE, NULL, NULL, (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM regla_entrega WHERE codigo = 'CONFIRMACION_DATOS'), 'APROBADA', 'confirmado', 'confirmado', FALSE, NULL, NULL, (now() - interval '95 days') - interval '30 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La cotización se congela y se muestra ANTES de cobrar: el usuario ve el número antes de que le toquen la bolsa
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cotizacion_comision (concepto_tarifa_id, tarifario_id, referencia_tipo, referencia_id, monto_base, monto_comision, monto_impuesto, monto_total, moneda, desglose, valida_hasta, mostrada_al_usuario_en, aceptada_en, clave_idempotencia) VALUES
      ((SELECT id FROM concepto_tarifa WHERE codigo = 'COM_ENTREGA' AND tarifario_id = (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1)), (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1), 'ENTREGA_FONDO', (SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 3000.0, 8.81, 1.69, 10.5, 'BOB', '{"tramo": "0 a 3000", "porcentaje": 0.35, "bruto": 10.5, "piso": 10, "techo": 50, "iva": 1.37, "it": 0.32, "neto_plataforma": 8.81}'::jsonb, (now() - interval '95 days') - interval '29 days', (now() - interval '95 days') - interval '31 days', (now() - interval '95 days') - interval '30 days', 'v04-cotizacion-entrega-p1')
    ON CONFLICT (referencia_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO devengo_comision (concepto_tarifa_id, tarifario_id, cotizacion_id, grupo_id, participante_id, usuario_obligado_id, asiento_contable_id, referencia_tipo, referencia_id, monto_base, monto_comision, monto_descuento, monto_impuesto, monto_total, moneda, estado, fecha_devengo, periodo_contable, clave_idempotencia) VALUES
      ((SELECT id FROM concepto_tarifa WHERE codigo = 'COM_ENTREGA' AND tarifario_id = (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1)), (SELECT id FROM tarifario WHERE codigo = 'GENERAL' AND version = 1), (SELECT id FROM cotizacion_comision WHERE clave_idempotencia = 'v04-cotizacion-entrega-p1'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001')), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), NULL, 'ENTREGA_FONDO', (SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 3000.0, 8.81, 0.0, 1.69, 10.5, 'BOB', 'COBRADO', (now() - interval '95 days') - interval '30 days', '2026-01', 'v04-devengo-entrega-p1')
    ON CONFLICT (COALESCE(grupo_id, '00000000-0000-0000-0000-000000000000'::uuid), clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO calculo_impuesto (devengo_id, impuesto_id, base_imponible, alicuota_aplicada, monto_impuesto, incluido_en_precio, periodo_fiscal) VALUES
      ((SELECT id FROM devengo_comision WHERE clave_idempotencia = 'v04-devengo-entrega-p1'), (SELECT id FROM impuesto WHERE codigo = 'IVA' ORDER BY vigente_desde DESC LIMIT 1), 10.5, 0.13, 1.37, TRUE, '2026-01'),
      ((SELECT id FROM devengo_comision WHERE clave_idempotencia = 'v04-devengo-entrega-p1'), (SELECT id FROM impuesto WHERE codigo = 'IT' ORDER BY vigente_desde DESC LIMIT 1), 10.5, 0.03, 0.32, TRUE, '2026-01')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO deduccion_entrega (entrega_id, tipo, descripcion, monto, referencia_origen_id, es_obligatoria, aplicada_en, revertida_en) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'COMISION_PLATAFORMA', 'Comisión por cobro de turno, impuestos incluidos', 10.5, NULL, TRUE, (now() - interval '95 days') - interval '30 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('ENTREGA_DE_FONDO', 'APLICADA', 'BOB', 3000.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'ENTREGA_FONDO', (SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = pe.grupo_id WHERE pe.numero = 1 AND g.codigo_publico = 'GRP-V04-01'), 'BATCH', 'v04-entrega-p1', '', (now() - interval '95 days') - interval '30 days', (now() - interval '95 days') - interval '30 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'DEBITO', 3000.0, 0.0, 0.0, 'Entrega de la bolsa del período 1 al turno 1', (now() - interval '95 days') - interval '30 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), 2, 'CREDITO', 2989.5, 3589.5, 0.0, 'Bolsa del período 1 acreditada, neta de comisión', (now() - interval '95 days') - interval '30 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-INGRESOS'), 3, 'CREDITO', 8.81, 8.81, 0.0, 'Comisión por cobro de turno, neta de impuestos', (now() - interval '95 days') - interval '30 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-IMPUESTOS'), 4, 'CREDITO', 1.69, 1.69, 0.0, 'IVA e IT retenidos sobre la comisión de entrega', (now() - interval '95 days') - interval '30 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cargo_comision (devengo_id, deduccion_entrega_id, transaccion_id, obligacion_id, forma_cobro, monto_cobrado, moneda, estado, intentos, ultimo_error, cobrado_en) VALUES
      ((SELECT id FROM devengo_comision WHERE clave_idempotencia = 'v04-devengo-entrega-p1'), (SELECT d.id FROM deduccion_entrega d JOIN entrega_fondo e ON e.id = d.entrega_id JOIN grupo g ON g.id = e.grupo_id WHERE d.tipo = 'COMISION_PLATAFORMA' AND g.codigo_publico = 'GRP-V04-01'), (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), NULL, 'DEDUCCION_DE_ENTREGA', 10.5, 'BOB', 'COBRADO', 1, NULL, (now() - interval '95 days') - interval '30 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO historial_estado_entrega (entrega_id, estado_anterior, estado_nuevo, motivo, ejecutado_por, fecha_hora) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'PROGRAMADA', 'LISTA_PARA_ENTREGA', 'Las siete validaciones previas bloqueantes dieron APROBADA', NULL, (now() - interval '95 days') - interval '31 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'LISTA_PARA_ENTREGA', 'AUTORIZADA', 'Autorizada por riesgos con el permiso ENTREGA_AUTORIZAR', (SELECT id FROM usuario WHERE codigo_publico = 'USR000009'), (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'AUTORIZADA', 'ENTREGADA', 'Ejecutada por tesorería: acreditación en la billetera del beneficiario', (SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), (now() - interval '95 days') - interval '30 days'),
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'ENTREGADA', 'CONFIRMADA', 'El beneficiario confirmó la recepción desde la app', NULL, (now() - interval '95 days') - interval '29 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO confirmacion_recepcion (entrega_id, estado, monto_confirmado, token_confirmacion_id, confirmada_en, plazo_limite, autoconfirmada_por_vencimiento, ip_confirmacion, comentario) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'CONFIRMADA_POR_BENEFICIARIO', 2989.5, NULL, (now() - interval '95 days') - interval '29 days', (now() - interval '95 days') - interval '23 days', FALSE, '190.129.0.11', 'Recibido conforme.')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO datos_facturacion (usuario_id, tipo_documento, numero_documento, razon_social, email_envio, es_predeterminado, verificado, actualizado_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'CI', '8040001', 'María Elena Quispe Mamani', 'v041@pasanaku.test', TRUE, TRUE, (now() - interval '95 days') - interval '45 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO factura_electronica (devengo_id, usuario_id, datos_facturacion_id, lote_envio_sin_id, evento_significativo_id, nit_emisor, sucursal, punto_venta, numero_factura, cuf, cufd, codigo_control, fecha_emision, monto_total, monto_iva, monto_no_sujeto, moneda, estado_fiscal, url_pdf, url_xml, hash_documento, qr_verificacion, leyenda, anulada_en, motivo_anulacion) VALUES
      ((SELECT id FROM devengo_comision WHERE clave_idempotencia = 'v04-devengo-entrega-p1'), (SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), (SELECT id FROM datos_facturacion WHERE numero_documento = '8040001'), NULL, NULL, '1099887766', 0, 0, 1, 'V04-CUF-0000000000000001', 'V04-CUFD-0000000000000001', NULL, (now() - interval '95 days') - interval '30 days', 10.5, 1.37, 0.0, 'BOB', 'VALIDADA', 'https://almacen.pasanaku.test/facturas/v04-1.pdf', 'https://almacen.pasanaku.test/facturas/v04-1.xml', encode(digest('factura-v04-1', 'sha256'), 'hex'), 'https://siat.impuestos.gob.bo/consulta/QR?nit=1099887766&cuf=V04-CUF-0000000000000001', 'Ley N° 453: El proveedor debe suministrar información completa y veraz.', NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO orden_cobro (obligacion_id, proveedor_id, monto_exacto, moneda, permite_monto_abierto, referencia_unica, estado, emitida_en, expira_en, clave_idempotencia) VALUES
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04001'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-01-V04', 'PAGADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-01'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04002'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-02-V04', 'PAGADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-02'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04003'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-03-V04', 'PAGADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-03'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04004'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-04-V04', 'EXPIRADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-04'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04005'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-05-V04', 'PAGADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-05'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04006'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', FALSE, 'AP-P2-06-V04', 'PAGADA', (now() - interval '95 days') - interval '15 days', (now() - interval '95 days') - interval '5 days', 'v04-orden-cobro-p2-06')
    ON CONFLICT (obligacion_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO qr_cobro (orden_cobro_id, payload_emv, url_imagen, crc, banco_emisor, cuenta_abono, es_reutilizable, escaneos) VALUES
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-01'), '00020101021226580014BO.QR.V040114AP-P2-01-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-01-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 2),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-02'), '00020101021226580014BO.QR.V040114AP-P2-02-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-02-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 2),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-03'), '00020101021226580014BO.QR.V040114AP-P2-03-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-03-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 2),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-04'), '00020101021226580014BO.QR.V040114AP-P2-04-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-04-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 0),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-05'), '00020101021226580014BO.QR.V040114AP-P2-05-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-05-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 2),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-06'), '00020101021226580014BO.QR.V040114AP-P2-06-V04520400005303068540550052.005802BO5910APORTAYA6304', 'https://almacen.pasanaku.test/qr/AP-P2-06-V04.png', 'A1B2', 'BANCO DEMO S.A.', '****4321', FALSE, 2)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO intento_pago (orden_cobro_id, numero_intento, canal, iniciado_en, finalizado_en, estado, codigo_error, mensaje_proveedor, clave_idempotencia) VALUES
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-01'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 'APROBADA', NULL, 'Pago aprobado', 'v04-intento-p2-01'),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-02'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 'APROBADA', NULL, 'Pago aprobado', 'v04-intento-p2-02'),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-03'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '11 days', (now() - interval '95 days') - interval '11 days', 'APROBADA', NULL, 'Pago aprobado', 'v04-intento-p2-03'),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-04'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '6 days', (now() - interval '95 days') - interval '5 days', 'EXPIRADA', 'ORDEN_EXPIRADA', 'La orden venció sin pago', 'v04-intento-p2-04'),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-05'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '13 days', (now() - interval '95 days') - interval '13 days', 'APROBADA', NULL, 'Pago aprobado', 'v04-intento-p2-05'),
      ((SELECT id FROM orden_cobro WHERE clave_idempotencia = 'v04-orden-cobro-p2-06'), 1, 'QR_INTEROPERABLE', (now() - interval '95 days') - interval '10 days', (now() - interval '95 days') - interval '10 days', 'APROBADA', NULL, 'Pago aprobado', 'v04-intento-p2-06')
    ON CONFLICT (orden_cobro_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO pago (obligacion_id, intento_pago_id, proveedor_id, monto, moneda, monto_comision_proveedor, monto_neto_acreditado, canal, estado, fecha_hora_pago, fecha_hora_acreditacion, referencia_proveedor, pagador_nombre, pagador_documento, cuenta_origen_enmascarada, registrado_por, es_manual, clave_idempotencia) VALUES
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04001'), (SELECT id FROM intento_pago WHERE clave_idempotencia = 'v04-intento-p2-01'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', 1.75, 498.25, 'QR_INTEROPERABLE', 'ACREDITADO', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 'BCO-P2-01-V04', 'María Elena Quispe Mamani', '8040001', '****4001-V04', NULL, FALSE, 'v04-pago-p2-01'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04002'), (SELECT id FROM intento_pago WHERE clave_idempotencia = 'v04-intento-p2-02'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', 1.75, 498.25, 'QR_INTEROPERABLE', 'ACREDITADO', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 'BCO-P2-02-V04', 'Juan Carlos Rojas Vargas', '9000002', '****4002', NULL, FALSE, 'v04-pago-p2-02'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04003'), (SELECT id FROM intento_pago WHERE clave_idempotencia = 'v04-intento-p2-03'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', 1.75, 498.25, 'QR_INTEROPERABLE', 'ACREDITADO', (now() - interval '95 days') - interval '11 days', (now() - interval '95 days') - interval '11 days', 'BCO-P2-03-V04', 'Ana Lucía Choque Flores', '9000003', '****4003', NULL, FALSE, 'v04-pago-p2-03'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04005'), (SELECT id FROM intento_pago WHERE clave_idempotencia = 'v04-intento-p2-05'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', 1.75, 498.25, 'QR_INTEROPERABLE', 'ACREDITADO', (now() - interval '95 days') - interval '13 days', (now() - interval '95 days') - interval '13 days', 'BCO-P2-05-V04', 'Rosa Condori Apaza', '9000005', '****4005', NULL, FALSE, 'v04-pago-p2-05'),
      ((SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04006'), (SELECT id FROM intento_pago WHERE clave_idempotencia = 'v04-intento-p2-06'), (SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 500.0, 'BOB', 1.75, 498.25, 'QR_INTEROPERABLE', 'ACREDITADO', (now() - interval '95 days') - interval '10 days', (now() - interval '95 days') - interval '10 days', 'BCO-P2-06-V04', 'Luis Fernando Mendoza Paz', '9000006', '****4006', NULL, FALSE, 'v04-pago-p2-06')
    ON CONFLICT (obligacion_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO webhook_pasarela (proveedor_id, evento, payload_crudo, firma, firma_valida, recibido_en, procesado_en, intentos_procesamiento, estado, clave_idempotencia, error_procesamiento, pago_id) VALUES
      ((SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 'pago.acreditado', '{"referencia": "BCO-P2-01-V04", "monto": 500.0, "moneda": "BOB", "orden": "AP-P2-01-V04", "estado": "APROBADO"}'::jsonb, 'hmac-v04-01', TRUE, (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 1, 'PROCESADO', 'v04-webhook-p2-01', NULL, (SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-01')),
      ((SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 'pago.acreditado', '{"referencia": "BCO-P2-02-V04", "monto": 500.0, "moneda": "BOB", "orden": "AP-P2-02-V04", "estado": "APROBADO"}'::jsonb, 'hmac-v04-02', TRUE, (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days', 1, 'PROCESADO', 'v04-webhook-p2-02', NULL, (SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-02')),
      ((SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 'pago.acreditado', '{"referencia": "BCO-P2-03-V04", "monto": 500.0, "moneda": "BOB", "orden": "AP-P2-03-V04", "estado": "APROBADO"}'::jsonb, 'hmac-v04-03', TRUE, (now() - interval '95 days') - interval '11 days', (now() - interval '95 days') - interval '11 days', 1, 'PROCESADO', 'v04-webhook-p2-03', NULL, (SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-03')),
      ((SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 'pago.acreditado', '{"referencia": "BCO-P2-05-V04", "monto": 500.0, "moneda": "BOB", "orden": "AP-P2-05-V04", "estado": "APROBADO"}'::jsonb, 'hmac-v04-05', TRUE, (now() - interval '95 days') - interval '13 days', (now() - interval '95 days') - interval '13 days', 1, 'PROCESADO', 'v04-webhook-p2-05', NULL, (SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-05')),
      ((SELECT id FROM proveedor_pago WHERE codigo = 'QR_INTEROP'), 'pago.acreditado', '{"referencia": "BCO-P2-06-V04", "monto": 500.0, "moneda": "BOB", "orden": "AP-P2-06-V04", "estado": "APROBADO"}'::jsonb, 'hmac-v04-06', TRUE, (now() - interval '95 days') - interval '10 days', (now() - interval '95 days') - interval '10 days', 1, 'PROCESADO', 'v04-webhook-p2-06', NULL, (SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-06'))
    ON CONFLICT (proveedor_id, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO conciliacion (pago_id, movimiento_bancario_id, estado, metodo, diferencia_monto, conciliado_por, fecha_conciliacion) VALUES
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-01'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-01-V04'), 'CONCILIADO_AUTOMATICO', 'REFERENCIA_EXACTA', 0.0, NULL, (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-02'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-02-V04'), 'CONCILIADO_AUTOMATICO', 'REFERENCIA_EXACTA', 0.0, NULL, (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-03'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-03-V04'), 'CONCILIADO_AUTOMATICO', 'REFERENCIA_EXACTA', 0.0, NULL, (now() - interval '95 days') - interval '11 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-05'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-05-V04'), 'CONCILIADO_AUTOMATICO', 'REFERENCIA_EXACTA', 0.0, NULL, (now() - interval '95 days') - interval '13 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-06'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-06-V04'), 'CONCILIADO_AUTOMATICO', 'REFERENCIA_EXACTA', 0.0, NULL, (now() - interval '95 days') - interval '10 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO constancia_pago (pago_id, codigo_verificacion, hash_contenido, url_publica, url_pdf, fecha_generacion) VALUES
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-01'), 'CONST-P2-01-V04', encode(digest('constancia-p2-01', 'sha256'), 'hex'), 'https://pasanaku.bo/constancia/CONST-P2-01-V04', 'https://almacen.pasanaku.test/constancias/CONST-P2-01-V04.pdf', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-02'), 'CONST-P2-02-V04', encode(digest('constancia-p2-02', 'sha256'), 'hex'), 'https://pasanaku.bo/constancia/CONST-P2-02-V04', 'https://almacen.pasanaku.test/constancias/CONST-P2-02-V04.pdf', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-03'), 'CONST-P2-03-V04', encode(digest('constancia-p2-03', 'sha256'), 'hex'), 'https://pasanaku.bo/constancia/CONST-P2-03-V04', 'https://almacen.pasanaku.test/constancias/CONST-P2-03-V04.pdf', (now() - interval '95 days') - interval '11 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-05'), 'CONST-P2-05-V04', encode(digest('constancia-p2-05', 'sha256'), 'hex'), 'https://pasanaku.bo/constancia/CONST-P2-05-V04', 'https://almacen.pasanaku.test/constancias/CONST-P2-05-V04.pdf', (now() - interval '95 days') - interval '13 days'),
      ((SELECT id FROM pago WHERE clave_idempotencia = 'v04-pago-p2-06'), 'CONST-P2-06-V04', encode(digest('constancia-p2-06', 'sha256'), 'hex'), 'https://pasanaku.bo/constancia/CONST-P2-06-V04', 'https://almacen.pasanaku.test/constancias/CONST-P2-06-V04.pdf', (now() - interval '95 days') - interval '10 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04001'), 'API', 'v04-aporte-p2-01', '', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04002'), 'API', 'v04-aporte-p2-02', '', (now() - interval '95 days') - interval '12 days', (now() - interval '95 days') - interval '12 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04003'), 'API', 'v04-aporte-p2-03', '', (now() - interval '95 days') - interval '11 days', (now() - interval '95 days') - interval '11 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04005'), 'API', 'v04-aporte-p2-05', '', (now() - interval '95 days') - interval '13 days', (now() - interval '95 days') - interval '13 days'),
      ('APORTE_A_GRUPO', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'OBLIGACION_APORTE', (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04006'), 'API', 'v04-aporte-p2-06', '', (now() - interval '95 days') - interval '10 days', (now() - interval '95 days') - interval '10 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'CREDITO', 500.0, 500.0, 0.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04001', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-01'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 500.0, -7900.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04001', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'CREDITO', 500.0, 1000.0, 0.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04002', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 500.0, -8400.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04002', (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'CREDITO', 500.0, 1500.0, 0.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04003', (now() - interval '95 days') - interval '11 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-03'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 500.0, -8900.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04003', (now() - interval '95 days') - interval '11 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'CREDITO', 500.0, 2000.0, 0.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04005', (now() - interval '95 days') - interval '13 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-05'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 500.0, -9400.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04005', (now() - interval '95 days') - interval '13 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 1, 'CREDITO', 500.0, 2500.0, 0.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04006', (now() - interval '95 days') - interval '10 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-06'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), 2, 'DEBITO', 500.0, -9900.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04006', (now() - interval '95 days') - interval '10 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El dinero de los cobros QR entró a la cuenta de custodia: sin esto el encaje no cuadraría
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_custodia (cuenta_custodia_id, movimiento_bancario_id, fecha_valor, tipo, sentido, monto, referencia_bancaria, glosa, conciliado, registrado_en) VALUES
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-01-V04'), (current_date + interval '-107 days'), 'INGRESO', 'CREDITO', 500.0, 'BCO-P2-01-V04', 'Cobro QR del aporte del período 2 — USRV04001', TRUE, (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-02-V04'), (current_date + interval '-107 days'), 'INGRESO', 'CREDITO', 500.0, 'BCO-P2-02-V04', 'Cobro QR del aporte del período 2 — USRV04002', TRUE, (now() - interval '95 days') - interval '12 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-03-V04'), (current_date + interval '-106 days'), 'INGRESO', 'CREDITO', 500.0, 'BCO-P2-03-V04', 'Cobro QR del aporte del período 2 — USRV04003', TRUE, (now() - interval '95 days') - interval '11 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-05-V04'), (current_date + interval '-108 days'), 'INGRESO', 'CREDITO', 500.0, 'BCO-P2-05-V04', 'Cobro QR del aporte del período 2 — USRV04005', TRUE, (now() - interval '95 days') - interval '13 days'),
      ((SELECT id FROM cuenta_custodia WHERE contrato_referencia = 'FID-DEMO-001'), (SELECT id FROM movimiento_bancario WHERE referencia_banco = 'BCO-P2-06-V04'), (current_date + interval '-105 days'), 'INGRESO', 'CREDITO', 500.0, 'BCO-P2-06-V04', 'Cobro QR del aporte del período 2 — USRV04006', TRUE, (now() - interval '95 days') - interval '10 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Por qué acá no hay UPDATE de saldos
-- El saldo NO se escribe desde el seeder: lo mantiene la base.
-- tg_movimiento_sincroniza_saldo recalcula saldo_disponible y saldo_retenido
-- en cada INSERT de movimiento_billetera, y tg_retencion_sincroniza_saldo hace
-- lo mismo al crear o cambiar una retención. Escribirlo a mano acá duplicaría
-- el efecto y es exactamente la clase de defecto que R-BIL-03 previene.
SELECT 1;

-- Estado NOTIFICADO: se detectó, se notificó y corre el plazo de descargo. Todavía no hay sanción.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO registro_incumplimiento (codigo_expediente, usuario_id, participante_id, grupo_id, periodo_id, cupo_id, obligacion_id, entrega_afectada_id, responsable_gestion, tipo, severidad, estado, origen_deteccion, monto_involucrado, monto_recuperado, monto_castigado, dias_mora_al_detectar, dias_mora_actuales, es_reincidencia, numero_reincidencia, afecto_a_la_entrega, detectado_en, notificado_en, fecha_limite_subsanacion, cerrado_en, motivo_cierre, resumen_resolucion, reportado_por, version) VALUES
      ('INC-V04-0001', (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), (SELECT id FROM cupo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 4), (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04004'), NULL, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), 'APORTE_IMPAGO', 'MODERADA', 'NOTIFICADO', 'AUTOMATICO_VENCIMIENTO', 510.0, 0.0, 0.0, 4, 8, FALSE, 1, FALSE, (now() - interval '95 days') - interval '4 days', (now() - interval '95 days') - interval '4 days', (now() - interval '95 days') + interval '3 days', NULL, NULL, NULL, NULL, 2)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La evidencia se arma sola con lo que ya existe: la orden vencida y el saldo insuficiente
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO evidencia_incumplimiento (registro_id, tipo, descripcion, url_archivo, hash_archivo, contenido_estructurado, aportada_por, fecha_hora, es_inmutable) VALUES
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), 'LOG_SISTEMA', 'La orden de cobro AP-P2-04-V04 venció sin pago y el intento quedó en estado EXPIRADA', NULL, NULL, '{"orden": "AP-P2-04-V04", "estado": "EXPIRADA", "intentos": 1}'::jsonb, NULL, (now() - interval '95 days') - interval '5 days', TRUE),
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), 'CAPTURA_ESTADO', 'Saldo disponible de la billetera del titular al vencimiento: Bs 0,00', NULL, NULL, '{"cuenta": "BOB-V040004", "saldo_disponible": 0.0, "fecha": "vencimiento"}'::jsonb, NULL, (now() - interval '95 days') - interval '8 days', TRUE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO historial_estado_incumplimiento (registro_id, estado_anterior, estado_nuevo, motivo, monto_asociado, ejecutado_por, es_automatico, fecha_hora) VALUES
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), NULL, 'DETECTADO', 'Vencimiento de la obligación sin pago, superada la gracia de 3 días', 500.0, NULL, TRUE, (now() - interval '95 days') - interval '4 days'),
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), 'DETECTADO', 'NOTIFICADO', 'Notificación entregada por WhatsApp y correo con plazo de descargo de 5 días hábiles', 510.0, NULL, TRUE, (now() - interval '95 days') - interval '4 days'),
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), 'NOTIFICADO', 'CUBIERTO_POR_GARANTIA', 'El fondo de garantía cubrió el aporte para no frenar la entrega del período', 500.0, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), FALSE, (now() - interval '95 days') - interval '3 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cobertura_incumplimiento (fondo_id, registro_id, obligacion_id, periodo_id, movimiento_fondo_id, asiento_contable_id, aprobada_por, monto_solicitado, monto_cubierto, porcentaje_cobertura, estado, requirio_aprobacion_manual, motivo_rechazo, solicitada_en, aplicada_en) VALUES
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), (SELECT o.id FROM obligacion_aporte o JOIN participante p ON p.id = o.participante_id JOIN usuario u ON u.id = p.usuario_id JOIN periodo pe ON pe.id = o.periodo_id WHERE o.tipo = 'APORTE_PERIODICO' AND pe.numero = 2 AND u.codigo_publico = 'USRV04004'), (SELECT id FROM periodo WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND numero = 2), NULL, NULL, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), 500.0, 500.0, 100.0, 'EN_RECUPERACION', FALSE, NULL, (now() - interval '95 days') - interval '3 days', (now() - interval '95 days') - interval '3 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO transaccion_billetera (tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, clave_idempotencia, hash_registro, ocurrida_en, registrada_en) VALUES
      ('COBERTURA_GARANTIA', 'APLICADA', 'BOB', 500.0, (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'COBERTURA_INCUMPLIMIENTO', (SELECT c.id FROM cobertura_incumplimiento c JOIN fondo_garantia f ON f.id = c.fondo_id JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), 'BATCH', 'v04-cobertura-p2-04', '', (now() - interval '95 days') - interval '3 days', (now() - interval '95 days') - interval '3 days')
    ON CONFLICT (COALESCE(iniciada_por, '00000000-0000-0000-0000-000000000000'::uuid), origen_tipo, clave_idempotencia) DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_billetera (transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en) VALUES
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-cobertura-p2-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), 1, 'DEBITO', 500.0, 100.0, 0.0, 'Cobertura del aporte impago del período 2 — USRV04004', (now() - interval '95 days') - interval '3 days'),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-cobertura-p2-04'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), 2, 'CREDITO', 500.0, 3000.0, 0.0, 'Aporte del período 2 cubierto por el fondo de garantía', (now() - interval '95 days') - interval '3 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_fondo (fondo_id, asiento_contable_id, tipo, monto, saldo_resultante, referencia_tipo, referencia_id, descripcion, fecha, registrado_por) VALUES
      ((SELECT f.id FROM fondo_garantia f JOIN grupo g ON g.id = f.grupo_id WHERE g.codigo_publico = 'GRP-V04-01'), NULL, 'COBERTURA_APLICADA', 500.0, 100.0, 'cobertura_incumplimiento', (SELECT id FROM cobertura_incumplimiento WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), 'Cobertura del aporte impago del período 2 — USRV04004', (now() - interval '95 days') - interval '3 days', (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO deuda_participante (usuario_id, participante_id, grupo_id, registro_id, cobertura_id, acreedor, capital_original, recargos_acumulados, total_abonado, saldo_actual, moneda, estado, es_subrogada, fecha_exigibilidad, fecha_prescripcion, dias_vencida, version) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), (SELECT id FROM cobertura_incumplimiento WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), 'FONDO_GARANTIA', 500.0, 10.0, 0.0, 510.0, 'BOB', 'EN_MORA', TRUE, (current_date + interval '-98 days'), (current_date + interval '1727 days'), 3, 1)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO subrogacion (cobertura_id, deuda_id, acreedor_original, acreedor_subrogado, monto_subrogado, fecha, documento_respaldo_url) VALUES
      ((SELECT id FROM cobertura_incumplimiento WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), (SELECT id FROM deuda_participante WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), 'GRUPO', 'FONDO_GARANTIA', 500.0, (now() - interval '95 days') - interval '3 days', 'https://almacen.pasanaku.test/subrogaciones/INC-V04-0001.pdf')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO gestion_cobranza (registro_id, estrategia_id, gestor_asignado_id, etapa_actual, monto_en_gestion, intentos_contacto, ultimo_contacto_en, proxima_accion_en, estado, abierta_en, cerrada_en) VALUES
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), (SELECT id FROM estrategia_cobranza WHERE etapa = 'TEMPRANA'), NULL, 'TEMPRANA', 510.0, 2, (now() - interval '95 days') - interval '2 days', (now() - interval '95 days') + interval '1 day', 'ACTIVA', (now() - interval '95 days') - interval '4 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO accion_cobranza (gestion_id, notificacion_id, etapa, tipo, canal, resultado, nota_gestor, costo, ejecutada_por, ejecutada_en) VALUES
      ((SELECT id FROM gestion_cobranza WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), NULL, 'TEMPRANA', 'RECORDATORIO_AUTOMATICO', 'WHATSAPP', 'SIN_RESPUESTA', 'Primer recordatorio automático tras el vencimiento', 0.28, NULL, (now() - interval '95 days') - interval '4 days'),
      ((SELECT id FROM gestion_cobranza WHERE registro_id = (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001')), NULL, 'TEMPRANA', 'MENSAJE_DIRECTO', 'WHATSAPP', 'SOLICITA_PLAN', 'El titular responde que cobra el día 15 y pide fraccionar en dos cuotas', 0.28, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), (now() - interval '95 days') - interval '2 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El descargo está presentado y sin resolver: mientras no se resuelva, no puede haber sanción firme
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO descargo_participante (registro_id, participante_id, argumento, evidencias, estado, resolucion, resuelto_por, presentado_en, resuelto_en) VALUES
      ((SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), (SELECT id FROM participante WHERE grupo_id = (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01') AND usuario_id = (SELECT id FROM usuario WHERE codigo_publico = 'USRV04004')), 'Reconozco el atraso. Me retrasaron el pago del servicio que presto y cobro el día 15. Pido plan de pagos en dos cuotas y que no se me expulse del grupo.', '{"adjuntos": [], "declara": "pago comprometido para el día 15"}'::jsonb, 'EN_ANALISIS', NULL, NULL, (now() - interval '95 days') - interval '2 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La alerta acompaña, no castiga: dispara una gestión, nunca una sanción
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO alerta_temprana (usuario_id, grupo_id, codigo, descripcion, severidad, estado, generada_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'PAGA_CADA_VEZ_MAS_TARDE', 'El período 1 lo pagó a los 2 días del corte y el período 2 no lo pagó: la tendencia empeora', 'MEDIA', 'ATENDIDA', (now() - interval '95 days') - interval '6 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Un asiento por transacción, en estado CONFIRMADO. El número es BIGSERIAL: lo asigna la base y no se repite.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO asiento_contable (fecha, glosa, origen_tipo, origen_id, grupo_id, estado, asiento_reversa_id, registrado_por) VALUES
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-01'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-02'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-03'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-04'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-05'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '44 days', 'Emisión de dinero electrónico por recarga', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-recarga-06'), NULL, 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-01'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-02'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-03'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-04'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-05'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '43 days', 'Constitución del fondo de garantía del grupo', 'BILLETERA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-fondo-inicial-06'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-01'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-03'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-04'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-05'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '36 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-06'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '30 days', 'Entrega de la bolsa al beneficiario del turno, neta de comisión', 'ENTREGA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '12 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-01'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '12 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-02'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '11 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-03'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '13 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-05'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '10 days', 'Aporte del participante a la bolsa del grupo', 'PAGO', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p2-06'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL),
      ((now() - interval '95 days') - interval '3 days', 'Cobertura del fondo de garantía por aporte impago', 'COBERTURA', (SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-cobertura-p2-04'), (SELECT id FROM grupo WHERE codigo_publico = 'GRP-V04-01'), 'CONFIRMADO', NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Las partidas de cada asiento. El total del debe iguala al del haber en cada uno; si no, el asiento no debería existir.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO movimiento_contable (asiento_id, cuenta_id, debe, haber, descripcion) VALUES
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 1200.0, 'Recarga acreditada — referencia QR-000001-INI-V04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-01'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 1200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 1200.0, 'Recarga acreditada — referencia QR-000002-INI-V04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-02'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 1200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 02'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 1200.0, 'Recarga acreditada — referencia QR-000003-INI-V04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-03'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 1200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 03'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 600.0, 'Recarga acreditada — referencia QR-000004-INI'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-04'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 600.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 2000.0, 'Recarga acreditada — referencia QR-000005-INI-V04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-05'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 2000.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 05'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 1200.0, 'Recarga acreditada — referencia QR-000006-INI-V04'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-recarga-06'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 1200.0, 0.0, 'Contrapartida en la cuenta puente de custodia — recarga 06'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04001'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04002'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04003'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04004'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04005'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 100.0, 0.0, 'Constitución del fondo de garantía del grupo'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-fondo-inicial-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 0.0, 100.0, 'Aporte inicial al fondo de garantía — USRV04006'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04001'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04002'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04003'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04004'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04005'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 500.0, 0.0, 'Aporte del período 1 al grupo GRP-V04-01'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p1-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 1 — USRV04006'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 3000.0, 0.0, 'Entrega de la bolsa del período 1 al turno 1'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.01'), 0.0, 2989.5, 'Bolsa del período 1 acreditada, neta de comisión'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_contable WHERE codigo = '4.1.01'), 0.0, 8.81, 'Comisión por cobro de turno, neta de impuestos'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_contable WHERE codigo = '2.2.01'), 0.0, 1.37, 'IVA débito fiscal sobre la comisión de entrega'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-entrega-p1'), (SELECT id FROM cuenta_contable WHERE codigo = '2.2.02'), 0.0, 0.32, 'Impuesto a las transacciones sobre la comisión de entrega'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-01'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04001'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-01'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 500.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04001'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-02'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04002'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-02'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 500.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04002'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-03'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04003'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-03'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 500.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04003'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-05'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04005'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-05'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 500.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04005'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-06'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cobrado por QR interoperable — USRV04006'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-aporte-p2-06'), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), 500.0, 0.0, 'Ingreso a custodia por cobro QR del aporte — USRV04006'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-cobertura-p2-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.03'), 500.0, 0.0, 'Cobertura del aporte impago del período 2 — USRV04004'),
      ((SELECT a.id FROM asiento_contable a JOIN transaccion_billetera t ON t.id = a.origen_id WHERE t.clave_idempotencia = 'v04-cobertura-p2-04'), (SELECT id FROM cuenta_contable WHERE codigo = '2.1.02'), 0.0, 500.0, 'Aporte del período 2 cubierto por el fondo de garantía')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Por qué el enlace queda en un solo sentido
-- Intencionalmente NO se actualiza transaccion_billetera.asiento_contable_id.
-- La tabla es append-only (R-AUD-01) y el trigger rechaza cualquier UPDATE, así
-- que el enlace hacia atrás no se puede cerrar desde un seeder. El vínculo vive
-- en asiento_contable.origen_id, que apunta a la transacción y alcanza para
-- reconstruir el libro. En la aplicación, el caso de uso debe crear el asiento
-- ANTES de insertar la transacción y pasarle el id ya generado: después de
-- insertarla, esa columna es inmutable.
SELECT 1;

-- Lo consumido del límite mensual de recarga por cada titular en la ventana en que recargó
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO consumo_limite (cuenta_billetera_id, limite_id, ventana_inicio, ventana_fin, monto_acumulado, cantidad_acumulada, actualizado_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 1200.0, 1, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 1200.0, 1, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 1200.0, 1, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 600.0, 1, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 2000.0, 1, (now() - interval '95 days') - interval '44 days'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), (SELECT id FROM limite_operativo_billetera WHERE concepto = 'RECARGA' AND ventana = 'MES' AND nivel_debida_diligencia = (SELECT nivel_debida_diligencia FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006') LIMIT 1), date_trunc('month', (now() - interval '95 days') - interval '44 days'), date_trunc('month', (now() - interval '95 days') - interval '44 days') + interval '1 month', 1200.0, 1, (now() - interval '95 days') - interval '44 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Cierre de saldos del día anterior, una fila por cuenta. Es append-only: corregir un saldo es agregar el día siguiente, nunca editar este.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO saldo_diario_billetera (cuenta_billetera_id, fecha, saldo_disponible, saldo_retenido, cantidad_movimientos, hash_registro, hash_anterior, cerrado_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), (current_date + interval '-96 days'), 3589.5, 0.0, 4, encode(digest('saldo-diario-BOB-V040001', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), (current_date + interval '-96 days'), 600.0, 0.0, 3, encode(digest('saldo-diario-BOB-V040002', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), (current_date + interval '-96 days'), 600.0, 0.0, 3, encode(digest('saldo-diario-BOB-V040003', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), (current_date + interval '-96 days'), 0.0, 0.0, 3, encode(digest('saldo-diario-BOB-V040004', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), (current_date + interval '-96 days'), 1400.0, 0.0, 3, encode(digest('saldo-diario-BOB-V040005', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), (current_date + interval '-96 days'), 600.0, 0.0, 3, encode(digest('saldo-diario-BOB-V040006', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'FDG-V040001'), (current_date + interval '-96 days'), 100.0, 0.0, 7, encode(digest('saldo-diario-FDG-V040001', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'GRP-V040001'), (current_date + interval '-96 days'), 3000.0, 0.0, 13, encode(digest('saldo-diario-GRP-V040001', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-CUSTODIA'), (current_date + interval '-96 days'), -9900.0, 0.0, 11, encode(digest('saldo-diario-SYS-CUSTODIA', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-IMPUESTOS'), (current_date + interval '-96 days'), 1.69, 0.0, 1, encode(digest('saldo-diario-SYS-IMPUESTOS', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'SYS-INGRESOS'), (current_date + interval '-96 days'), 8.81, 0.0, 1, encode(digest('saldo-diario-SYS-INGRESOS', 'sha256'), 'hex'), NULL, (now() - interval '95 days') - interval '18 hours')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El extracto que el cliente descarga, con su hash para poder verificar después que no se alteró
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO estado_cuenta_billetera (cuenta_billetera_id, periodo_desde, periodo_hasta, saldo_inicial, total_creditos, total_debitos, saldo_final, cantidad_movimientos, url_archivo, hash_archivo, emitido_en, entregado_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040001'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 4189.5, 600.0, 3589.5, 4, 'https://almacen.pasanaku.test/extractos/BOB-V040001.pdf', encode(digest('extracto-BOB-V040001', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 1200.0, 600.0, 600.0, 3, 'https://almacen.pasanaku.test/extractos/BOB-V040002.pdf', encode(digest('extracto-BOB-V040002', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 1200.0, 600.0, 600.0, 3, 'https://almacen.pasanaku.test/extractos/BOB-V040003.pdf', encode(digest('extracto-BOB-V040003', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040004'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 600.0, 600.0, 0.0, 3, 'https://almacen.pasanaku.test/extractos/BOB-V040004.pdf', encode(digest('extracto-BOB-V040004', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 2000.0, 600.0, 1400.0, 3, 'https://almacen.pasanaku.test/extractos/BOB-V040005.pdf', encode(digest('extracto-BOB-V040005', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), (current_date + interval '-140 days'), (current_date + interval '-96 days'), 0.0, 1200.0, 600.0, 600.0, 3, 'https://almacen.pasanaku.test/extractos/BOB-V040006.pdf', encode(digest('extracto-BOB-V040006', 'sha256'), 'hex'), (now() - interval '95 days') - interval '12 hours', (now() - interval '95 days') - interval '12 hours')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Cuentas de destino verificadas por microdepósito. `ck_cuenta_bancaria_sin_claro` rechaza cualquier fila cuyo enmascarado contenga 9 dígitos seguidos o cuyo hash no mida 64 caracteres. `version_llave` dice con qué versión de la llave maestra se cifró el dato: rotar la llave es cifrar de nuevo y subir el número, no perder el acceso a lo viejo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO cuenta_bancaria_beneficiario (usuario_id, tipo_cuenta, entidad_financiera, numero_cuenta_cifrado, hash_numero_cuenta, numero_enmascarado, titular_nombre, titular_documento, moneda, es_principal, estado_verificacion, metodo_verificacion, verificada_en, bloqueada_hasta, version_llave) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'AHORRO', 'BANCO DEMO S.A.', 'enc:v1:v04-cuenta-01', encode(digest('cuenta-beneficiario-01', 'sha256'), 'hex'), '****4001-V04', 'María Elena Quispe Mamani', '8040001', 'BOB', TRUE, 'VERIFICADA', 'MICRODEPOSITO', (now() - interval '95 days') - interval '40 days', NULL, 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'AHORRO', 'BANCO DEMO S.A.', 'enc:v1:v04-cuenta-05', encode(digest('cuenta-beneficiario-05', 'sha256'), 'hex'), '****4005', 'Rosa Condori Apaza', '9000005', 'BOB', TRUE, 'VERIFICADA', 'MICRODEPOSITO', (now() - interval '95 days') - interval '38 days', NULL, 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'CORRIENTE', 'BANCO DEMO S.A.', 'enc:v1:v04-cuenta-02', encode(digest('cuenta-beneficiario-02', 'sha256'), 'hex'), '****4002', 'Juan Carlos Rojas Vargas', '9000002', 'BOB', TRUE, 'PENDIENTE', NULL, NULL, NULL, 1)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Primer intento de pagar la bolsa del período 1 por transferencia bancaria: el banco la devolvió. Recién después se resolvió por compensación interna, que es como quedó registrada la entrega. Nada de esto movió dinero.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO orden_desembolso (entrega_id, proveedor_id, cuenta_destino_id, monto, moneda, estado, referencia_proveedor, glosa, clave_idempotencia, creada_en, acreditada_en) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), (SELECT id FROM proveedor_pago WHERE codigo = 'ACH_INTERBANCARIA'), (SELECT id FROM cuenta_bancaria_beneficiario WHERE numero_enmascarado = '****4001-V04'), 2989.5, 'BOB', 'DEVUELTA_POR_BANCO', 'ACH-V04-000001', 'Entrega período 1 grupo GRP-V04-01', 'v04-desembolso-entrega-p1', (now() - interval '95 days') - interval '31 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Dos intentos con error clasificado. El primero fue un timeout —reintentable—; el segundo, una cuenta inactiva, que no se reintenta y escala a una persona.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO intento_desembolso (orden_desembolso_id, numero_intento, iniciado_en, finalizado_en, resultado, codigo_error, mensaje_proveedor, reintentable_en) VALUES
      ((SELECT id FROM orden_desembolso WHERE clave_idempotencia = 'v04-desembolso-entrega-p1'), 1, (now() - interval '95 days') - interval '31 days', (now() - interval '95 days') - interval '31 days', 'TIMEOUT', 'GATEWAY_TIMEOUT', 'Sin respuesta del banco en 30 segundos', (now() - interval '95 days') - interval '31 days' + interval '15 minutes'),
      ((SELECT id FROM orden_desembolso WHERE clave_idempotencia = 'v04-desembolso-entrega-p1'), 2, (now() - interval '95 days') - interval '31 days' + interval '20 minutes', (now() - interval '95 days') - interval '31 days' + interval '21 minutes', 'FALLIDO', 'CUENTA_INACTIVA', 'La cuenta de destino no admite abonos', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La incidencia que abre el desembolso devuelto: tiene dueño, plazo y cierre documentado.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO incidencia_entrega (entrega_id, tipo, severidad, descripcion, reportada_por, asignada_a, estado, sla_horas, fecha_limite_sla, resolucion, evidencias, abierta_en, resuelta_en) VALUES
      ((SELECT e.id FROM entrega_fondo e JOIN periodo pe ON pe.id = e.periodo_id JOIN grupo g ON g.id = e.grupo_id WHERE g.codigo_publico = 'GRP-V04-01' AND pe.numero = 1), 'DESEMBOLSO_RECHAZADO', 'ALTA', 'El banco devolvió el abono de Bs 2.989,50 por cuenta de destino inactiva. La beneficiaria quedó sin cobrar su turno hasta resolverlo.', (SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), (SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), 'RESUELTA', 24, (now() - interval '95 days') - interval '30 days', 'Se acordó con la beneficiaria acreditar en su billetera de la plataforma. La entrega se ejecutó por compensación interna el mismo día.', '{"intentos": 2, "codigos": ["GATEWAY_TIMEOUT", "CUENTA_INACTIVA"], "orden_desembolso": "v04-desembolso-entrega-p1"}'::jsonb, (now() - interval '95 days') - interval '31 days', (now() - interval '95 days') - interval '30 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- La retención del retiro en curso. Baja el disponible y sube el retenido: el total no cambia, que es lo que impide que el mismo saldo se comprometa dos veces.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO retencion_saldo (cuenta_billetera_id, transaccion_origen_id, liberada_por, motivo, referencia_tipo, referencia_id, monto, estado, expira_en, creada_en, liberada_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), NULL, NULL, 'ANTIFRAUDE', 'orden_retiro', NULL, 900.0, 'VIGENTE', (now() - interval '95 days') + interval '29 days', (now() - interval '95 days') - interval '6 hours', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Retiro en revisión: pasó el segundo factor pero cayó en la ventana de enfriamiento de 24 horas de la política de billetera, y el motor antifraude pidió revisión humana.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO orden_retiro (cuenta_billetera_id, instrumento_destino_id, retencion_id, transaccion_id, aprobada_por, proveedor_id, monto_solicitado, costo_retiro, monto_neto, moneda, estado, mfa_verificado, requiere_doble_aprobacion, ventana_enfriamiento_hasta, referencia_proveedor, clave_idempotencia, solicitada_en, pagada_en, solicitada_por) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), (SELECT id FROM instrumento_fondeo WHERE hash_identificador = encode(digest('v04-instrumento-05', 'sha256'), 'hex')), (SELECT id FROM retencion_saldo WHERE cuenta_billetera_id = (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005') AND estado = 'VIGENTE'), NULL, NULL, (SELECT id FROM proveedor_pago WHERE codigo = 'ACH_INTERBANCARIA'), 900.0, 0.0, 900.0, 'BOB', 'EN_REVISION', TRUE, FALSE, (now() - interval '95 days') + interval '18 hours', NULL, 'v04-retiro-05', (now() - interval '95 days') - interval '6 hours', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'))
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Dos evaluaciones del motor: la del retiro que quedó en revisión y la de un aporte que pasó limpio. Guardar las reglas disparadas es lo que permite explicarle a la persona por qué se le frenó la operación.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO evaluacion_antifraude (transaccion_id, cuenta_billetera_id, revisada_por, motor_version, puntaje_riesgo, decision, reglas_disparadas, latencia_ms, evaluada_en, revisada_en) VALUES
      (NULL, (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040005'), NULL, 'v1', 62.0, 'REVISAR', '{"disparadas": ["VELOCIDAD_RETIROS", "RETIRO_INSTRUMENTO_NUEVO"], "puntajes": {"VELOCIDAD_RETIROS": 30, "RETIRO_INSTRUMENTO_NUEVO": 32}}'::jsonb, 84, (now() - interval '95 days') - interval '6 hours', NULL),
      ((SELECT id FROM transaccion_billetera WHERE clave_idempotencia = 'v04-aporte-p1-02'), (SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040002'), NULL, 'v1', 8.0, 'PERMITIR', '{"disparadas": [], "puntajes": {}}'::jsonb, 41, (now() - interval '95 days') - interval '36 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Orden de autoridad ya levantada. Se conserva con número de oficio y hash del documento: es la prueba de que se cumplió y de cuándo se levantó.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO bloqueo_saldo (cuenta_billetera_id, retencion_id, levantada_por, autoridad, tipo_orden, numero_oficio, monto_bloqueado, alcance, documento_url, hash_documento, estado, recibido_en, vence_en, levantado_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040003'), NULL, (SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), 'JUZGADO', 'RETENCION', 'OF-JUZ-2026-0142-V04', 200.0, 'PARCIAL', 'https://almacen.pasanaku.test/oficios/OF-JUZ-2026-0142-V04.pdf', encode(digest('oficio-juz-2026-0142', 'sha256'), 'hex'), 'LEVANTADO', (now() - interval '95 days') - interval '25 days', NULL, (now() - interval '95 days') - interval '18 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Una cuenta no se cierra con saldo ni con obligaciones abiertas: la solicitud queda EN_VALIDACION hasta que el grupo termine y el saldo tenga destino.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO solicitud_cierre_billetera (cuenta_billetera_id, orden_retiro_id, aprobada_por, motivo, saldo_al_solicitar, destino_saldo, estado, solicitada_en, ejecutada_en) VALUES
      ((SELECT id FROM cuenta_billetera WHERE numero_cuenta = 'BOB-V040006'), NULL, NULL, 'El titular quiere cerrar la billetera al terminar el pasanaku', 600.0, 'RETIRO', 'EN_VALIDACION', (now() - interval '95 days') - interval '5 days', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- El saldo lo movió el trigger, no el seeder
-- La retención de Bs 900 ya movió el saldo sola: al insertar la fila en
-- retencion_saldo, tg_retencion_sincroniza_saldo dejó BOB-V040005 con
-- saldo_disponible 500,00 y saldo_retenido 900,00. El saldo_total sigue
-- siendo 1.400,00 porque es una columna generada.
SELECT 1;

DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO direccion_usuario (usuario_id, departamento, ciudad, zona, detalle, latitud, longitud) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'SANTA CRUZ', 'Santa Cruz de la Sierra', 'Plan Tres Mil', 'Calle 6 esq. Av. Paurito 45', -17.812345, -63.123456),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'SANTA CRUZ', 'Santa Cruz de la Sierra', 'Villa 1ro de Mayo', 'Av. Che Guevara 210', -17.798765, -63.154321),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'LA PAZ', 'El Alto', 'Ciudad Satélite', 'Calle 5 nro. 88', -16.512345, -68.201234),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'SANTA CRUZ', 'Santa Cruz de la Sierra', 'Los Lotes', 'Calle Los Cusis 12', -17.823456, -63.098765),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'COCHABAMBA', 'Cochabamba', 'La Cancha', 'Av. Punata 340, puesto 12', -17.401234, -66.156789),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'SANTA CRUZ', 'Santa Cruz de la Sierra', 'Equipetrol', 'Calle H nro. 15, dpto. 3B', -17.76789, -63.190123)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- `es_pep` en USRV04001 es lo que obliga a la debida diligencia reforzada y a la segunda revisión independiente (R-UIF-10).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO perfil_financiero (usuario_id, ocupacion, ingreso_mensual_declarado, capacidad_aporte_declarada, fuente_ingresos, es_pep, actualizado_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'Comerciante minorista', 8000.0, 1500.0, 'Venta de abarrotes en mercado', TRUE, (now() - interval '95 days') - interval '120 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'Asalariado', 4500.0, 700.0, 'Salario en empresa privada', FALSE, (now() - interval '95 days') - interval '110 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'Asalariada', 3800.0, 600.0, 'Salario en institución educativa', FALSE, (now() - interval '95 days') - interval '100 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'Transportista', 2500.0, 500.0, 'Servicio de transporte propio', FALSE, (now() - interval '95 days') - interval '95 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'Comerciante mayorista', 15000.0, 3000.0, 'Distribución de productos de limpieza', FALSE, (now() - interval '95 days') - interval '90 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'Profesional independiente', 6000.0, 1000.0, 'Honorarios por consultoría', FALSE, (now() - interval '95 days') - interval '85 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Consentimientos con versión, hash del documento, IP y fecha. USRV04004 revocó el de marketing: es lo que hace que sus avisos comerciales se supriman y los obligatorios no.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO consentimiento (usuario_id, tipo, version_documento, hash_documento, otorgado, fecha_hora, ip_origen, agente_usuario, revocado_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '120 days', '190.129.0.11', 'AportaYa/1.0 (Android 14)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'TRATAMIENTO_DATOS', 'v1', encode(digest('tratamiento-datos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '120 days', '190.129.0.11', 'AportaYa/1.0 (Android 14)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '110 days', '190.129.0.12', 'AportaYa/1.0 (Android 14)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '100 days', '190.129.0.13', 'AportaYa/1.0 (iOS 17)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '95 days', '190.129.0.14', 'AportaYa/1.0 (Android 13)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'MARKETING', 'v1', encode(digest('marketing-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '95 days', '190.129.0.14', 'AportaYa/1.0 (Android 13)', (now() - interval '95 days') - interval '20 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '90 days', '190.129.0.15', 'AportaYa/1.0 (Android 14)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'TERMINOS', 'v1', encode(digest('terminos-v1', 'sha256'), 'hex'), TRUE, (now() - interval '95 days') - interval '85 days', '190.129.0.16', 'AportaYa/1.0 (iOS 17)', NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'MARKETING', 'v1', encode(digest('marketing-v1', 'sha256'), 'hex'), FALSE, (now() - interval '95 days') - interval '85 days', '190.129.0.16', 'AportaYa/1.0 (iOS 17)', NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Segundo factor por WhatsApp para casi todos y TOTP para la organizadora, que además es persona expuesta políticamente. El secreto va cifrado: la columna guarda el sobre, no el secreto. `version_llave` dice con qué versión de la llave maestra se cifró el dato: rotar la llave es cifrar de nuevo y subir el número, no perder el acceso a lo viejo.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO factor_mfa (usuario_id, tipo, secreto_cifrado, activo, es_principal, confirmado_en, ultimo_uso_en, version_llave) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'TOTP', 'enc:v1:v04-totp-01', TRUE, TRUE, (now() - interval '95 days') - interval '119 days', (now() - interval '95 days') - interval '1 day', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 'RESPALDO', 'enc:v1:v04-respaldo-01', TRUE, FALSE, (now() - interval '95 days') - interval '119 days', NULL, 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'WHATSAPP', 'enc:v1:v04-wa-02', TRUE, TRUE, (now() - interval '95 days') - interval '109 days', (now() - interval '95 days') - interval '2 days', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 'WHATSAPP', 'enc:v1:v04-wa-03', TRUE, TRUE, (now() - interval '95 days') - interval '99 days', (now() - interval '95 days') - interval '3 days', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'SMS', 'enc:v1:v04-sms-04', TRUE, TRUE, (now() - interval '95 days') - interval '94 days', (now() - interval '95 days') - interval '9 days', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 'WHATSAPP', 'enc:v1:v04-wa-05', TRUE, TRUE, (now() - interval '95 days') - interval '89 days', (now() - interval '95 days') - interval '6 hours', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'WHATSAPP', 'enc:v1:v04-wa-06', TRUE, TRUE, (now() - interval '95 days') - interval '84 days', (now() - interval '95 days') - interval '4 days', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), 'TOTP', 'enc:v1:v04-totp-of-cumplimiento', TRUE, TRUE, (now() - interval '95 days') - interval '150 days', (now() - interval '95 days') - interval '3 hours', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000007'), 'RESPALDO', 'enc:v1:v04-respaldo-07', TRUE, FALSE, (now() - interval '95 days') - interval '150 days', NULL, 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), 'TOTP', 'enc:v1:v04-totp-tesoreria', TRUE, TRUE, (now() - interval '95 days') - interval '150 days', (now() - interval '95 days') - interval '1 day', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000008'), 'RESPALDO', 'enc:v1:v04-respaldo-08', TRUE, FALSE, (now() - interval '95 days') - interval '150 days', NULL, 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000009'), 'TOTP', 'enc:v1:v04-totp-riesgos', TRUE, TRUE, (now() - interval '95 days') - interval '150 days', (now() - interval '95 days') - interval '2 days', 1),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USR000009'), 'RESPALDO', 'enc:v1:v04-respaldo-09', TRUE, FALSE, (now() - interval '95 days') - interval '150 days', NULL, 1)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Cuatro sesiones vigentes y una revocada con motivo. La revocada es la que permite probar que cerrar sesión no borra el rastro.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO sesion (usuario_id, dispositivo_id, refresco_familia_id, iniciada_en, ultima_actividad_en, expira_en, ip_origen, geolocalizacion_aprox, revocada_en, motivo_revocacion) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), (SELECT id FROM dispositivo WHERE huella = encode(digest('demo-dispositivo-01', 'sha256'), 'hex')), NULL, (now() - interval '95 days') - interval '1 day', (now() - interval '95 days') - interval '2 hours', (now() - interval '95 days') + interval '29 days', '190.129.0.11', 'Santa Cruz de la Sierra, BO', NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), (SELECT id FROM dispositivo WHERE huella = encode(digest('demo-dispositivo-02', 'sha256'), 'hex')), NULL, (now() - interval '95 days') - interval '2 days', (now() - interval '95 days') - interval '2 days', (now() - interval '95 days') + interval '28 days', '190.129.0.12', 'Santa Cruz de la Sierra, BO', NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), (SELECT id FROM dispositivo WHERE huella = encode(digest('demo-dispositivo-04', 'sha256'), 'hex')), NULL, (now() - interval '95 days') - interval '9 days', (now() - interval '95 days') - interval '9 days', (now() - interval '95 days') + interval '21 days', '190.129.0.14', 'Santa Cruz de la Sierra, BO', NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), (SELECT id FROM dispositivo WHERE huella = encode(digest('demo-dispositivo-05', 'sha256'), 'hex')), NULL, (now() - interval '95 days') - interval '7 hours', (now() - interval '95 days') - interval '5 hours', (now() - interval '95 days') + interval '29 days', '190.129.0.15', 'Cochabamba, BO', NULL, NULL),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), (SELECT id FROM dispositivo WHERE huella = encode(digest('demo-dispositivo-03', 'sha256'), 'hex')), NULL, (now() - interval '95 days') - interval '30 days', (now() - interval '95 days') - interval '26 days', (now() - interval '95 days') - interval '1 day', '190.129.0.13', 'El Alto, BO', (now() - interval '95 days') - interval '26 days', 'Cierre de sesión solicitado por el titular')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Tres fallos seguidos contra el mismo identificador desde la misma IP, y el cuarto exitoso. Sin filas de fallo no se puede probar el bloqueo por intentos ni la detección de fuerza bruta.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO intento_autenticacion (usuario_id, identificador_usado, fecha_hora, exitoso, motivo_fallo, ip_origen, agente_usuario, huella_dispositivo, puntaje_riesgo) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), '+59179040004', (now() - interval '95 days') - interval '10 days' - interval '4 minutes', FALSE, 'CONTRASENA_INCORRECTA', '181.114.20.55', 'AportaYa/1.0 (Android 13)', encode(digest('demo-dispositivo-04', 'sha256'), 'hex'), 22.0),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), '+59179040004', (now() - interval '95 days') - interval '10 days' - interval '3 minutes', FALSE, 'CONTRASENA_INCORRECTA', '181.114.20.55', 'AportaYa/1.0 (Android 13)', encode(digest('demo-dispositivo-04', 'sha256'), 'hex'), 38.0),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), '+59179040004', (now() - interval '95 days') - interval '10 days' - interval '2 minutes', FALSE, 'CONTRASENA_INCORRECTA', '181.114.20.55', 'AportaYa/1.0 (Android 13)', encode(digest('demo-dispositivo-04', 'sha256'), 'hex'), 55.0),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), '+59179040004', (now() - interval '95 days') - interval '10 days', TRUE, NULL, '181.114.20.55', 'AportaYa/1.0 (Android 13)', encode(digest('demo-dispositivo-04', 'sha256'), 'hex'), 18.0),
      (NULL, '+59171000099', (now() - interval '95 days') - interval '3 days', FALSE, 'USUARIO_INEXISTENTE', '45.190.77.12', 'curl/8.4.0', NULL, 88.0),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), '+59179040005', (now() - interval '95 days') - interval '7 hours', TRUE, NULL, '190.129.0.15', 'AportaYa/1.0 (Android 14)', encode(digest('demo-dispositivo-05', 'sha256'), 'hex'), 12.0)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Contraseñas anteriores, para impedir reutilizarlas. Solo el hash: no se guarda ni se puede recuperar la contraseña.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO historial_credencial (usuario_id, hash_contrasena, reemplazada_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), '$argon2id$v=19$m=65536,t=3,p=4$ZGVtb2hpc3QwNDE$0000000000000000000000000000000000000000000', (now() - interval '95 days') - interval '10 days'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), '$argon2id$v=19$m=65536,t=3,p=4$ZGVtb2hpc3QwMTE$0000000000000000000000000000000000000000000', (now() - interval '95 days') - interval '80 days')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Puntaje calculado con el modelo v1 del catálogo mínimo 16. USRV04004 arrastra el incumplimiento del período 2 y por eso baja de 700 a 615; el resto sube por puntualidad.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO reputacion_usuario (usuario_id, puntaje, indice_puntualidad, total_obligaciones, obligaciones_cumplidas, obligaciones_en_mora, incumplimientos_graves, grupos_completados, version_modelo, calculado_en) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04001'), 745.0, 100.0, 3, 3, 0, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 742.0, 100.0, 3, 3, 0, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04003'), 742.0, 100.0, 3, 3, 0, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 615.0, 66.67, 3, 2, 1, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04005'), 747.0, 100.0, 3, 3, 0, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours'),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 742.0, 100.0, 3, 3, 0, 0, 0, 'v1', (now() - interval '95 days') - interval '12 hours')
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Restricción vigente por incumplimiento: no puede unirse a grupos nuevos mientras la deuda esté abierta. La levanta una persona y el motivo queda escrito.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO restriccion_usuario (usuario_id, tipo, origen, referencia_origen_id, valor_limite, vigente_desde, vigente_hasta, levantada_por, motivo_levantamiento) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'NO_UNIRSE', 'INCUMPLIMIENTO', (SELECT id FROM registro_incumplimiento WHERE codigo_expediente = 'INC-V04-0001'), NULL, (now() - interval '95 days') - interval '3 days', NULL, NULL, NULL)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;

-- Referencias verificadas por llamada. `acepta_ser_avalista` es lo que habilita a la persona a respaldar un cupo, y pesa en el emparejamiento cuando alguien no tiene historial en la plataforma.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO referencia_personal (usuario_id, nombre, telefono, relacion, verificada, verificada_en, acepta_ser_avalista) VALUES
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04004'), 'Teresa Gutiérrez Soto', '+59171000404', 'FAMILIAR', TRUE, (now() - interval '95 days') - interval '94 days', TRUE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04006'), 'Carlos Mendoza Paz', '+59171000606', 'FAMILIAR', TRUE, (now() - interval '95 days') - interval '84 days', FALSE),
      ((SELECT id FROM usuario WHERE codigo_publico = 'USRV04002'), 'Marta Rojas Vargas', '+59171000202', 'LABORAL', FALSE, NULL, FALSE)
    ON CONFLICT DO NOTHING;
  END IF;
END $siembra$;
