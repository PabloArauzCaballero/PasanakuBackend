---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: webhook_pasarela
clase: WebhookPasarela
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 8
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `webhook_pasarela`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `WebhookPasarela`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `proveedor_id` | UUID | FK IDX | no | FK, IDX |
| `evento` | VARCHAR(60) | — | no | — |
| `payload_crudo` | JSONB | — | no | — |
| `firma` | VARCHAR(255) | — | no | — |
| `firma_valida` | BOOLEAN | — | no | — |
| `recibido_en` | TIMESTAMPTZ | IDX | no | IDX |
| `clave_idempotencia` | VARCHAR(120) | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `proveedor_id` | [[proveedor_pago]] | 03 | no | [[webhook_pasarela.proveedor_id → proveedor_pago]] |

## Entidades vecinas

[[proveedor_pago]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
