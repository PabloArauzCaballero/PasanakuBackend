---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
  - append-only
tabla: movimiento_contable
clase: MovimientoContable
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 7
fk_salientes: 2
fk_entrantes: 0
append_only: true
---

# `movimiento_contable`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `MovimientoContable` · **append-only** (sin `UPDATE`/`DELETE`)

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `asiento_id` | UUID | FK IDX | no | FK, IDX |
| `cuenta_id` | UUID | FK IDX | no | FK, IDX |
| `debe` | DECIMAL(16,2) | — | no | — |
| `haber` | DECIMAL(16,2) | — | no | — |
| `moneda` | CHAR(3) | — | no | — |
| `descripcion` | VARCHAR(160) | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `asiento_id` | [[asiento_contable]] | 03 | no | [[movimiento_contable.asiento_id → asiento_contable]] |
| `cuenta_id` | [[cuenta_contable]] | 03 | no | [[movimiento_contable.cuenta_id → cuenta_contable]] |

## Entidades vecinas

[[asiento_contable]] · [[cuenta_contable]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
