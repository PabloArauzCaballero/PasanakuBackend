---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: cobertura_respaldo_linea
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 5
fk_salientes: 2
fk_entrantes: 1
append_only: false
---

# `cobertura_respaldo_linea`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `cobertura_respaldo_id` | UUID | FK IDX | no | FK, IDX |
| `obligacion_id` | UUID | FK UQ IDX | no | FK, IDX, UQ+cobertura_respaldo_id |
| `monto_cubierto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_recuperado` | DECIMAL(14,2) | — | no | CK: >= 0 |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_cobertura_respaldo_linea_recuperada` | CHECK | `monto_cubierto`, `monto_recuperado` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `cobertura_respaldo_id` | [[cobertura_respaldo]] | 08 | no | [[cobertura_respaldo_linea.cobertura_respaldo_id → cobertura_respaldo]] |
| `obligacion_id` | [[obligacion_aporte]] | ↗ 03 | no | [[cobertura_respaldo_linea.obligacion_id → obligacion_aporte]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[recuperacion_respaldo]] | `cobertura_respaldo_linea_id` | 08 | [[recuperacion_respaldo.cobertura_respaldo_linea_id → cobertura_respaldo_linea]] |

## Entidades vecinas

[[cobertura_respaldo]] · [[obligacion_aporte]] · [[recuperacion_respaldo]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
