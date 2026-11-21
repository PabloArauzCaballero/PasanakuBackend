---
tags:
  - relacion
  - fk
  - modulo/03-aportes-pagos-qr-y-conciliacion
  - cross-modulo
origen: historial_estado_reembolso
columna: ejecutado_por
destino: usuario
modulo_origen: "03"
modulo_destino: "01"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# historial_estado_reembolso.ejecutado_por → usuario

> **[[historial_estado_reembolso]]** `.ejecutado_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[historial_estado_reembolso]] (módulo 03) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `ejecutado_por` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 03 con el 01. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[03_aportes_pagos_qr]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
