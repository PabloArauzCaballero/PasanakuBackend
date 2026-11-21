---
tags:
  - relacion
  - fk
  - modulo/10-billetera-custodia-y-dinero-electronico
  - cross-modulo
origen: solicitud_reverso
columna: solicitada_por
destino: usuario
modulo_origen: "10"
modulo_destino: "01"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# solicitud_reverso.solicitada_por → usuario

> **[[solicitud_reverso]]** `.solicitada_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_reverso]] (módulo 10) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `solicitada_por` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 10 con el 01. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[10_billetera_custodia]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
