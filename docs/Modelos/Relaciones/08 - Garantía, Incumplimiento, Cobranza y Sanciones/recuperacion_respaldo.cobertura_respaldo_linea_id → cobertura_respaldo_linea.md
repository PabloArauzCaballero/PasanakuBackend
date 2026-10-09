---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: recuperacion_respaldo
columna: cobertura_respaldo_linea_id
destino: cobertura_respaldo_linea
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# recuperacion_respaldo.cobertura_respaldo_linea_id → cobertura_respaldo_linea

> **[[recuperacion_respaldo]]** `.cobertura_respaldo_linea_id` → **[[cobertura_respaldo_linea]]**

| | |
| --- | --- |
| Entidad origen | [[recuperacion_respaldo]] (módulo 08) |
| Entidad destino | [[cobertura_respaldo_linea]] (módulo 08) |
| Columna | `cobertura_respaldo_linea_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "se recupera con" |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
