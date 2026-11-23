---
tags:
  - relacion
  - fk
  - modulo/03-aportes-pagos-qr-y-conciliacion
origen: webhook_clave
columna: proveedor_id
destino: proveedor_pago
modulo_origen: "03"
modulo_destino: "03"
cross_modulo: false
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# webhook_clave.proveedor_id → proveedor_pago

> **[[webhook_clave]]** `.proveedor_id` → **[[proveedor_pago]]**

| | |
| --- | --- |
| Entidad origen | [[webhook_clave]] (módulo 03) |
| Entidad destino | [[proveedor_pago]] (módulo 03) |
| Columna | `proveedor_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | no |

## Ver también

- [[03_aportes_pagos_qr]] — justificación de negocio del origen
- [[03_aportes_pagos_qr]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
