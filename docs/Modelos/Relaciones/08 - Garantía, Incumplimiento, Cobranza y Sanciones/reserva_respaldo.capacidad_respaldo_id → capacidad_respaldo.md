---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
origen: reserva_respaldo
columna: capacidad_respaldo_id
destino: capacidad_respaldo
modulo_origen: "08"
modulo_destino: "08"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# reserva_respaldo.capacidad_respaldo_id → capacidad_respaldo

> **[[reserva_respaldo]]** `.capacidad_respaldo_id` → **[[capacidad_respaldo]]**

| | |
| --- | --- |
| Entidad origen | [[reserva_respaldo]] (módulo 08) |
| Entidad destino | [[capacidad_respaldo]] (módulo 08) |
| Columna | `capacidad_respaldo_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "respalda" |

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[08_garantia_incumplimiento]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
