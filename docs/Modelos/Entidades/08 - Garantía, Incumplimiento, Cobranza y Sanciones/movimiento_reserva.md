---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: movimiento_reserva
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 13
fk_salientes: 3
fk_entrantes: 1
append_only: false
---

# `movimiento_reserva`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `reserva_respaldo_id` | UUID | FK IDX | no | FK, IDX |
| `tipo` | VARCHAR(25) | IDX | no | CK: RESERVA|AMPLIACION|APLICACION|REVERSA_APLICACION|RECUPERACION|LIBERACION, IDX |
| `monto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `moneda` | CHAR(3) | — | no | — |
| `disponible_resultante` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `exposicion_resultante` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `referencia_tipo` | VARCHAR(30) | — | no | — |
| `referencia_id` | UUID | IDX | no | IDX |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `registrado_por` | UUID | FK | no | FK |
| `responsable_id` | UUID | FK | no | FK |
| `fecha` | TIMESTAMPTZ | IDX | no | IDX |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `registrado_por` | [[usuario]] | ↗ 01 | no | [[movimiento_reserva.registrado_por → usuario]] |
| `reserva_respaldo_id` | [[reserva_respaldo]] | 08 | no | [[movimiento_reserva.reserva_respaldo_id → reserva_respaldo]] |
| `responsable_id` | [[usuario]] | ↗ 01 | no | [[movimiento_reserva.responsable_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[recuperacion_respaldo]] | `movimiento_reserva_id` | 08 | [[recuperacion_respaldo.movimiento_reserva_id → movimiento_reserva]] |

## Entidades vecinas

[[recuperacion_respaldo]] · [[reserva_respaldo]] · [[usuario]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
