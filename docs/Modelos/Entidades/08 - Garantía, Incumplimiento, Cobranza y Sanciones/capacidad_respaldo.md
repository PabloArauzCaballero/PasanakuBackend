---
tags:
  - entidad
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
tabla: capacidad_respaldo
modulo: "08 — Garantía, Incumplimiento, Cobranza y Sanciones"
clave_primaria: [id]
columnas: 8
fk_salientes: 1
fk_entrantes: 1
append_only: false
---

# `capacidad_respaldo`

> Módulo [[08_garantia_incumplimiento|08 — Garantía, Incumplimiento, Cobranza y Sanciones]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `ambito` | VARCHAR(30) | UQ | no | UQ+moneda |
| `moneda` | CHAR(3) | — | no | — |
| `monto_tope` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `monto_comprometido` | DECIMAL(16,2) | — | no | CK: >= 0 |
| `responsable_id` | UUID | FK | no | FK |
| `vigente_desde` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_capacidad_respaldo_comprometido` | CHECK | `monto_comprometido`, `monto_tope` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `responsable_id` | [[usuario]] | ↗ 01 | no | [[capacidad_respaldo.responsable_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[reserva_respaldo]] | `capacidad_respaldo_id` | 08 | [[reserva_respaldo.capacidad_respaldo_id → capacidad_respaldo]] |

## Entidades vecinas

[[reserva_respaldo]] · [[usuario]]

## Ver también

- Justificación de negocio: [[08_garantia_incumplimiento]]
- Diagramas: `docs/entidades/08_garantia_incumplimiento.puml`
- Índice: [[_Entidades]] · [[Index]]
