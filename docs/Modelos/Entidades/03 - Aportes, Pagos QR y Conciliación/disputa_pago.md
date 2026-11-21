---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: disputa_pago
clase: DisputaPago
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 11
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `disputa_pago`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `DisputaPago`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `pago_id` | UUID | FK IDX | no | FK, IDX |
| `tipo` | VARCHAR(25) | — | no | CK |
| `descripcion` | TEXT | — | no | — |
| `monto_disputado` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `moneda` | CHAR(3) | — | sí | NULL |
| `estado` | VARCHAR(25) | — | no | CK |
| `evidencias` | JSONB | — | no | — |
| `abierta_en` | TIMESTAMPTZ | — | no | — |
| `fecha_limite_respuesta` | TIMESTAMPTZ | — | no | — |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_disputa_pago_moneda` | CHECK | `moneda` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `pago_id` | [[pago]] | 03 | no | [[disputa_pago.pago_id → pago]] |

## Entidades vecinas

[[pago]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
