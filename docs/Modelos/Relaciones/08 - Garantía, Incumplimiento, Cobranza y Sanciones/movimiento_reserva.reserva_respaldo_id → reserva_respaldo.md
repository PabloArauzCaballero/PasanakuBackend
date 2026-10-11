---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: movimiento_reserva
columna: reserva_respaldo_id
destino: reserva_respaldo
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (1..N)"
---

# movimiento_reserva.reserva_respaldo_id → reserva_respaldo

> **[[movimiento_reserva]]** `.reserva_respaldo_id` → **[[reserva_respaldo]]**

| | |
| --- | --- |
| Entidad origen | [[movimiento_reserva]] (módulo 08) |
| Entidad destino | [[reserva_respaldo]] (módulo 08) |
| Columna | `reserva_respaldo_id` — UUID |
| Cardinalidad | uno a muchos (1..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "registra" |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
