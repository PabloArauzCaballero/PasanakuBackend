---
tags:
  - relacion
  - fk
  - modulo/02-grupos-cupos-turnos-y-gobernanza
  - cross-modulo
origen: enlace_grupo
columna: emisor_id
destino: usuario
modulo_origen: "02"
modulo_destino: "01"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# enlace_grupo.emisor_id → usuario

> **[[enlace_grupo]]** `.emisor_id` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[enlace_grupo]] (módulo 02) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `emisor_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 02 con el 01. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[02_grupos_turnos]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
