---
tags:
  - relacion
  - fk
  - modulo/05-notificaciones-y-comunicaciones
  - cross-modulo
origen: aviso_interno
columna: usuario_id
destino: usuario
modulo_origen: "05"
modulo_destino: "01"
cross_modulo: true
opcional: true
cardinalidad: "no declarada en el diagrama"
---

# aviso_interno.usuario_id → usuario

> **[[aviso_interno]]** `.usuario_id` → **[[usuario]]**

| | |
| --- | --- |
| Entidad origen | [[aviso_interno]] (módulo 05) |
| Entidad destino | [[usuario]] (módulo 01) |
| Columna | `usuario_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | no (admite NULL) |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 05 con el 01. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[05_notificaciones]] — justificación de negocio del origen
- [[01_identidad_usuarios]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
