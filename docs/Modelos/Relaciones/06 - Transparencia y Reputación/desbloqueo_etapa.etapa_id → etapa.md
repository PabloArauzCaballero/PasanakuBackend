---
tags:
  - relacion
  - fk
  - modulo/06-transparencia-y-reputacion
origen: desbloqueo_etapa
columna: etapa_id
destino: etapa
modulo_origen: "06"
modulo_destino: "06"
cross_modulo: false
opcional: false
cardinalidad: "uno a uno opcional"
---

# desbloqueo_etapa.etapa_id → etapa

> **[[desbloqueo_etapa]]** `.etapa_id` → **[[etapa]]**

| | |
| --- | --- |
| Entidad origen | [[desbloqueo_etapa]] (módulo 06) |
| Entidad destino | [[etapa]] (módulo 06) |
| Columna | `etapa_id` — UUID |
| Cardinalidad | uno a uno opcional |
| Obligatoria | sí |
| Uno a uno | sí (columna UNIQUE) |
| Cruza módulos | no |
| Semántica | "desbloquea" |

## Ver también

- [[06_transparencia_reputacion]] — justificación de negocio del origen
- [[06_transparencia_reputacion]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
