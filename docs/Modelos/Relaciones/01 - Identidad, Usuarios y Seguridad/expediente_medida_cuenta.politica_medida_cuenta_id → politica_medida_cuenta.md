---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: expediente_medida_cuenta
columna: politica_medida_cuenta_id
destino: politica_medida_cuenta
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# expediente_medida_cuenta.politica_medida_cuenta_id → politica_medida_cuenta

> **[[expediente_medida_cuenta]]** `.politica_medida_cuenta_id` → **[[politica_medida_cuenta]]**

| | |
| --- | --- |
| Entidad origen | [[expediente_medida_cuenta]] (módulo 01) |
| Entidad destino | [[politica_medida_cuenta]] (módulo 01) |
| Columna | `politica_medida_cuenta_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "fija los plazos de" |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
