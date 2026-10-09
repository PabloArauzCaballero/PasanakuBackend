---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
origen: fondeo_entrega
columna: entrega_id
destino: entrega_fondo
modulo_origen: "04"
modulo_destino: "04"
cross_modulo: false
opcional: false
cardinalidad: "uno a uno opcional"
---

# fondeo_entrega.entrega_id → entrega_fondo

> **[[fondeo_entrega]]** `.entrega_id` → **[[entrega_fondo]]**

| | |
| --- | --- |
| Entidad origen | [[fondeo_entrega]] (módulo 04) |
| Entidad destino | [[entrega_fondo]] (módulo 04) |
| Columna | `entrega_id` — UUID |
| Cardinalidad | uno a uno opcional |
| Obligatoria | sí |
| Uno a uno | sí (columna UNIQUE) |
| Cruza módulos | no |
| Semántica | "se fondea con" |

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[04_entregas_fondo]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
