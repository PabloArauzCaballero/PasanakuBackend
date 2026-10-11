-- Dos cuentas SINTETICAS de prueba para recorrer de punta a punta «A organiza e invita, B acepta o rechaza» contra TEST: A (organizador habilitado) y B (invitado), mas un otorgante sin acceso que solo existe para firmar las asignaciones de rol.
-- GENERADO desde seeders/dev/17-usuarios-e2e-sinteticos.json — no editar a mano.

-- SINTETICO · apto_produccion=false. Tres personas inventadas. nivel_kyc BASICO en A y B es una asignacion de la semilla, no el resultado de una verificacion (ver advertencia).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO usuario (id, codigo_publico, nombres, apellidos, telefono_e164, correo, fecha_nacimiento, estado, nivel_kyc, idioma, zona_horaria, fecha_registro) VALUES
      ('e2e00000-0000-4000-8000-000000000000', 'SINT-E2E-0', 'Otorgante', 'Sintetico E2E (sin acceso)', '+59170009100', 'sint-e2e-0@example.invalid', '1990-01-01', 'SUSPENDIDO', 'NINGUNO', 'es-BO', 'America/La_Paz', now()),
      ('e2e00000-0000-4000-8000-000000000001', 'SINT-E2E-A', 'Cuenta A', 'Sintetica E2E (organizador)', '+59170009101', 'sint-e2e-a@example.invalid', '1990-01-01', 'ACTIVO', 'BASICO', 'es-BO', 'America/La_Paz', now()),
      ('e2e00000-0000-4000-8000-000000000002', 'SINT-E2E-B', 'Cuenta B', 'Sintetica E2E (invitado)', '+59170009102', 'sint-e2e-b@example.invalid', '1990-01-01', 'ACTIVO', 'BASICO', 'es-BO', 'America/La_Paz', now())
    ON CONFLICT (codigo_publico) DO NOTHING;
  END IF;
END $siembra$;

-- A: ORGANIZADOR (trae GRUPO_CREAR y GRUPO_ADMINISTRAR) y PARTICIPANTE; B: PARTICIPANTE. Ambito GLOBAL como en 15-usuarios-dev.json. Nadie se asigna un rol a si mismo (ck_asignacion_no_autoasignada): firma el otorgante sintetico.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO asignacion_rol (id, usuario_id, rol_id, ambito, ambito_id, otorgada_por, otorgada_en, vigente_hasta) VALUES
      ('e2e00000-0000-4000-8000-0000000000a1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), (SELECT id FROM rol WHERE codigo = 'ORGANIZADOR'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL),
      ('e2e00000-0000-4000-8000-0000000000a2', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), (SELECT id FROM rol WHERE codigo = 'PARTICIPANTE'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL),
      ('e2e00000-0000-4000-8000-0000000000a3', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-B'), (SELECT id FROM rol WHERE codigo = 'PARTICIPANTE'), 'GLOBAL', NULL, (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-0'), now() - interval '1 hour', NULL)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;

-- Segundo factor SMS enrolado y confirmado: sin factor, el ingreso de un dispositivo nuevo termina en AP-CU04-04. El secreto es un marcador, no un secreto: el codigo lo decide el perfil local de TEST.
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO factor_mfa (id, usuario_id, tipo, secreto_cifrado, activo, es_principal, confirmado_en, version_llave) VALUES
      ('e2e00000-0000-4000-8000-0000000000b1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), 'SMS', 'enc:v1:sintetico-e2e-sms-a', TRUE, TRUE, now() - interval '1 hour', 1),
      ('e2e00000-0000-4000-8000-0000000000b2', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-B'), 'SMS', 'enc:v1:sintetico-e2e-sms-b', TRUE, TRUE, now() - interval '1 hour', 1)
    ON CONFLICT (id) DO NOTHING;
  END IF;
END $siembra$;

-- SINTETICO · A figura HABILITADO, nivel APRENDIZ, con los limites iniciales de aportaya.organizador (2 grupos, 30000.00): es lo unico que consulta grupos antes de crear un grupo. No hubo postulacion, capacitacion ni contrato (ver advertencia).
DO $siembra$
BEGIN
  IF current_setting('app.dev_sembrado', true) IS DISTINCT FROM 'si' THEN
    INSERT INTO organizador (id, usuario_id, estado, nivel, limite_grupos_simultaneos, limite_monto_administrado, grupos_activos, grupos_historicos, monto_administrado_actual, calificacion_promedio, indice_morosidad_cartera, fecha_postulacion, fecha_habilitacion) VALUES
      ('e2e00000-0000-4000-8000-0000000000c1', (SELECT id FROM usuario WHERE codigo_publico = 'SINT-E2E-A'), 'HABILITADO', 'APRENDIZ', 2, 30000.0, 0, 0, 0, 0, 0, now(), now())
    ON CONFLICT (usuario_id) DO NOTHING;
  END IF;
END $siembra$;
