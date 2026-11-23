---
tags:
  - relacion
  - fk
  - modulo/03-aportes-pagos-qr-y-conciliacion
origen: historial_estado_webhook
columna: pago_id
destino: pago
modulo_origen: "03"
modulo_destino: "03"
cross_modulo: false
opcional: true
cardinalidad: "no declarada en el diagrama"
---

# historial_estado_webhook.pago_id → pago

> **[[historial_estado_webhook]]** `.pago_id` → **[[pago]]**

| | |
| --- | --- |
| Entidad origen | [[historial_estado_webhook]] (módulo 03) |
| Entidad destino | [[pago]] (módulo 03) |
| Columna | `pago_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | no |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[03_aportes_pagos_qr]] — justificación de negocio del origen
- [[03_aportes_pagos_qr]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
