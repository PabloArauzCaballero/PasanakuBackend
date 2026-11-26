---
tags:
  - relacion
  - fk
  - modulo/01-identidad-usuarios-y-seguridad
origen: cuenta_servicio
columna: revocada_por
destino: usuario
modulo_origen: "01"
modulo_destino: "01"
cross_modulo: false
opcional: true
cardinalidad: "uno a uno opcional"
---

# cuenta_servicio.revocada_por → usuario

> **[[cuenta_servicio]]** `.revocada_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[cuenta_servicio]] (módulo 01) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `revocada_por` — UUID |
| Cardinalidad | uno a uno opcional |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "es la cuenta" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[01_identidad_usuarios]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
