---
tags:
  - moc
  - modulo/03-aportes-pagos-qr-y-conciliacion
modulo: "03 — Aportes, Pagos QR y Conciliación"
relaciones_fk: 52
---

# 03 — Aportes, Pagos QR y Conciliación · relaciones

Las **52 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[asiento_contable.asiento_reversa_id → asiento_contable]] | [[asiento_contable]] | — | sí |
| [[asiento_contable.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[asiento_contable.periodo_contable_id → periodo_contable]] | [[periodo_contable]] | ↗ 13 | sí |
| [[asiento_contable.registrado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[cierre_diario.cerrado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[comprobante_manual.pago_id → pago]] | [[pago]] | — | no |
| [[comprobante_manual.revisado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[comprobante_manual.segunda_revision_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[conciliacion.conciliado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[conciliacion.movimiento_bancario_id → movimiento_bancario]] | [[movimiento_bancario]] | — | sí |
| [[conciliacion.pago_id → pago]] | [[pago]] | — | no |
| [[constancia_pago.pago_id → pago]] | [[pago]] | — | no |
| [[cuenta_contable.cuenta_padre_id → cuenta_contable]] | [[cuenta_contable]] | — | sí |
| [[cuenta_contable.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[cuenta_contable.participante_id → participante]] | [[participante]] | ↗ 02 | sí |
| [[disputa_pago.pago_id → pago]] | [[pago]] | — | no |
| [[enlace_pago_rapido.orden_cobro_id → orden_cobro]] | [[orden_cobro]] | — | no |
| [[enlace_pago_rapido.token_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | no |
| [[excepcion_conciliacion.asignada_a → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[excepcion_conciliacion.conciliacion_id → conciliacion]] | [[conciliacion]] | — | no |
| [[extracto_bancario.importado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[extracto_bancario.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | — | no |
| [[historial_estado_pago.pago_id → pago]] | [[pago]] | — | no |
| [[historial_estado_reembolso.ejecutado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[historial_estado_reembolso.reembolso_id → reembolso]] | [[reembolso]] | — | no |
| [[historial_estado_webhook.pago_id → pago]] | [[pago]] | — | sí |
| [[intento_pago.orden_cobro_id → orden_cobro]] | [[orden_cobro]] | — | no |
| [[movimiento_bancario.extracto_id → extracto_bancario]] | [[extracto_bancario]] | — | no |
| [[movimiento_contable.asiento_id → asiento_contable]] | [[asiento_contable]] | — | no |
| [[movimiento_contable.cuenta_id → cuenta_contable]] | [[cuenta_contable]] | — | no |
| [[obligacion_aporte.cupo_id → cupo]] | [[cupo]] | ↗ 02 | no |
| [[obligacion_aporte.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[obligacion_aporte.obligacion_origen_id → obligacion_aporte]] | [[obligacion_aporte]] | — | sí |
| [[obligacion_aporte.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[obligacion_aporte.periodo_id → periodo]] | [[periodo]] | ↗ 02 | no |
| [[obligacion_aporte.plan_regularizacion_id → plan_regularizacion]] | [[plan_regularizacion]] | — | sí |
| [[obligacion_aporte.politica_mora_id → politica_mora]] | [[politica_mora]] | — | sí |
| [[orden_cobro.obligacion_id → obligacion_aporte]] | [[obligacion_aporte]] | — | no |
| [[orden_cobro.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | — | no |
| [[pago.intento_pago_id → intento_pago]] | [[intento_pago]] | — | sí |
| [[pago.obligacion_id → obligacion_aporte]] | [[obligacion_aporte]] | — | no |
| [[pago.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | — | sí |
| [[pago.registrado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[plan_regularizacion.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[plan_regularizacion.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[politica_mora.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[qr_cobro.orden_cobro_id → orden_cobro]] | [[orden_cobro]] | — | no |
| [[reembolso.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[reembolso.pago_id → pago]] | [[pago]] | — | no |
| [[reembolso.solicitado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[webhook_clave.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | — | no |
| [[webhook_pasarela.proveedor_id → proveedor_pago]] | [[proveedor_pago]] | — | no |
