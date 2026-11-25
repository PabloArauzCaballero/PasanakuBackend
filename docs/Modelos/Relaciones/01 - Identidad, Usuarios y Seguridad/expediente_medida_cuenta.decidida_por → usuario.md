---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: expediente_medida_cuenta
columna: decidida_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: true
cardinalidad: "uno a muchos (0..N)"
---

# expediente_medida_cuenta.decidida_por → usuario

> **[[expediente_medida_cuenta]]** `.decidida_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[expediente_medida_cuenta]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `decidida_por` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "es objeto de" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
