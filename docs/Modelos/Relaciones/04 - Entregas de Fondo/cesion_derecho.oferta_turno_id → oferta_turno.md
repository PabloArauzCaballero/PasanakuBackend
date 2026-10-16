---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
origen: cesion_derecho
columna: oferta_turno_id
destino: oferta_turno
modulo_origen: "04"
modulo_destino: "04"
cross_modulo: false
opcional: false
cardinalidad: "uno a uno opcional"
---

# cesion_derecho.oferta_turno_id → oferta_turno

> **[[cesion_derecho]]** `.oferta_turno_id` → **[[oferta_turno]]**

| | |
| --- | --- |
| Entidad origen | [[cesion_derecho]] (módulo 04) |
| Entidad destino | [[oferta_turno]] (módulo 04) |
| Columna | `oferta_turno_id` — UUID |
| Cardinalidad | uno a uno opcional |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "se liquida en" |

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[04_entregas_fondo]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
