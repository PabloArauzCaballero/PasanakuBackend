---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: proveedor_pago
clase: ProveedorPago
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 13
fk_salientes: 0
fk_entrantes: 9
append_only: false
---

# `proveedor_pago`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `ProveedorPago`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `codigo` | VARCHAR(30) | UQ | no | UQ |
| `nombre` | VARCHAR(80) | — | no | — |
| `tipo` | VARCHAR(15) | — | no | CK |
| `url_base` | VARCHAR(200) | — | no | — |
| `referencia_credenciales` | VARCHAR(120) | — | no | — |
| `comision_fija` | DECIMAL(10,2) | — | no | — |
| `comision_porcentual` | DECIMAL(5,3) | — | no | — |
| `soporta_webhook` | BOOLEAN | — | no | — |
| `soporta_consulta_estado` | BOOLEAN | — | no | — |
| `activo` | BOOLEAN | — | no | — |
| `prioridad` | SMALLINT | — | no | — |
| `clave_idempotencia` | VARCHAR(100) | UQ | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_proveedor_pago_clave` | UNIQUE | `clave_idempotencia` |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[costo_proveedor_operacion]] | `proveedor_id` | ↗ 11 | [[costo_proveedor_operacion.proveedor_id → proveedor_pago]] |
| [[extracto_bancario]] | `proveedor_id` | 03 | [[extracto_bancario.proveedor_id → proveedor_pago]] |
| [[orden_cobro]] | `proveedor_id` | 03 | [[orden_cobro.proveedor_id → proveedor_pago]] |
| [[orden_desembolso]] | `proveedor_id` | ↗ 04 | [[orden_desembolso.proveedor_id → proveedor_pago]] |
| [[orden_recarga]] | `proveedor_id` | ↗ 10 | [[orden_recarga.proveedor_id → proveedor_pago]] |
| [[orden_retiro]] | `proveedor_id` | ↗ 10 | [[orden_retiro.proveedor_id → proveedor_pago]] |
| [[pago]] | `proveedor_id` | 03 | [[pago.proveedor_id → proveedor_pago]] |
| [[webhook_clave]] | `proveedor_id` | 03 | [[webhook_clave.proveedor_id → proveedor_pago]] |
| [[webhook_pasarela]] | `proveedor_id` | 03 | [[webhook_pasarela.proveedor_id → proveedor_pago]] |

## Entidades vecinas

[[costo_proveedor_operacion]] · [[extracto_bancario]] · [[orden_cobro]] · [[orden_desembolso]] · [[orden_recarga]] · [[orden_retiro]] · [[pago]] · [[webhook_clave]] · [[webhook_pasarela]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
