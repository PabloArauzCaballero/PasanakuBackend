---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: recuperacion_respaldo
columna: movimiento_reserva_id
destino: movimiento_reserva
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# recuperacion_respaldo.movimiento_reserva_id → movimiento_reserva

> **[[recuperacion_respaldo]]** `.movimiento_reserva_id` → **[[movimiento_reserva]]**

| | |
| --- | --- |
| Entidad origen | [[recuperacion_respaldo]] (módulo 08) |
| Entidad destino | [[movimiento_reserva]] (módulo 08) |
| Columna | `movimiento_reserva_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
