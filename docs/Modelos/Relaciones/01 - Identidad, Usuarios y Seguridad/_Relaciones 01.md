---
tags:
  - moc
  - modulo/01-identidad-usuarios-y-seguridad
modulo: "01 — Identidad, Usuarios y Seguridad"
relaciones_fk: 32
---

# 01 — Identidad, Usuarios y Seguridad · relaciones

Las **32 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[asignacion_rol.otorgada_por → usuario]] | [[usuario]] | — | no |
| [[asignacion_rol.rol_id → rol]] | [[rol]] | — | no |
| [[asignacion_rol.usuario_id → usuario]] | [[usuario]] | — | no |
| [[bloqueo_cuenta.liberada_por → usuario]] | [[usuario]] | — | sí |
| [[bloqueo_cuenta.usuario_id → usuario]] | [[usuario]] | — | no |
| [[consentimiento.usuario_id → usuario]] | [[usuario]] | — | no |
| [[credencial_acceso.usuario_id → usuario]] | [[usuario]] | — | no |
| [[direccion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[dispositivo.usuario_id → usuario]] | [[usuario]] | — | no |
| [[documento_identidad.usuario_id → usuario]] | [[usuario]] | — | no |
| [[factor_mfa.usuario_id → usuario]] | [[usuario]] | — | no |
| [[historial_credencial.usuario_id → usuario]] | [[usuario]] | — | no |
| [[intento_autenticacion.usuario_id → usuario]] | [[usuario]] | — | sí |
| [[intento_validacion_token.token_id → token_verificacion]] | [[token_verificacion]] | — | no |
| [[perfil_financiero.usuario_id → usuario]] | [[usuario]] | — | no |
| [[preferencia_notificacion.usuario_id → usuario]] | [[usuario]] | — | no |
| [[referencia_personal.usuario_id → usuario]] | [[usuario]] | — | no |
| [[reputacion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[restriccion_usuario.levantada_por → usuario]] | [[usuario]] | — | sí |
| [[restriccion_usuario.usuario_id → usuario]] | [[usuario]] | — | no |
| [[rol_permiso.permiso_id → permiso]] | [[permiso]] | — | no |
| [[rol_permiso.rol_id → rol]] | [[rol]] | — | no |
| [[sesion.dispositivo_id → dispositivo]] | [[dispositivo]] | — | no |
| [[sesion.usuario_id → usuario]] | [[usuario]] | — | no |
| [[solicitud_baja.usuario_id → usuario]] | [[usuario]] | — | no |
| [[token_verificacion.dispositivo_id → dispositivo]] | [[dispositivo]] | — | sí |
| [[token_verificacion.politica_id → politica_sancion]] | [[politica_sancion]] | ↗ 08 | no |
| [[token_verificacion.rotado_de_id → token_verificacion]] | [[token_verificacion]] | — | sí |
| [[token_verificacion.usuario_id → usuario]] | [[usuario]] | — | sí |
| [[verificacion_kyc.documento_id → documento_identidad]] | [[documento_identidad]] | — | sí |
| [[verificacion_kyc.revisada_por → usuario]] | [[usuario]] | — | sí |
| [[verificacion_kyc.usuario_id → usuario]] | [[usuario]] | — | no |
