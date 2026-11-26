-- Claves foráneas del módulo 01 — Identidad, Usuarios y Seguridad
-- Generado por scripts/generar_ddl.py — no editar a mano.
-- Se aplican después de crear todas las tablas: el modelo tiene
-- referencias circulares entre módulos.
--
-- Cada una se borra si existe antes de crearse: PostgreSQL no tiene
-- ADD CONSTRAINT IF NOT EXISTS, y sql/aplicar.sql se aplica también
-- sobre una base que ya lo tiene. Borrar y volver a crear —en vez de
-- saltear si ya está— es lo que hace que un ON DELETE cambiado en el
-- modelo quede corregido al reaplicar.

ALTER TABLE identidad.alcance_invitacion DROP CONSTRAINT IF EXISTS fk_alcance_invitacion_token_id;
ALTER TABLE identidad.alcance_invitacion
  ADD CONSTRAINT fk_alcance_invitacion_token_id
  FOREIGN KEY (token_id) REFERENCES identidad.token_verificacion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.apelacion_medida_cuenta DROP CONSTRAINT IF EXISTS fk_apelacion_medida_cuenta_apelante_id;
ALTER TABLE identidad.apelacion_medida_cuenta
  ADD CONSTRAINT fk_apelacion_medida_cuenta_apelante_id
  FOREIGN KEY (apelante_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.apelacion_medida_cuenta DROP CONSTRAINT IF EXISTS fk_apelacion_medida_cuenta_expediente_medida_cuenta_id;
ALTER TABLE identidad.apelacion_medida_cuenta
  ADD CONSTRAINT fk_apelacion_medida_cuenta_expediente_medida_cuenta_id
  FOREIGN KEY (expediente_medida_cuenta_id) REFERENCES identidad.expediente_medida_cuenta (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.apelacion_medida_cuenta DROP CONSTRAINT IF EXISTS fk_apelacion_medida_cuenta_resuelta_por;
ALTER TABLE identidad.apelacion_medida_cuenta
  ADD CONSTRAINT fk_apelacion_medida_cuenta_resuelta_por
  FOREIGN KEY (resuelta_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.asignacion_rol DROP CONSTRAINT IF EXISTS fk_asignacion_rol_otorgada_por;
ALTER TABLE identidad.asignacion_rol
  ADD CONSTRAINT fk_asignacion_rol_otorgada_por
  FOREIGN KEY (otorgada_por) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.asignacion_rol DROP CONSTRAINT IF EXISTS fk_asignacion_rol_rol_id;
ALTER TABLE identidad.asignacion_rol
  ADD CONSTRAINT fk_asignacion_rol_rol_id
  FOREIGN KEY (rol_id) REFERENCES identidad.rol (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.asignacion_rol DROP CONSTRAINT IF EXISTS fk_asignacion_rol_usuario_id;
ALTER TABLE identidad.asignacion_rol
  ADD CONSTRAINT fk_asignacion_rol_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.bloqueo_cuenta DROP CONSTRAINT IF EXISTS fk_bloqueo_cuenta_liberada_por;
ALTER TABLE identidad.bloqueo_cuenta
  ADD CONSTRAINT fk_bloqueo_cuenta_liberada_por
  FOREIGN KEY (liberada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.bloqueo_cuenta DROP CONSTRAINT IF EXISTS fk_bloqueo_cuenta_usuario_id;
ALTER TABLE identidad.bloqueo_cuenta
  ADD CONSTRAINT fk_bloqueo_cuenta_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.consentimiento DROP CONSTRAINT IF EXISTS fk_consentimiento_usuario_id;
ALTER TABLE identidad.consentimiento
  ADD CONSTRAINT fk_consentimiento_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.credencial_acceso DROP CONSTRAINT IF EXISTS fk_credencial_acceso_usuario_id;
ALTER TABLE identidad.credencial_acceso
  ADD CONSTRAINT fk_credencial_acceso_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.cuenta_servicio DROP CONSTRAINT IF EXISTS fk_cuenta_servicio_creada_por;
ALTER TABLE identidad.cuenta_servicio
  ADD CONSTRAINT fk_cuenta_servicio_creada_por
  FOREIGN KEY (creada_por) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.cuenta_servicio DROP CONSTRAINT IF EXISTS fk_cuenta_servicio_revocada_por;
ALTER TABLE identidad.cuenta_servicio
  ADD CONSTRAINT fk_cuenta_servicio_revocada_por
  FOREIGN KEY (revocada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.cuenta_servicio DROP CONSTRAINT IF EXISTS fk_cuenta_servicio_rol_id;
ALTER TABLE identidad.cuenta_servicio
  ADD CONSTRAINT fk_cuenta_servicio_rol_id
  FOREIGN KEY (rol_id) REFERENCES identidad.rol (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.cuenta_servicio DROP CONSTRAINT IF EXISTS fk_cuenta_servicio_usuario_id;
ALTER TABLE identidad.cuenta_servicio
  ADD CONSTRAINT fk_cuenta_servicio_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.direccion_usuario DROP CONSTRAINT IF EXISTS fk_direccion_usuario_usuario_id;
ALTER TABLE identidad.direccion_usuario
  ADD CONSTRAINT fk_direccion_usuario_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.dispositivo DROP CONSTRAINT IF EXISTS fk_dispositivo_usuario_id;
ALTER TABLE identidad.dispositivo
  ADD CONSTRAINT fk_dispositivo_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.documento_identidad DROP CONSTRAINT IF EXISTS fk_documento_identidad_usuario_id;
ALTER TABLE identidad.documento_identidad
  ADD CONSTRAINT fk_documento_identidad_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.evidencia_medida_cuenta DROP CONSTRAINT IF EXISTS fk_evidencia_medida_cuenta_aportada_por;
ALTER TABLE identidad.evidencia_medida_cuenta
  ADD CONSTRAINT fk_evidencia_medida_cuenta_aportada_por
  FOREIGN KEY (aportada_por) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.evidencia_medida_cuenta DROP CONSTRAINT IF EXISTS fk_evidencia_medida_cuenta_expediente_medida_cuenta_id;
ALTER TABLE identidad.evidencia_medida_cuenta
  ADD CONSTRAINT fk_evidencia_medida_cuenta_expediente_medida_cuenta_id
  FOREIGN KEY (expediente_medida_cuenta_id) REFERENCES identidad.expediente_medida_cuenta (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.evidencia_step_up_consumida DROP CONSTRAINT IF EXISTS fk_evidencia_step_up_consumida_usuario_id;
ALTER TABLE identidad.evidencia_step_up_consumida
  ADD CONSTRAINT fk_evidencia_step_up_consumida_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.expediente_medida_cuenta DROP CONSTRAINT IF EXISTS fk_expediente_medida_cuenta_decidida_por;
ALTER TABLE identidad.expediente_medida_cuenta
  ADD CONSTRAINT fk_expediente_medida_cuenta_decidida_por
  FOREIGN KEY (decidida_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.expediente_medida_cuenta DROP CONSTRAINT IF EXISTS fk_expediente_medida_cuenta_entrega_liberada_por;
ALTER TABLE identidad.expediente_medida_cuenta
  ADD CONSTRAINT fk_expediente_medida_cuenta_entrega_liberada_por
  FOREIGN KEY (entrega_liberada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.expediente_medida_cuenta DROP CONSTRAINT IF EXISTS fk_expediente_medida_cuenta_politica_medida_cuenta_id;
ALTER TABLE identidad.expediente_medida_cuenta
  ADD CONSTRAINT fk_expediente_medida_cuenta_politica_medida_cuenta_id
  FOREIGN KEY (politica_medida_cuenta_id) REFERENCES identidad.politica_medida_cuenta (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.expediente_medida_cuenta DROP CONSTRAINT IF EXISTS fk_expediente_medida_cuenta_propuesta_por;
ALTER TABLE identidad.expediente_medida_cuenta
  ADD CONSTRAINT fk_expediente_medida_cuenta_propuesta_por
  FOREIGN KEY (propuesta_por) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.expediente_medida_cuenta DROP CONSTRAINT IF EXISTS fk_expediente_medida_cuenta_usuario_id;
ALTER TABLE identidad.expediente_medida_cuenta
  ADD CONSTRAINT fk_expediente_medida_cuenta_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.factor_mfa DROP CONSTRAINT IF EXISTS fk_factor_mfa_usuario_id;
ALTER TABLE identidad.factor_mfa
  ADD CONSTRAINT fk_factor_mfa_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.historial_credencial DROP CONSTRAINT IF EXISTS fk_historial_credencial_usuario_id;
ALTER TABLE identidad.historial_credencial
  ADD CONSTRAINT fk_historial_credencial_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.historial_estado_medida_cuenta DROP CONSTRAINT IF EXISTS fk_historial_estado_medida_cuenta_ejecutado_por;
ALTER TABLE identidad.historial_estado_medida_cuenta
  ADD CONSTRAINT fk_historial_estado_medida_cuenta_ejecutado_por
  FOREIGN KEY (ejecutado_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.historial_estado_medida_cuenta DROP CONSTRAINT IF EXISTS fk_historial_estado_medida_cuenta_expediente_medida_cuenta_id;
ALTER TABLE identidad.historial_estado_medida_cuenta
  ADD CONSTRAINT fk_historial_estado_medida_cuenta_expediente_medida_cuenta_id
  FOREIGN KEY (expediente_medida_cuenta_id) REFERENCES identidad.expediente_medida_cuenta (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.intento_autenticacion DROP CONSTRAINT IF EXISTS fk_intento_autenticacion_usuario_id;
ALTER TABLE identidad.intento_autenticacion
  ADD CONSTRAINT fk_intento_autenticacion_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.intento_validacion_token DROP CONSTRAINT IF EXISTS fk_intento_validacion_token_token_id;
ALTER TABLE identidad.intento_validacion_token
  ADD CONSTRAINT fk_intento_validacion_token_token_id
  FOREIGN KEY (token_id) REFERENCES identidad.token_verificacion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.perfil_financiero DROP CONSTRAINT IF EXISTS fk_perfil_financiero_usuario_id;
ALTER TABLE identidad.perfil_financiero
  ADD CONSTRAINT fk_perfil_financiero_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.politica_medida_cuenta DROP CONSTRAINT IF EXISTS fk_politica_medida_cuenta_aprobada_por;
ALTER TABLE identidad.politica_medida_cuenta
  ADD CONSTRAINT fk_politica_medida_cuenta_aprobada_por
  FOREIGN KEY (aprobada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.preferencia_notificacion DROP CONSTRAINT IF EXISTS fk_preferencia_notificacion_usuario_id;
ALTER TABLE identidad.preferencia_notificacion
  ADD CONSTRAINT fk_preferencia_notificacion_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.referencia_personal DROP CONSTRAINT IF EXISTS fk_referencia_personal_usuario_id;
ALTER TABLE identidad.referencia_personal
  ADD CONSTRAINT fk_referencia_personal_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.reputacion_usuario DROP CONSTRAINT IF EXISTS fk_reputacion_usuario_usuario_id;
ALTER TABLE identidad.reputacion_usuario
  ADD CONSTRAINT fk_reputacion_usuario_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.restriccion_usuario DROP CONSTRAINT IF EXISTS fk_restriccion_usuario_levantada_por;
ALTER TABLE identidad.restriccion_usuario
  ADD CONSTRAINT fk_restriccion_usuario_levantada_por
  FOREIGN KEY (levantada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.restriccion_usuario DROP CONSTRAINT IF EXISTS fk_restriccion_usuario_usuario_id;
ALTER TABLE identidad.restriccion_usuario
  ADD CONSTRAINT fk_restriccion_usuario_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.rol DROP CONSTRAINT IF EXISTS fk_rol_creado_por;
ALTER TABLE identidad.rol
  ADD CONSTRAINT fk_rol_creado_por
  FOREIGN KEY (creado_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.rol_permiso DROP CONSTRAINT IF EXISTS fk_rol_permiso_permiso_id;
ALTER TABLE identidad.rol_permiso
  ADD CONSTRAINT fk_rol_permiso_permiso_id
  FOREIGN KEY (permiso_id) REFERENCES identidad.permiso (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.rol_permiso DROP CONSTRAINT IF EXISTS fk_rol_permiso_rol_id;
ALTER TABLE identidad.rol_permiso
  ADD CONSTRAINT fk_rol_permiso_rol_id
  FOREIGN KEY (rol_id) REFERENCES identidad.rol (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.sesion DROP CONSTRAINT IF EXISTS fk_sesion_dispositivo_id;
ALTER TABLE identidad.sesion
  ADD CONSTRAINT fk_sesion_dispositivo_id
  FOREIGN KEY (dispositivo_id) REFERENCES identidad.dispositivo (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.sesion DROP CONSTRAINT IF EXISTS fk_sesion_usuario_id;
ALTER TABLE identidad.sesion
  ADD CONSTRAINT fk_sesion_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_baja DROP CONSTRAINT IF EXISTS fk_solicitud_baja_usuario_id;
ALTER TABLE identidad.solicitud_baja
  ADD CONSTRAINT fk_solicitud_baja_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_cambio_rol DROP CONSTRAINT IF EXISTS fk_solicitud_cambio_rol_propuesta_por;
ALTER TABLE identidad.solicitud_cambio_rol
  ADD CONSTRAINT fk_solicitud_cambio_rol_propuesta_por
  FOREIGN KEY (propuesta_por) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_cambio_rol DROP CONSTRAINT IF EXISTS fk_solicitud_cambio_rol_resuelta_por;
ALTER TABLE identidad.solicitud_cambio_rol
  ADD CONSTRAINT fk_solicitud_cambio_rol_resuelta_por
  FOREIGN KEY (resuelta_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_cambio_rol DROP CONSTRAINT IF EXISTS fk_solicitud_cambio_rol_rol_id;
ALTER TABLE identidad.solicitud_cambio_rol
  ADD CONSTRAINT fk_solicitud_cambio_rol_rol_id
  FOREIGN KEY (rol_id) REFERENCES identidad.rol (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_recuperacion_acceso DROP CONSTRAINT IF EXISTS fk_solicitud_recuperacion_acceso_decidida_por;
ALTER TABLE identidad.solicitud_recuperacion_acceso
  ADD CONSTRAINT fk_solicitud_recuperacion_acceso_decidida_por
  FOREIGN KEY (decidida_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_recuperacion_acceso DROP CONSTRAINT IF EXISTS fk_solicitud_recuperacion_acceso_solicitada_por;
ALTER TABLE identidad.solicitud_recuperacion_acceso
  ADD CONSTRAINT fk_solicitud_recuperacion_acceso_solicitada_por
  FOREIGN KEY (solicitada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_recuperacion_acceso DROP CONSTRAINT IF EXISTS fk_solicitud_recuperacion_acceso_token_id;
ALTER TABLE identidad.solicitud_recuperacion_acceso
  ADD CONSTRAINT fk_solicitud_recuperacion_acceso_token_id
  FOREIGN KEY (token_id) REFERENCES identidad.token_verificacion (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.solicitud_recuperacion_acceso DROP CONSTRAINT IF EXISTS fk_solicitud_recuperacion_acceso_usuario_id;
ALTER TABLE identidad.solicitud_recuperacion_acceso
  ADD CONSTRAINT fk_solicitud_recuperacion_acceso_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.token_verificacion DROP CONSTRAINT IF EXISTS fk_token_verificacion_dispositivo_id;
ALTER TABLE identidad.token_verificacion
  ADD CONSTRAINT fk_token_verificacion_dispositivo_id
  FOREIGN KEY (dispositivo_id) REFERENCES identidad.dispositivo (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.token_verificacion DROP CONSTRAINT IF EXISTS fk_token_verificacion_politica_id;
ALTER TABLE identidad.token_verificacion
  ADD CONSTRAINT fk_token_verificacion_politica_id
  FOREIGN KEY (politica_id) REFERENCES identidad.politica_token (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.token_verificacion DROP CONSTRAINT IF EXISTS fk_token_verificacion_rotado_de_id;
ALTER TABLE identidad.token_verificacion
  ADD CONSTRAINT fk_token_verificacion_rotado_de_id
  FOREIGN KEY (rotado_de_id) REFERENCES identidad.token_verificacion (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.token_verificacion DROP CONSTRAINT IF EXISTS fk_token_verificacion_usuario_id;
ALTER TABLE identidad.token_verificacion
  ADD CONSTRAINT fk_token_verificacion_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.usuario_interno DROP CONSTRAINT IF EXISTS fk_usuario_interno_usuario_id;
ALTER TABLE identidad.usuario_interno
  ADD CONSTRAINT fk_usuario_interno_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE identidad.verificacion_kyc DROP CONSTRAINT IF EXISTS fk_verificacion_kyc_documento_id;
ALTER TABLE identidad.verificacion_kyc
  ADD CONSTRAINT fk_verificacion_kyc_documento_id
  FOREIGN KEY (documento_id) REFERENCES identidad.documento_identidad (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.verificacion_kyc DROP CONSTRAINT IF EXISTS fk_verificacion_kyc_revisada_por;
ALTER TABLE identidad.verificacion_kyc
  ADD CONSTRAINT fk_verificacion_kyc_revisada_por
  FOREIGN KEY (revisada_por) REFERENCES identidad.usuario (id) ON DELETE SET NULL ON UPDATE CASCADE;

ALTER TABLE identidad.verificacion_kyc DROP CONSTRAINT IF EXISTS fk_verificacion_kyc_usuario_id;
ALTER TABLE identidad.verificacion_kyc
  ADD CONSTRAINT fk_verificacion_kyc_usuario_id
  FOREIGN KEY (usuario_id) REFERENCES identidad.usuario (id) ON DELETE RESTRICT ON UPDATE CASCADE;
