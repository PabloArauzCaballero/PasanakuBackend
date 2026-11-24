---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
origen: puja_subasta
columna: subasta_cupo_id
destino: subasta_cupo
modulo_origen: "04"
modulo_destino: "04"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# puja_subasta.subasta_cupo_id → subasta_cupo

> **[[puja_subasta]]** `.subasta_cupo_id` → **[[subasta_cupo]]**

| | |
| --- | --- |
| Entidad origen | [[puja_subasta]] (módulo 04) |
| Entidad destino | [[subasta_cupo]] (módulo 04) |
| Columna | `subasta_cupo_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "recibe" |

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[04_entregas_fondo]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
