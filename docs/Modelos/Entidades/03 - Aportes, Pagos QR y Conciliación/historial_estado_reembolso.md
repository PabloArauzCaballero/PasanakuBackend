---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: historial_estado_reembolso
clase: HistorialEstadoReembolso
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 6
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `historial_estado_reembolso`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `HistorialEstadoReembolso`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `reembolso_id` | UUID | FK IDX | no | FK, IDX |
| `estado_anterior` | VARCHAR(15) | — | no | — |
| `estado_nuevo` | VARCHAR(15) | — | no | — |
| `ejecutado_por` | UUID | FK | no | FK |
| `fecha_hora` | TIMESTAMPTZ | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_historial_estado_reembolso_salida` | UNIQUE | `reembolso_id`, `estado_anterior` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `ejecutado_por` | [[usuario]] | ↗ 01 | no | [[historial_estado_reembolso.ejecutado_por → usuario]] |
| `reembolso_id` | [[reembolso]] | 03 | no | [[historial_estado_reembolso.reembolso_id → reembolso]] |

## Entidades vecinas

[[reembolso]] · [[usuario]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
