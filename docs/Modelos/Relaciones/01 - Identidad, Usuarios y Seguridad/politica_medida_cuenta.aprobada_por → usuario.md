---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: politica_medida_cuenta
columna: aprobada_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: true
cardinalidad: "no declarada en el diagrama"
---

# politica_medida_cuenta.aprobada_por → usuario

> **[[politica_medida_cuenta]]** `.aprobada_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[politica_medida_cuenta]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `aprobada_por` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
