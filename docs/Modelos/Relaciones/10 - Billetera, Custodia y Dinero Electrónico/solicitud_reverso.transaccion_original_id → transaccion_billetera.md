---
tags:
  - relacion
  - fk
  - modulo/10-billetera-custodia-y-dinero-electronico
origen: solicitud_reverso
columna: transaccion_original_id
destino: transaccion_billetera
modulo_origen: "10"
modulo_destino: "10"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# solicitud_reverso.transaccion_original_id → transaccion_billetera

> **[[solicitud_reverso]]** `.transaccion_original_id` → **[[transaccion_billetera]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_reverso]] (módulo 10) |
| Entidad destino | [[transaccion_billetera]] (módulo 10) |
| Columna | `transaccion_original_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "se pide reversar con" |

## Ver también

- [[10_billetera_custodia]] — justificación de negocio del origen
- [[10_billetera_custodia]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
