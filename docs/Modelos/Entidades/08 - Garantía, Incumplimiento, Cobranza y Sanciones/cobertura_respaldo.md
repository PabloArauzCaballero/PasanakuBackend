---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: cobertura_respaldo
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 18
fk_salientes: 6
fk_entrantes: 1
append_only: false
---

# `cobertura_respaldo`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `reserva_respaldo_id` | UUID | FK IDX | no | FK, IDX |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `periodo_id` | UUID | FK | no | FK |
| `turno_id` | UUID | FK IDX | no | FK, IDX |
| `moneda` | CHAR(3) | — | no | — |
| `monto_pozo` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_confirmado` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_cubierto_mutual` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_faltante` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_recuperado` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `estado` | VARCHAR(20) | IDX | no | CK: APLICADA|RECUPERADA_PARCIAL|RECUPERADA_TOTAL|REVERSADA, IDX |
| `corte_en` | TIMESTAMPTZ | — | no | — |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `solicitada_por` | UUID | FK | no | FK |
| `responsable_id` | UUID | FK | no | FK |
| `aplicada_en` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_cobertura_respaldo_cuadra` | CHECK | `monto_confirmado`, `monto_cubierto_mutual`, `monto_faltante`, `monto_pozo` |
| `ck_cobertura_respaldo_estado` | CHECK | `estado`, `monto_faltante`, `monto_recuperado` |
| `ck_cobertura_respaldo_recuperado` | CHECK | `monto_faltante`, `monto_recuperado` |
| `uq_cobertura_respaldo_turno_viva` | UNIQUE parcial | `turno_id` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `grupo_id` | [[grupo]] | ↗ 02 | no | [[cobertura_respaldo.grupo_id → grupo]] |
| `periodo_id` | [[periodo]] | ↗ 02 | no | [[cobertura_respaldo.periodo_id → periodo]] |
| `reserva_respaldo_id` | [[reserva_respaldo]] | 08 | no | [[cobertura_respaldo.reserva_respaldo_id → reserva_respaldo]] |
| `responsable_id` | [[usuario]] | ↗ 01 | no | [[cobertura_respaldo.responsable_id → usuario]] |
| `solicitada_por` | [[usuario]] | ↗ 01 | no | [[cobertura_respaldo.solicitada_por → usuario]] |
| `turno_id` | [[turno]] | ↗ 02 | no | [[cobertura_respaldo.turno_id → turno]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[cobertura_respaldo_linea]] | `cobertura_respaldo_id` | 08 | [[cobertura_respaldo_linea.cobertura_respaldo_id → cobertura_respaldo]] |

## Entidades vecinas

[[cobertura_respaldo_linea]] · [[grupo]] · [[periodo]] · [[reserva_respaldo]] · [[turno]] · [[usuario]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
