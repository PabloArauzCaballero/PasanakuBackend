---
tags:
  - relacion
  - fk
  - modulo/02-grupos-cupos-turnos-y-gobernanza
origen: solicitud_ingreso
columna: enlace_grupo_id
destino: enlace_grupo
modulo_origen: "02"
modulo_destino: "02"
cross_modulo: false
opcional: true
cardinalidad: "uno a muchos, origen opcional"
---

# solicitud_ingreso.enlace_grupo_id → enlace_grupo

> **[[solicitud_ingreso]]** `.enlace_grupo_id` → **[[enlace_grupo]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_ingreso]] (módulo 02) |
| Entidad destino | [[enlace_grupo]] (módulo 02) |
| Columna | `enlace_grupo_id` — UUID |
| Cardinalidad | uno a muchos, origen opcional |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "origina" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[02_grupos_turnos]] — justificación de negocio del origen
- [[02_grupos_turnos]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
