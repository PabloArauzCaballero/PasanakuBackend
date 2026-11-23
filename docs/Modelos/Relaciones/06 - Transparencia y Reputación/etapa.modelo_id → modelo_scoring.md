---
tags:
  - relacion
  - fk
  - modulo/06-transparencia-y-reputacion
origen: etapa
columna: modelo_id
destino: modelo_scoring
modulo_origen: "06"
modulo_destino: "06"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (1..N)"
---

# etapa.modelo_id → modelo_scoring

> **[[etapa]]** `.modelo_id` → **[[modelo_scoring]]**

| | |
| --- | --- |
| Entidad origen | [[etapa]] (módulo 06) |
| Entidad destino | [[modelo_scoring]] (módulo 06) |
| Columna | `modelo_id` — UUID |
| Cardinalidad | uno a muchos (1..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "parte su escala en" |

## Ver también

- [[06_transparencia_reputacion]] — justificación de negocio del origen
- [[06_transparencia_reputacion]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
