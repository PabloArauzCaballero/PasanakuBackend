---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: cobertura_respaldo
columna: reserva_respaldo_id
destino: reserva_respaldo
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# cobertura_respaldo.reserva_respaldo_id → reserva_respaldo

> **[[cobertura_respaldo]]** `.reserva_respaldo_id` → **[[reserva_respaldo]]**

| | |
| --- | --- |
| Entidad origen | [[cobertura_respaldo]] (módulo 08) |
| Entidad destino | [[reserva_respaldo]] (módulo 08) |
| Columna | `reserva_respaldo_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "cubre con" |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
