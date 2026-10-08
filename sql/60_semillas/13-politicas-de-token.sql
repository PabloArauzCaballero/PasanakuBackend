-- Parámetros de emisión y validación de tokens, uno por propósito. Sin la fila, el propósito no puede emitir token: el vencimiento, el largo del código y el tope de intentos son dato, no constante en el código.
-- GENERADO desde seeders/minimos/13-politicas-de-token.json — no editar a mano.

-- OTP de seis digitos para confirmar el correo durante el alta mediante Gmail API.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('VERIFICACION_CORREO', 600, 6, 5, 2, 60, 5, 'CORREO', FALSE, TRUE, '2026-10-07T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- OTP numéricos: código corto, vida corta y pocos intentos. La ventana de 5 minutos y los 3 intentos son el estándar de segundo factor en banca móvil.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('SEGUNDO_FACTOR', 300, 6, 3, 3, 60, 10, 'SMS,WHATSAPP,APP_AUTENTICADORA', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('INICIO_SESION_SIN_CONTRASENA', 300, 6, 3, 3, 60, 10, 'SMS,WHATSAPP', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('VERIFICACION_TELEFONO', 600, 6, 5, 3, 60, 8, 'SMS,WHATSAPP,LLAMADA_VOZ', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('RECUPERACION_CONTRASENA', 900, 8, 3, 2, 120, 5, 'SMS,CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CAMBIO_TELEFONO', 300, 6, 3, 2, 120, 3, 'CORREO', TRUE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CAMBIO_CORREO', 300, 6, 3, 2, 120, 3, 'SMS,WHATSAPP', TRUE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('AUTORIZACION_DISPOSITIVO', 300, 6, 3, 2, 120, 5, 'SMS,WHATSAPP,CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('CONFIRMACION_ENTREGA', 3600, 6, 3, 2, 60, 5, 'SMS,WHATSAPP,PUSH_APP', TRUE, TRUE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- Enlaces firmados: no son adivinables, así que la vida es más larga, pero `longitud_codigo` es la del secreto aleatorio y no un código que alguien tipea.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('VERIFICACION_CORREO', 86400, 32, 5, 2, 300, 5, 'CORREO', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('INVITACION_GRUPO', 604800, 32, 5, 2, 300, 20, 'SMS,WHATSAPP,CORREO', FALSE, FALSE, '2026-01-01T00:00:00-04:00'),
  ('ENLACE_PAGO', 172800, 32, 10, 3, 120, 20, 'SMS,WHATSAPP,PUSH_APP', FALSE, FALSE, '2026-01-01T00:00:00-04:00'),
  ('FIRMA_REGLAMENTO', 259200, 32, 5, 2, 300, 5, 'WHATSAPP,CORREO,PUSH_APP', FALSE, TRUE, '2026-01-01T00:00:00-04:00'),
  ('EXPORTACION_DATOS', 604800, 32, 3, 1, 600, 2, 'CORREO', TRUE, TRUE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;

-- Refresh de sesión del backoffice (ADR-010): viaja en cookie httpOnly + Secure + SameSite estricto, se rota en cada uso y su reuso revoca la familia (trigger R-SEG-09). Vive lo mismo que la sesión (aportaya.acceso.vigencia-sesion, 12 h). Cada uso emite uno nuevo, por eso el tope diario es alto.
INSERT INTO politica_token (proposito, ttl_segundos, longitud_codigo, max_intentos_validacion, max_reenvios_por_hora, cooldown_reenvio_segundos, max_emisiones_por_dia, canales_permitidos, exige_dispositivo_conocido, invalida_anteriores, vigente_desde) VALUES
  ('REFRESCO_SESION', 43200, 64, 1, 0, 0, 200, 'COOKIE_HTTPONLY', FALSE, FALSE, '2026-01-01T00:00:00-04:00')
ON CONFLICT (proposito, vigente_desde) DO NOTHING;
