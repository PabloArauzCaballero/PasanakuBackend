---
tags:
  - relacion
  - fk
  - modulo/03-aportes-pagos-qr-y-conciliacion
origen: historial_estado_reembolso
columna: reembolso_id
destino: reembolso
modulo_origen: "03"
modulo_destino: "03"
cross_modulo: false
opcional: false
cardinalidad: "uno a muchos (0..N)"
---

# historial_estado_reembolso.reembolso_id → reembolso

> **[[historial_estado_reembolso]]** `.reembolso_id` → **[[reembolso]]**

| | |
| --- | --- |
| Entidad origen | [[historial_estado_reembolso]] (módulo 03) |
| Entidad destino | [[reembolso]] (módulo 03) |
| Columna | `reembolso_id` — UUID |
| Cardinalidad | uno a muchos (0..N) |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |
| Semántica | "transiciona" |

## Ver también

- [[03_aportes_pagos_qr]] — justificación de negocio del origen
- [[03_aportes_pagos_qr]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
