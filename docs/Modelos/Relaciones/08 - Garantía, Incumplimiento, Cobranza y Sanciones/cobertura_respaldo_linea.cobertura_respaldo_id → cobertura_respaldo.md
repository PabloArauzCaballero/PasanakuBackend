---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: cobertura_respaldo_linea
columna: cobertura_respaldo_id
destino: cobertura_respaldo
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (1..N)"
---

# cobertura_respaldo_linea.cobertura_respaldo_id → cobertura_respaldo

> **[[cobertura_respaldo_linea]]** `.cobertura_respaldo_id` → **[[cobertura_respaldo]]**

| | |
| --- | --- |
| Entidad origen | [[cobertura_respaldo_linea]] (módulo 08) |
| Entidad destino | [[cobertura_respaldo]] (módulo 08) |
| Columna | `cobertura_respaldo_id` — UUID |
| Cardinalidad | uno a muchos (1..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "detalla por obligacion" |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
