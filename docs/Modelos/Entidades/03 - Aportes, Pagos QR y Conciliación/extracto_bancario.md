---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: extracto_bancario
clase: ExtractoBancario
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 11
fk_salientes: 2
fk_entrantes: 1
append_only: false
---

# `extracto_bancario`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `ExtractoBancario`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `proveedor_id` | UUID | FK | no | FK |
| `cuenta` | VARCHAR(40) | — | no | — |
| `fecha_desde` | DATE | — | no | — |
| `fecha_hasta` | DATE | — | no | — |
| `saldo_inicial` | DECIMAL(14,2) | — | no | — |
| `saldo_final` | DECIMAL(14,2) | — | no | — |
| `moneda` | CHAR(3) | — | sí | NULL |
| `archivo_url` | VARCHAR(255) | — | no | — |
| `importado_en` | TIMESTAMPTZ | — | no | — |
| `importado_por` | UUID | FK | no | FK |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_extracto_bancario_moneda` | CHECK | `moneda` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `importado_por` | [[usuario]] | ↗ 01 | no | [[extracto_bancario.importado_por → usuario]] |
| `proveedor_id` | [[proveedor_pago]] | 03 | no | [[extracto_bancario.proveedor_id → proveedor_pago]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[movimiento_bancario]] | `extracto_id` | 03 | [[movimiento_bancario.extracto_id → extracto_bancario]] |

## Entidades vecinas

[[movimiento_bancario]] · [[proveedor_pago]] · [[usuario]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
