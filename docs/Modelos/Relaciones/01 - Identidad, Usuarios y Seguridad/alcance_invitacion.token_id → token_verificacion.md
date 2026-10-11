---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: alcance_invitacion
columna: token_id
destino: token_verificacion
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# alcance_invitacion.token_id → token_verificacion

> **[[alcance_invitacion]]** `.token_id` → **[[token_verificacion]]**

| | |
| --- | --- |
| Entidad origen | [[alcance_invitacion]] (módulo 01) |
| Entidad destino | [[token_verificacion]] (módulo 01) |
| Columna | `token_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | sí (columna UNIQUE) |
| Cruza módulos | no |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
