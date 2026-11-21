---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: historial_estado_pago
clase: HistorialEstadoPago
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 6
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `historial_estado_pago`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `HistorialEstadoPago`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `pago_id` | UUID | FK IDX | no | FK, IDX |
| `estado_anterior` | VARCHAR(15) | — | no | — |
| `estado_nuevo` | VARCHAR(15) | — | no | — |
| `motivo` | VARCHAR(200) | — | no | — |
| `fecha_hora` | TIMESTAMPTZ | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_historial_estado_pago_salida` | UNIQUE | `pago_id`, `estado_anterior` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `pago_id` | [[pago]] | 03 | no | [[historial_estado_pago.pago_id → pago]] |

## Entidades vecinas

[[pago]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
