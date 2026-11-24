---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: cuenta_servicio
columna: rol_id
destino: rol
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# cuenta_servicio.rol_id → rol

> **[[cuenta_servicio]]** `.rol_id` → **[[rol]]**

| | |
| --- | --- |
| Entidad origen | [[cuenta_servicio]] (módulo 01) |
| Entidad destino | [[rol]] (módulo 01) |
| Columna | `rol_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "acota" |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
