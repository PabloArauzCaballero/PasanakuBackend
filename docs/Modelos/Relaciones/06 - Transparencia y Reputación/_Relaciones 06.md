---
tags:
  - moc
  - modulo/06-transparencia-y-reputacion
modulo: "06 — Transparencia y Reputación"
relaciones_fk: 22
---

# 06 — Transparencia y Reputación · relaciones

Las **22 claves foráneas** que salen de las tablas de este módulo.

[[_Relaciones|← Todas las relaciones]] · [[Index]]

| Relación | Destino | Cruza | Opcional |
| --- | --- | :-: | :-: |
| [[bloque_transparencia.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[certificado_reputacion.snapshot_id → snapshot_reputacion]] | [[snapshot_reputacion]] | — | no |
| [[certificado_reputacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[componente_score.puntaje_id → puntaje_reputacion]] | [[puntaje_reputacion]] | — | no |
| [[evento_reputacion.grupo_id → grupo]] | [[grupo]] | ↗ 02 | sí |
| [[evento_reputacion.participante_id → participante]] | [[participante]] | ↗ 02 | sí |
| [[evento_reputacion.revertido_por_id → evento_reputacion]] | [[evento_reputacion]] | — | sí |
| [[evento_reputacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[insignia_otorgada.insignia_id → insignia_logro]] | [[insignia_logro]] | — | no |
| [[insignia_otorgada.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[metrica_grupo.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[metrica_grupo.periodo_id → periodo]] | [[periodo]] | ↗ 02 | sí |
| [[peso_factor.modelo_id → modelo_scoring]] | [[modelo_scoring]] | — | no |
| [[puntaje_reputacion.modelo_id → modelo_scoring]] | [[modelo_scoring]] | — | no |
| [[puntaje_reputacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[registro_sellado.bloque_id → bloque_transparencia]] | [[bloque_transparencia]] | — | no |
| [[regla_impacto_evento.modelo_id → modelo_scoring]] | [[modelo_scoring]] | — | no |
| [[resena_participante.autor_participante_id → participante]] | [[participante]] | ↗ 02 | no |
| [[resena_participante.evaluado_usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
| [[resena_participante.grupo_id → grupo]] | [[grupo]] | ↗ 02 | no |
| [[resena_participante.moderada_por → usuario]] | [[usuario]] | ↗ 01 | sí |
| [[snapshot_reputacion.usuario_id → usuario]] | [[usuario]] | ↗ 01 | no |
