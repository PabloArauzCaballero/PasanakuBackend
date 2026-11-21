---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: conciliacion
clase: Conciliacion
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 9
fk_salientes: 3
fk_entrantes: 1
append_only: false
---

# `conciliacion`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `Conciliacion`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `pago_id` | UUID | FK UQ | no | FK, UQ |
| `movimiento_bancario_id` | UUID | FK UQ | sí | FK, NULL, UQ |
| `estado` | VARCHAR(25) | IDX | no | CK, IDX |
| `metodo` | VARCHAR(20) | — | no | CK |
| `diferencia_monto` | DECIMAL(14,2) | — | no | — |
| `moneda` | CHAR(3) | — | sí | NULL |
| `conciliado_por` | UUID | FK | sí | FK, NULL |
| `fecha_conciliacion` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_conciliacion_moneda` | CHECK | `moneda` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `conciliado_por` | [[usuario]] | ↗ 01 | sí | [[conciliacion.conciliado_por → usuario]] |
| `movimiento_bancario_id` | [[movimiento_bancario]] | 03 | sí | [[conciliacion.movimiento_bancario_id → movimiento_bancario]] |
| `pago_id` | [[pago]] | 03 | no | [[conciliacion.pago_id → pago]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[excepcion_conciliacion]] | `conciliacion_id` | 03 | [[excepcion_conciliacion.conciliacion_id → conciliacion]] |

## Entidades vecinas

[[excepcion_conciliacion]] · [[movimiento_bancario]] · [[pago]] · [[usuario]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
