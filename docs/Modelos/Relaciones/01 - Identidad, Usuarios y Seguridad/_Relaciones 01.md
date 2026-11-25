---
tags:
  - moc
  - modulo/01-identidad-usuarios-y-seguridad
modulo: "01 — Identidad, Usuarios y Seguridad"
relaciones_fk: 55
---

# 01 — Identidad, Usuarios y Seguridad · relaciones

Las **55 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[alcance_invitacion.token_id → token_verificacion]] | [[token_verificacion]] | — | no |
| [[apelacion_medida_cuenta.apelante_id → usuario]] | [[usuario]] | — | no |
| [[apelacion_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] | [[expediente_medida_cuenta]] | — | no |
| [[apelacion_medida_cuenta.resuelta_por → usuario]] | [[usuario]] | — | sí |
| [[asignacion_rol.otorgada_por → usuario]] | [[usuario]] | — | no |
| [[asignacion_rol.rol_id → rol]] | [[rol]] | — | no |
| [[asignacion_rol.usuario_id → usuario]] | [[usuario]] | — | no |
| [[bloqueo_cuenta.liberada_por → usuario]] | [[usuario]] | — | sí |
| [[bloqueo_cuenta.usuario_id → usuario]] | [[usuario]] | — | no |
| [[consentimiento.usuario_id → usuario]] | [[usuario]] | — | no |
| [[credencial_acceso.usuario_id → usuario]] | [[usuario]] | — | no |
| [[cuenta_servicio.creada_por → usuario]] | [[usuario]] | — | no |
| [[cuenta_servicio.revocada_por → usuario]] | [[usuario]] | — | sí |
| [[cuenta_servicio.rol_id → rol]] | [[rol]] | — | no |
| [[cuenta_servicio.usuario_id → usuario]] | [[usuario]] | — | no |
| [[direccion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[dispositivo.usuario_id → usuario]] | [[usuario]] | — | no |
| [[documento_identidad.usuario_id → usuario]] | [[usuario]] | — | no |
| [[evidencia_medida_cuenta.aportada_por → usuario]] | [[usuario]] | — | no |
| [[evidencia_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] | [[expediente_medida_cuenta]] | — | no |
| [[evidencia_step_up_consumida.usuario_id → usuario]] | [[usuario]] | — | no |
| [[expediente_medida_cuenta.decidida_por → usuario]] | [[usuario]] | — | sí |
| [[expediente_medida_cuenta.politica_medida_cuenta_id → politica_medida_cuenta]] | [[politica_medida_cuenta]] | — | no |
| [[expediente_medida_cuenta.propuesta_por → usuario]] | [[usuario]] | — | no |
| [[expediente_medida_cuenta.usuario_id → usuario]] | [[usuario]] | — | no |
| [[factor_mfa.usuario_id → usuario]] | [[usuario]] | — | no |
| [[historial_credencial.usuario_id → usuario]] | [[usuario]] | — | no |
| [[historial_estado_medida_cuenta.ejecutado_por → usuario]] | [[usuario]] | — | sí |
| [[historial_estado_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] | [[expediente_medida_cuenta]] | — | no |
| [[intento_autenticacion.usuario_id → usuario]] | [[usuario]] | — | sí |
| [[intento_validacion_token.token_id → token_verificacion]] | [[token_verificacion]] | — | no |
| [[perfil_financiero.usuario_id → usuario]] | [[usuario]] | — | no |
| [[politica_medida_cuenta.aprobada_por → usuario]] | [[usuario]] | — | sí |
| [[preferencia_notificacion.usuario_id → usuario]] | [[usuario]] | — | no |
| [[referencia_personal.usuario_id → usuario]] | [[usuario]] | — | no |
| [[reputacion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[restriccion_usuario.levantada_por → usuario]] | [[usuario]] | — | sí |
| [[restriccion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[rol.creado_por → usuario]] | [[usuario]] | — | sí |
| [[rol_permiso.permiso_id → permiso]] | [[permiso]] | — | no |
| [[rol_permiso.rol_id → rol]] | [[rol]] | — | no |
| [[sesion.dispositivo_id → dispositivo]] | [[dispositivo]] | — | no |
| [[sesion.usuario_id → usuario]] | [[usuario]] | — | no |
| [[solicitud_baja.usuario_id → usuario]] | [[usuario]] | — | no |
| [[solicitud_cambio_rol.propuesta_por → usuario]] | [[usuario]] | — | no |
| [[solicitud_cambio_rol.resuelta_por → usuario]] | [[usuario]] | — | sí |
| [[solicitud_cambio_rol.rol_id → rol]] | [[rol]] | — | sí |
| [[token_verificacion.dispositivo_id → dispositivo]] | [[dispositivo]] | — | sí |
| [[token_verificacion.politica_id → politica_sancion]] | [[politica_sancion]] | ↗ 08 | no |
| [[token_verificacion.rotado_de_id → token_verificacion]] | [[token_verificacion]] | — | sí |
| [[token_verificacion.usuario_id → usuario]] | [[usuario]] | — | sí |
| [[usuario_interno.usuario_id → usuario]] | [[usuario]] | — | no |
| [[verificacion_kyc.documento_id → documento_identidad]] | [[documento_identidad]] | — | sí |
| [[verificacion_kyc.revisada_por → usuario]] | [[usuario]] | — | sí |
| [[verificacion_kyc.usuario_id → usuario]] | [[usuario]] | — | no |
