---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: webhook_clave
clase: WebhookClave
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [proveedor_id, clave_idempotencia]
columnas: 4
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `webhook_clave`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `WebhookClave`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `proveedor_id` | UUID | PK FK IDX | no | PK, FK, IDX |
| `clave_idempotencia` | VARCHAR(120) | PK | no | PK |
| `webhook_id` | UUID | IDX | no | IDX |
| `webhook_recibido_en` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `proveedor_id` | [[proveedor_pago]] | 03 | no | [[webhook_clave.proveedor_id → proveedor_pago]] |

## Entidades vecinas

[[proveedor_pago]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
