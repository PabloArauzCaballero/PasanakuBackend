---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
  - cross-modulo
origen: cesion_derecho
columna: participante_origen_id
destino: participante
modulo_origen: "04"
modulo_destino: "02"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# cesion_derecho.participante_origen_id → participante

> **[[cesion_derecho]]** `.participante_origen_id` → **[[participante]]**

| | |
| --- | --- |
| Entidad origen | [[cesion_derecho]] (módulo 04) |
| Entidad destino | [[participante]] (módulo 02) |
| Columna | `participante_origen_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 04 con el 02. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[02_grupos_turnos]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
