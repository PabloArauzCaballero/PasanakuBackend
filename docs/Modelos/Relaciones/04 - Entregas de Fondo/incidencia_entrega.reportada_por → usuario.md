---
tags:
  - relacion
  - fk
  - modulo/04-entregas-de-fondo
  - cross-modulo
origen: incidencia_entrega
columna: reportada_por
destino: usuario
modulo_origen: "04"
modulo_destino: "01"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# incidencia_entrega.reportada_por → usuario

> **[[incidencia_entrega]]** `.reportada_por` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[incidencia_entrega]] (módulo 04) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `reportada_por` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 04 con el 01. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[04_entregas_fondo]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
