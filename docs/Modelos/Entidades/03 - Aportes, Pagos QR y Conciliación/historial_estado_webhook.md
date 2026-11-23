---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: historial_estado_webhook
clase: HistorialEstadoWebhook
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 9
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `historial_estado_webhook`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `HistorialEstadoWebhook`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `webhook_id` | UUID | IDX | no | IDX |
| `webhook_recibido_en` | TIMESTAMPTZ | IDX | no | IDX |
| `estado_anterior` | VARCHAR(15) | UQ | sí | NULL, CK, UQ+webhook_id+webhook_recibido_en |
| `estado_nuevo` | VARCHAR(15) | IDX | no | CK, IDX |
| `procesado_en` | TIMESTAMPTZ | — | sí | NULL |
| `intentos_procesamiento` | SMALLINT | — | no | — |
| `error_procesamiento` | TEXT | — | sí | NULL |
| `pago_id` | UUID | FK | sí | FK, NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `pago_id` | [[pago]] | 03 | sí | [[historial_estado_webhook.pago_id → pago]] |

## Entidades vecinas

[[pago]]

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
