---
tags:
  - relacion
  - fk
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
origen: parametro_congelado_grupo
columna: version_parametro_id
destino: version_parametro
modulo_origen: "11"
modulo_destino: "11"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# parametro_congelado_grupo.version_parametro_id → version_parametro

> **[[parametro_congelado_grupo]]** `.version_parametro_id` → **[[version_parametro]]**

| | |
| --- | --- |
| Entidad origen | [[parametro_congelado_grupo]] (módulo 11) |
| Entidad destino | [[version_parametro]] (módulo 11) |
| Columna | `version_parametro_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "queda congelada en" |

## Ver también

- [[11_tarifas_comisiones]] — justificación de negocio del origen
- [[11_tarifas_comisiones]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
