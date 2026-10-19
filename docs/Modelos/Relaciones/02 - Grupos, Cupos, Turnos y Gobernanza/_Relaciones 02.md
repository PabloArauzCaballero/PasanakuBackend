---
tags:
  - moc
  - modulo/02-grupos-cupos-turnos-y-gobernanza
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
relaciones_fk: 52
---

# 02 — Grupos, Cupos, Turnos y Gobernanza · relaciones

Las **52 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[aceptacion_reglamento.participante_id → participante]] | [[participante]] | — | no |
| [[aceptacion_reglamento.reglamento_id → reglamento_grupo]] | [[reglamento_grupo]] | — | no |
| [[aceptacion_reglamento.token_firma_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | sí |
| [[acuerdo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[acuerdo.propuesto_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[configuracion_grupo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[configuracion_grupo.politica_mora_id → politica_mora]] | [[politica_mora]] | ↗ 03 | sí |
| [[configuracion_grupo.politica_sancion_id → politica_sancion]] | [[politica_sancion]] | ↗ 08 | sí |
| [[cupo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[cupo.participante_id → participante]] | [[participante]] | — | sí |
| [[dia_no_habil.grupo_id → grupo]] | [[grupo]] | — | sí |
| [[enlace_grupo.emisor_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[enlace_grupo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[grupo.organizador_id → organizador]] | [[organizador]] | ↗ 07 | sí |
| [[historial_estado_grupo.ejecutado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[historial_estado_grupo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[invitacion.emisor_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[invitacion.grupo_id → grupo]] | [[grupo]] | — | no |
| [[invitacion.token_id → token_verificacion]] | [[token_verificacion]] | ↗ 01 | no |
| [[participante.grupo_id → grupo]] | [[grupo]] | — | no |
| [[participante.invitado_por_id → participante]] | [[participante]] | — | sí |
| [[participante.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[periodo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[postulacion_emparejamiento.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[propuesta_grupo.criterio_id → criterio_emparejamiento]] | [[criterio_emparejamiento]] | — | no |
| [[propuesta_grupo.grupo_materializado_id → grupo]] | [[grupo]] | — | sí |
| [[propuesta_postulacion.postulacion_id → postulacion_emparejamiento]] | [[postulacion_emparejamiento]] | — | no |
| [[propuesta_postulacion.propuesta_id → propuesta_grupo]] | [[propuesta_grupo]] | — | no |
| [[reglamento_grupo.grupo_id → grupo]] | [[grupo]] | — | no |
| [[reglamento_grupo.redactado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[solicitud_ingreso.enlace_grupo_id → enlace_grupo]] | [[enlace_grupo]] | — | sí |
| [[solicitud_ingreso.grupo_id → grupo]] | [[grupo]] | — | no |
| [[solicitud_ingreso.revisada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[solicitud_ingreso.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[solicitud_permuta.contraparte_id → participante]] | [[participante]] | — | no |
| [[solicitud_permuta.solicitante_id → participante]] | [[participante]] | — | no |
| [[solicitud_permuta.turno_destino_id → turno]] | [[turno]] | — | no |
| [[solicitud_permuta.turno_origen_id → turno]] | [[turno]] | — | no |
| [[solicitud_retiro.participante_id → participante]] | [[participante]] | — | no |
| [[solicitud_retiro.plan_regularizacion_id → plan_regularizacion]] | [[plan_regularizacion]] | ↗ 03 | sí |
| [[sorteo_turnos.ejecutado_por → usuario]] | [[usuario]] | ↗ 01 | no |
| [[sorteo_turnos.grupo_id → grupo]] | [[grupo]] | — | no |
| [[traspaso_cupo.aprobado_por_acuerdo_id → acuerdo]] | [[acuerdo]] | — | sí |
| [[traspaso_cupo.cupo_id → cupo]] | [[cupo]] | — | no |
| [[traspaso_cupo.participante_destino_id → participante]] | [[participante]] | — | no |
| [[traspaso_cupo.participante_origen_id → participante]] | [[participante]] | — | no |
| [[turno.cupo_id → cupo]] | [[cupo]] | — | no |
| [[turno.grupo_id → grupo]] | [[grupo]] | — | no |
| [[turno.periodo_id → periodo]] | [[periodo]] | — | no |
| [[turno.permutado_con_turno_id → turno]] | [[turno]] | — | sí |
| [[voto_participante.acuerdo_id → acuerdo]] | [[acuerdo]] | — | no |
| [[voto_participante.participante_id → participante]] | [[participante]] | — | no |
