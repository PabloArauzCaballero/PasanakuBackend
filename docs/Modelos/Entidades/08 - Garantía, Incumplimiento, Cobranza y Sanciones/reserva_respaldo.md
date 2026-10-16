---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: reserva_respaldo
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 14
fk_salientes: 3
fk_entrantes: 2
append_only: false
---

# `reserva_respaldo`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `capacidad_respaldo_id` | UUID | FK IDX | no | FK, IDX |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `ciclo_numero` | SMALLINT | UQ | no | CK: >= 1, UQ+grupo_id |
| `moneda` | CHAR(3) | — | no | — |
| `monto_reservado` | DECIMAL(16,2) | — | no | CK: > 0 |
| `monto_aplicado` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `monto_recuperado` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `monto_liberado` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `estado` | VARCHAR(10) | IDX | no | CK: VIGENTE|LIBERADA, IDX |
| `responsable_id` | UUID | FK | no | FK |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `reservada_en` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_reserva_respaldo_recuperado` | CHECK | `monto_aplicado`, `monto_recuperado` |
| `ck_reserva_respaldo_uso` | CHECK | `monto_aplicado`, `monto_liberado`, `monto_reservado` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `capacidad_respaldo_id` | [[capacidad_respaldo]] | 08 | no | [[reserva_respaldo.capacidad_respaldo_id → capacidad_respaldo]] |
| `grupo_id` | [[grupo]] | ↗ 02 | no | [[reserva_respaldo.grupo_id → grupo]] |
| `responsable_id` | [[usuario]] | ↗ 01 | no | [[reserva_respaldo.responsable_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[cobertura_respaldo]] | `reserva_respaldo_id` | 08 | [[cobertura_respaldo.reserva_respaldo_id → reserva_respaldo]] |
| [[movimiento_reserva]] | `reserva_respaldo_id` | 08 | [[movimiento_reserva.reserva_respaldo_id → reserva_respaldo]] |

## Entidades vecinas

[[capacidad_respaldo]] · [[cobertura_respaldo]] · [[grupo]] · [[movimiento_reserva]] · [[usuario]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
