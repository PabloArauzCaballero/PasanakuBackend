---
tags:
  - relacion
  - fk
  - modulo/09-auditoria-reportes-y-cumplimiento
origen: ficha_gobierno_tabla
columna: politica_retencion_id
destino: politica_retencion
modulo_origen: "09"
modulo_destino: "09"
cross_modulo: false
opcional: true
cardinalidad: "uno a muchos (0..N)"
---

# ficha_gobierno_tabla.politica_retencion_id → politica_retencion

> **[[ficha_gobierno_tabla]]** `.politica_retencion_id` → **[[politica_retencion]]**

| | |
| --- | --- |
| Entidad origen | [[ficha_gobierno_tabla]] (módulo 09) |
| Entidad destino | [[politica_retencion]] (módulo 09) |
| Columna | `politica_retencion_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "fija la retencion de" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[09_auditoria_reportes]] — justificación de negocio del origen
- [[09_auditoria_reportes]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
