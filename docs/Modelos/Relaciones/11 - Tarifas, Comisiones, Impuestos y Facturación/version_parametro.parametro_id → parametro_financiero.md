---
tags:
  - relacion
  - fk
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
origen: version_parametro
columna: parametro_id
destino: parametro_financiero
modulo_origen: "11"
modulo_destino: "11"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# version_parametro.parametro_id → parametro_financiero

> **[[version_parametro]]** `.parametro_id` → **[[parametro_financiero]]**

| | |
| --- | --- |
| Entidad origen | [[version_parametro]] (módulo 11) |
| Entidad destino | [[parametro_financiero]] (módulo 11) |
| Columna | `parametro_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "se versiona en" |

## Ver también

- [[11_tarifas_comisiones]] — justificación de negocio del origen
- [[11_tarifas_comisiones]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
