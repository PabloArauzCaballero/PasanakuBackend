---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: apelacion_medida_cuenta
columna: apelante_id
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# apelacion_medida_cuenta.apelante_id → usuario

> **[[apelacion_medida_cuenta]]** `.apelante_id` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[apelacion_medida_cuenta]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `apelante_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
