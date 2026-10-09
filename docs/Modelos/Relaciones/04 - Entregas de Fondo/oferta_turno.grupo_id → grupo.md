---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
  - cross-modulo
origen: oferta_turno
columna: grupo_id
destino: grupo
modulo_origen: "04"
modulo_destino: "02"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# oferta_turno.grupo_id → grupo

> **[[oferta_turno]]** `.grupo_id` → **[[grupo]]**

| | |
| --- | --- |
| Entidad origen | [[oferta_turno]] (módulo 04) |
| Entidad destino | [[grupo]] (módulo 02) |
| Columna | `grupo_id` — UUID |
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
