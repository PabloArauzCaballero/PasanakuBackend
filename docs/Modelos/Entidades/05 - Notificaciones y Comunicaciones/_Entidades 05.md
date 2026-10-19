---
tags:
  - moc
  - modulo/05-notificaciones-y-comunicaciones
modulo: "05 — Notificaciones y Comunicaciones"
entidades: 16
---

# 05 — Notificaciones y Comunicaciones · entidades

Las **16 tablas** de este módulo. Justificación de negocio en [[05_notificaciones]].

[[_Entidades|← Todas las entidades]] · [[Index]]

| Tabla | Columnas | FK sal. | FK ent. |
| --- | --: | --: | --: |
| [[evento_notificable]] | 12 | 0 | 3 |
| [[plantilla_mensaje]] | 9 | 1 | 1 |
| [[version_plantilla]] | 11 | 1 | 1 |
| [[proveedor_mensajeria]] | 11 | 0 | 1 |
| [[canal_vinculado]] | 12 | 1 | 2 |
| [[lista_supresion]] | 8 | 0 | 0 |
| [[notificacion]] | 11 | 2 | 5 |
| [[envio_notificacion]] | 22 | 4 | 3 |
| [[evento_entrega_mensaje]] | 8 | 1 | 0 |
| [[cola_envio]] | 6 | 1 | 0 |
| [[cola_muerta]] | 6 | 1 | 0 |
| [[enlace_pago_notificado]] | 9 | 3 | 0 |
| [[respuesta_entrante]] | 8 | 2 | 0 |
| [[programacion_recordatorio]] | 9 | 2 | 0 |
| [[bandeja_entrada]] | 9 | 2 | 0 |
| [[aviso_interno]] | 9 | 1 | 0 |
