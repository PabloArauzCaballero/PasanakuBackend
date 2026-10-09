---
tags:
  - moc
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
relaciones_fk: 99
---

# 08 — Garantía, Incumplimiento, Cobranza y Sanciones · relaciones

Las **99 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[abono_recuperacion.deuda_id → deuda_participante]] | [[deuda_participante]] | — | no |
| [[abono_recuperacion.entrega_id → entrega_fondo]] | [[entrega_fondo]] | ↗ 04 | sí |
| [[abono_recuperacion.movimiento_fondo_id → movimiento_fondo]] | [[movimiento_fondo]] | — | sí |
| [[abono_recuperacion.pago_id → pago]] | [[pago]] | ↗ 03 | sí |
| [[abono_recuperacion.registrado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[accion_cobranza.ejecutada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[accion_cobranza.gestion_id → gestion_cobranza]] | [[gestion_cobranza]] | — | no |
| [[accion_cobranza.notificacion_id → notificacion]] | [[notificacion]] | ↗ 05 | sí |
| [[acuerdo_quita.acuerdo_grupo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[acuerdo_quita.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[acuerdo_quita.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[alerta_temprana.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[alerta_temprana.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[apelacion_sancion.apelante_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[apelacion_sancion.resuelta_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[apelacion_sancion.sancion_id → sancion]] | [[sancion]] | — | no |
| [[aval_participante.avalista_usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[aval_participante.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[aval_participante.participante_avalado_id → participante]] | [[participante]] | ↗ 02 | no |
| [[aval_participante.token_aceptacion_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | sí |
| [[candidato_reemplazo.reemplazo_id → reemplazo_participante]] | [[reemplazo_participante]] | — | no |
| [[candidato_reemplazo.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[castigo_deuda.aprobado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[castigo_deuda.asiento_contable_id → asiento_contable]] | [[asiento_contable]] | ↗ 03 | sí |
| [[castigo_deuda.deuda_id → deuda_participante]] | [[deuda_participante]] | — | no |
| [[cobertura_incumplimiento.aprobada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[cobertura_incumplimiento.asiento_contable_id → asiento_contable]] | [[asiento_contable]] | ↗ 03 | sí |
| [[cobertura_incumplimiento.fondo_id → fondo_garantia]] | [[fondo_garantia]] | — | no |
| [[cobertura_incumplimiento.movimiento_fondo_id → movimiento_fondo]] | [[movimiento_fondo]] | — | sí |
| [[cobertura_incumplimiento.obligacion_id → obligacion_aporte]] | [[obligacion_aporte]] | ↗ 03 | no |
| [[cobertura_incumplimiento.periodo_id → periodo]] | [[periodo]] | ↗ 02 | no |
| [[cobertura_incumplimiento.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[descargo_participante.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[descargo_participante.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[descargo_participante.resuelto_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[deuda_participante.cobertura_id → cobertura_incumplimiento]] | [[cobertura_incumplimiento]] | — | sí |
| [[deuda_participante.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[deuda_participante.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[deuda_participante.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[deuda_participante.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[devolucion_fondo.fondo_id → fondo_garantia]] | [[fondo_garantia]] | — | no |
| [[devolucion_fondo.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[disolucion_anticipada.acuerdo_grupo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[disolucion_anticipada.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[ejecucion_aval.aval_id → aval_participante]] | [[aval_participante]] | — | no |
| [[ejecucion_aval.deuda_id → deuda_participante]] | [[deuda_participante]] | — | no |
| [[ejecucion_aval.pago_id → pago]] | [[pago]] | ↗ 03 | sí |
| [[ejecucion_aval.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[evidencia_incumplimiento.aportada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[evidencia_incumplimiento.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[fondo_garantia.cuenta_contable_id → cuenta_contable]] | [[cuenta_contable]] | ↗ 03 | no |
| [[fondo_garantia.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[fondo_garantia.politica_cobertura_id → politica_cobertura]] | [[politica_cobertura]] | — | no |
| [[gestion_cobranza.estrategia_id → estrategia_cobranza]] | [[estrategia_cobranza]] | — | no |
| [[gestion_cobranza.gestor_asignado_id → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[gestion_cobranza.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[historial_estado_incumplimiento.ejecutado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[historial_estado_incumplimiento.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[historial_incumplimiento_usuario.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[liquidacion_participante.disolucion_id → disolucion_anticipada]] | [[disolucion_anticipada]] | — | no |
| [[liquidacion_participante.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[lista_restriccion_interna.registro_origen_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | sí |
| [[lista_restriccion_interna.retirado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[lista_restriccion_interna.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[matriz_sancion.politica_id → politica_sancion]] | [[politica_sancion]] | — | no |
| [[movimiento_fondo.asiento_contable_id → asiento_contable]] | [[asiento_contable]] | ↗ 03 | sí |
| [[movimiento_fondo.fondo_id → fondo_garantia]] | [[fondo_garantia]] | — | no |
| [[movimiento_fondo.registrado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[plan_contingencia.acuerdo_grupo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[plan_contingencia.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[politica_cobertura.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[politica_sancion.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[promesa_pago.gestion_id → gestion_cobranza]] | [[gestion_cobranza]] | — | no |
| [[promesa_pago.registrada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[reemplazo_participante.acuerdo_grupo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[reemplazo_participante.cupo_id → cupo]] | [[cupo]] | ↗ 02 | no |
| [[reemplazo_participante.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[reemplazo_participante.participante_entrante_id → participante]] | [[participante]] | ↗ 02 | sí |
| [[reemplazo_participante.participante_saliente_id → participante]] | [[participante]] | ↗ 02 | no |
| [[reemplazo_participante.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | sí |
| [[registro_incumplimiento.cupo_id → cupo]] | [[cupo]] | ↗ 02 | sí |
| [[registro_incumplimiento.entrega_afectada_id → entrega_fondo]] | [[entrega_fondo]] | ↗ 04 | sí |
| [[registro_incumplimiento.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[registro_incumplimiento.obligacion_id → obligacion_aporte]] | [[obligacion_aporte]] | ↗ 03 | sí |
| [[registro_incumplimiento.participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[registro_incumplimiento.periodo_id → periodo]] | [[periodo]] | ↗ 02 | sí |
| [[registro_incumplimiento.reportado_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[registro_incumplimiento.responsable_gestion → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[registro_incumplimiento.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[sancion.acuerdo_grupo_id → acuerdo]] | [[acuerdo]] | ↗ 02 | sí |
| [[sancion.aplicada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[sancion.matriz_id → matriz_sancion]] | [[matriz_sancion]] | — | sí |
| [[sancion.participante_id → participante]] | [[participante]] | ↗ 02 | sí |
| [[sancion.registro_id → registro_incumplimiento]] | [[registro_incumplimiento]] | — | no |
| [[sancion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[score_riesgo_incumplimiento.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[score_riesgo_incumplimiento.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[subrogacion.cobertura_id → cobertura_incumplimiento]] | [[cobertura_incumplimiento]] | — | no |
| [[subrogacion.deuda_id → deuda_participante]] | [[deuda_participante]] | — | no |
