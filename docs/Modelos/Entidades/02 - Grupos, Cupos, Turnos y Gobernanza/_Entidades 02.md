---
tags:
  - moc
  - modulo/02-grupos-cupos-turnos-y-gobernanza
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
entidades: 26
---

# 02 — Grupos, Cupos, Turnos y Gobernanza · entidades

Las **26 tablas** de este módulo. Justificación de negocio en [[02_grupos_turnos]].

[[_Entidades|← Todas las entidades]] · [[Index]]

| Tabla | Columnas | FK sal. | FK ent. |
| --- | --: | --: | --: |
| [[grupo]] | 27 | 1 | 48 |
| [[alta_grupo]] | 7 | 0 | 0 |
| [[configuracion_grupo]] | 10 | 3 | 0 |
| [[reglamento_grupo]] | 10 | 2 | 1 |
| [[aceptacion_reglamento]] | 7 | 3 | 0 |
| [[historial_estado_grupo]] | 7 | 2 | 0 |
| [[participante]] | 13 | 3 | 28 |
| [[cupo]] | 8 | 2 | 7 |
| [[traspaso_cupo]] | 10 | 4 | 0 |
| [[solicitud_retiro]] | 9 | 2 | 0 |
| [[solicitud_ingreso]] | 10 | 3 | 0 |
| [[decision_ingreso]] | 16 | 0 | 0 |
| [[sustitucion_administrador]] | 10 | 0 | 0 |
| [[invitacion]] | 16 | 3 | 0 |
| [[periodo]] | 11 | 1 | 7 |
| [[turno]] | 11 | 4 | 8 |
| [[sorteo_turnos]] | 14 | 2 | 0 |
| [[snapshot_sorteo]] | 10 | 0 | 0 |
| [[solicitud_permuta]] | 11 | 4 | 0 |
| [[dia_no_habil]] | 5 | 1 | 0 |
| [[postulacion_emparejamiento]] | 11 | 1 | 1 |
| [[criterio_emparejamiento]] | 8 | 0 | 1 |
| [[propuesta_grupo]] | 10 | 2 | 1 |
| [[propuesta_postulacion]] | 4 | 2 | 0 |
| [[acuerdo]] | 15 | 2 | 8 |
| [[voto_participante]] | 7 | 2 | 0 |
