---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: cuenta_servicio
columna: creada_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "uno a uno opcional"
---

# cuenta_servicio.creada_por → usuario

> **[[cuenta_servicio]]** `.creada_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[cuenta_servicio]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `creada_por` — UUID |
| Cardinalidad | uno a uno opcional |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "es la cuenta" |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
