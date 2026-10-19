---
tags:
  - moc
  - modulo/05-notificaciones-y-comunicaciones
modulo: "05 — Notificaciones y Comunicaciones"
relaciones_fk: 22
---

# 05 — Notificaciones y Comunicaciones · relaciones

Las **22 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[aviso_interno.usuario_id → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[bandeja_entrada.notificacion_id → notificacion]] | [[notificacion]] | — | no |
| [[bandeja_entrada.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[canal_vinculado.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[cola_envio.envio_id → envio_notificacion]] | [[envio_notificacion]] | — | no |
| [[cola_muerta.envio_id → envio_notificacion]] | [[envio_notificacion]] | — | no |
| [[enlace_pago_notificado.notificacion_id → notificacion]] | [[notificacion]] | — | no |
| [[enlace_pago_notificado.orden_cobro_id → orden_cobro]] | [[orden_cobro]] | ↗ 03 | no |
| [[enlace_pago_notificado.token_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | no |
| [[envio_notificacion.canal_vinculado_id → canal_vinculado]] | [[canal_vinculado]] | — | sí |
| [[envio_notificacion.notificacion_id → notificacion]] | [[notificacion]] | — | no |
| [[envio_notificacion.proveedor_id → proveedor_mensajeria]] | [[proveedor_mensajeria]] | — | no |
| [[envio_notificacion.version_plantilla_id → version_plantilla]] | [[version_plantilla]] | — | no |
| [[evento_entrega_mensaje.envio_id → envio_notificacion]] | [[envio_notificacion]] | — | no |
| [[notificacion.evento_id → evento_notificable]] | [[evento_notificable]] | — | no |
| [[notificacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[plantilla_mensaje.evento_id → evento_notificable]] | [[evento_notificable]] | — | no |
| [[programacion_recordatorio.evento_id → evento_notificable]] | [[evento_notificable]] | — | no |
| [[programacion_recordatorio.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[respuesta_entrante.canal_vinculado_id → canal_vinculado]] | [[canal_vinculado]] | — | no |
| [[respuesta_entrante.notificacion_relacionada_id → notificacion]] | [[notificacion]] | — | sí |
| [[version_plantilla.plantilla_id → plantilla_mensaje]] | [[plantilla_mensaje]] | — | no |
