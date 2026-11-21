---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: reembolso
clase: Reembolso
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 11
fk_salientes: 3
fk_entrantes: 0
append_only: false
---

# `reembolso`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `Reembolso`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `pago_id` | UUID | FK IDX | no | FK, IDX |
| `monto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `motivo` | VARCHAR(30) | — | no | CK |
| `estado` | VARCHAR(15) | — | no | CK |
| `referencia_proveedor` | VARCHAR(80) | — | sí | NULL |
| `solicitado_por` | UUID | FK | no | FK |
| `aprobado_por` | UUID | FK | sí | FK, NULL |
| `clave_idempotencia` | VARCHAR(100) | — | sí | NULL |
| `fecha_solicitud` | TIMESTAMPTZ | — | no | — |
| `fecha_ejecucion` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_reembolso_clave` | UNIQUE | `pago_id`, `clave_idempotencia` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `aprobado_por` | [[usuario]] | ↗ 01 | sí | [[reembolso.aprobado_por → usuario]] |
| `pago_id` | [[pago]] | 03 | no | [[reembolso.pago_id → pago]] |
| `solicitado_por` | [[usuario]] | ↗ 01 | no | [[reembolso.solicitado_por → usuario]] |

## Entidades vecinas

[[pago]] · [[usuario]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
