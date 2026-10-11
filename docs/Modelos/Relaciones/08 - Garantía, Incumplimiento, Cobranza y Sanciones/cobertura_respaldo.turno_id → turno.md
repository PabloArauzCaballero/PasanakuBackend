---
tags:
  - relacion
  - fk
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
  - cross-modulo
origen: cobertura_respaldo
columna: turno_id
destino: turno
modulo_origen: "08"
modulo_destino: "02"
cross_modulo: true
opcional: false
cardinalidad: "no declarada en el diagrama"
---

# cobertura_respaldo.turno_id → turno

> **[[cobertura_respaldo]]** `.turno_id` → **[[turno]]**

| | |
| --- | --- |
| Entidad origen | [[cobertura_respaldo]] (módulo 08) |
| Entidad destino | [[turno]] (módulo 02) |
| Columna | `turno_id` — UUID |
| Cardinalidad | no declarada en el diagrama |
| Obligatoria | sí |
| Uno a uno | no |
| Cruza módulos | sí ↗ |

> [!info] Referencia entre módulos
> Esta FK conecta el módulo 08 con el 02. En los diagramas se documenta en notas al pie y, cuando es polimórfica, se valida por aplicación o trigger en lugar de con una FK física.

## Ver también

- [[08_garantia_incumplimiento]] — justificación de negocio del origen
- [[02_grupos_turnos]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
