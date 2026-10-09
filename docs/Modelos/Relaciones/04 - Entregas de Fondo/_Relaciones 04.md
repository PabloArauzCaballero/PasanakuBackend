---
tags:
  - moc
  - modulo/04-entregas-de-fondo
modulo: "04 — Entregas de Fondo"
relaciones_fk: 24
---

# 04 — Entregas de Fondo · relaciones

Las **24 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[confirmacion_recepcion.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[confirmacion_recepcion.token_confirmacion_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | sí |
| [[cuenta_bancaria_beneficiario.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[deduccion_entrega.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[entrega_fondo.autorizada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[entrega_fondo.beneficiario_participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[entrega_fondo.cuenta_destino_id → cuenta_bancaria_beneficiario]] | [[cuenta_bancaria_beneficiario]] | — | sí |
| [[entrega_fondo.cupo_id → cupo]] | [[cupo]] | ↗ 02 | no |
| [[entrega_fondo.ejecutada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[entrega_fondo.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[entrega_fondo.periodo_id → periodo]] | [[periodo]] | ↗ 02 | no |
| [[entrega_fondo.turno_id → turno]] | [[turno]] | ↗ 02 | no |
| [[historial_estado_entrega.ejecutado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[historial_estado_entrega.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[incidencia_entrega.asignada_a → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[incidencia_entrega.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[incidencia_entrega.reportada_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[intento_desembolso.orden_desembolso_id → orden_desembolso]] | [[orden_desembolso]] | — | no |
| [[orden_desembolso.cuenta_destino_id → cuenta_bancaria_beneficiario]] | [[cuenta_bancaria_beneficiario]] | — | no |
| [[orden_desembolso.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[orden_desembolso.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | ↗ 03 | no |
| [[validacion_pre_entrega.entrega_id → entrega_fondo]] | [[entrega_fondo]] | — | no |
| [[validacion_pre_entrega.omitida_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[validacion_pre_entrega.regla_id → regla_entrega]] | [[regla_entrega]] | — | no |
