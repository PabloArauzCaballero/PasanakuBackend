---
tags:
  - relacion
  - fk
  - modulo/10-billetera-custodia-y-dinero-electronico
origen: solicitud_reverso
columna: reverso_transaccion_id
destino: reverso_transaccion
modulo_origen: "10"
modulo_destino: "10"
cross_modulo: false
opcional: true
cardinalidad: "uno a uno, ambos opcionales"
---

# solicitud_reverso.reverso_transaccion_id → reverso_transaccion

> **[[solicitud_reverso]]** `.reverso_transaccion_id` → **[[reverso_transaccion]]**

| | |
| --- | --- |
| Entidad origen | [[solicitud_reverso]] (módulo 10) |
| Entidad destino | [[reverso_transaccion]] (módulo 10) |
| Columna | `reverso_transaccion_id` — UUID |
| Cardinalidad | uno a uno, ambos opcionales |
| Obligatoria | no (admite NULL) |
| Uno a uno | sí (columna UNIQUE) |
| Cruza módulos | no |
| Semántica | "se ejecuta como" |

> [!note] Opcional
> La columna admite `NULL`: la relación puede no existir. Conviene revisar en la ficha de negocio qué significa su ausencia.

## Ver también

- [[10_billetera_custodia]] — justificación de negocio del origen
- [[10_billetera_custodia]] — justificación de negocio del destino
- [[_Relaciones]] · [[Index]]
