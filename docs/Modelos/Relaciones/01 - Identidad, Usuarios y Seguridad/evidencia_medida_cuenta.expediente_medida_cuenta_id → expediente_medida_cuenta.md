---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: evidencia_medida_cuenta
columna: expediente_medida_cuenta_id
destino: expediente_medida_cuenta
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# evidencia_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta

> **[[evidencia_medida_cuenta]]** `.expediente_medida_cuenta_id` → **[[expediente_medida_cuenta]]**

| | |
| --- | --- |
| Entidad origen | [[evidencia_medida_cuenta]] (módulo 01) |
| Entidad destino | [[expediente_medida_cuenta]] (módulo 01) |
| Columna | `expediente_medida_cuenta_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "se sustenta en" |

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
