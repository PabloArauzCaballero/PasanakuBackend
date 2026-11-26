---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: solicitud_cambio_rol
columna: propuesta_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# solicitud_cambio_rol.propuesta_por → usuario

> **[[solicitud_cambio_rol]]** `.propuesta_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_cambio_rol]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `propuesta_por` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "propone" |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
