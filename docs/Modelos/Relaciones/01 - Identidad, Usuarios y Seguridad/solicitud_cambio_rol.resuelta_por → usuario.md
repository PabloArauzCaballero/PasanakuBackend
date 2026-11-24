---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: solicitud_cambio_rol
columna: resuelta_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: true
cardinalidad: "uno a muchos (0..N)"
---

# solicitud_cambio_rol.resuelta_por → usuario

> **[[solicitud_cambio_rol]]** `.resuelta_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_cambio_rol]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `resuelta_por` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "propone" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
