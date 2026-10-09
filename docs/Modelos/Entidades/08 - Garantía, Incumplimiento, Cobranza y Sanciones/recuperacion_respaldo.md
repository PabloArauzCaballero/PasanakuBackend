---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: recuperacion_respaldo
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 7
fk_salientes: 3
fk_entrantes: 0
append_only: false
---

# `recuperacion_respaldo`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `cobertura_respaldo_linea_id` | UUID | FK IDX | no | FK, IDX |
| `pago_id` | UUID | FK UQ | no | FK, UQ |
| `movimiento_reserva_id` | UUID | FK | no | FK |
| `monto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `moneda` | CHAR(3) | — | no | — |
| `recuperada_en` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `cobertura_respaldo_linea_id` | [[cobertura_respaldo_linea]] | 08 | no | [[recuperacion_respaldo.cobertura_respaldo_linea_id → cobertura_respaldo_linea]] |
| `movimiento_reserva_id` | [[movimiento_reserva]] | 08 | no | [[recuperacion_respaldo.movimiento_reserva_id → movimiento_reserva]] |
| `pago_id` | [[pago]] | ↗ 03 | no | [[recuperacion_respaldo.pago_id → pago]] |

## Entidades vecinas

[[cobertura_respaldo_linea]] · [[movimiento_reserva]] · [[pago]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
