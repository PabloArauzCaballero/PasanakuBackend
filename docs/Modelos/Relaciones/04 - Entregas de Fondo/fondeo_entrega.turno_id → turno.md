---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
  - cross-modulo
origen: fondeo_entrega
columna: turno_id
destino: turno
modulo_origen: "04"
modulo_destino: "02"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# fondeo_entrega.turno_id → turno

> **[[fondeo_entrega]]** `.turno_id` → **[[turno]]**

| | |
| --- | --- |
| Entidad origen | [[fondeo_entrega]] (módulo 04) |
| Entidad destino | [[turno]] (módulo 02) |
| Columna | `turno_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | sí (columna UNIQUE) |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 04 con el 02. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[02_grupos_turnos]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
