---
tags:
  - moc
  - modulo/02-grupos-cupos-turnos-y-gobernanza
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
entidades: 22
---

# 02 — Grupos, Cupos, Turnos y Gobernanza · entidades

Las **22 tablas** de este módulo. Justificación de negocio en [[02_grupos_turnos]].

[[_Entidades|← Todas las entidades]] · [[Index]]

| Tabla | Columnas | FK sal. | FK ent. |
| --- | --: | --: | --: |
| [[grupo]] | 27 | 1 | 45 |
| [[configuracion_grupo]] | 10 | 3 | 0 |
| [[reglamento_grupo]] | 10 | 2 | 1 |
| [[aceptacion_reglamento]] | 7 | 3 | 0 |
| [[historial_estado_grupo]] | 7 | 2 | 0 |
| [[participante]] | 13 | 3 | 25 |
| [[cupo]] | 8 | 2 | 6 |
| [[traspaso_cupo]] | 10 | 4 | 0 |
| [[solicitud_retiro]] | 9 | 2 | 0 |
| [[solicitud_ingreso]] | 10 | 3 | 0 |
| [[invitacion]] | 12 | 3 | 0 |
| [[periodo]] | 11 | 1 | 6 |
| [[turno]] | 11 | 4 | 4 |
| [[sorteo_turnos]] | 14 | 2 | 0 |
| [[solicitud_permuta]] | 11 | 4 | 0 |
| [[dia_no_habil]] | 5 | 1 | 0 |
| [[postulacion_emparejamiento]] | 11 | 1 | 1 |
| [[criterio_emparejamiento]] | 8 | 0 | 1 |
| [[propuesta_grupo]] | 10 | 2 | 1 |
| [[propuesta_postulacion]] | 4 | 2 | 0 |
| [[acuerdo]] | 15 | 2 | 8 |
| [[voto_participante]] | 7 | 2 | 0 |
