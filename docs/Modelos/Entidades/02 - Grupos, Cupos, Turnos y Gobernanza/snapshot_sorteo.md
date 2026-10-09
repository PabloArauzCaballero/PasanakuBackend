---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: snapshot_sorteo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 10
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `snapshot_sorteo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `sorteo_id` | UUID | UQ | no | UQ |
| `grupo_id` | UUID | IDX | no | IDX |
| `roster` | TEXT | — | no | — |
| `periodos` | TEXT | — | no | — |
| `reglas` | TEXT | — | no | — |
| `hash_snapshot` | VARCHAR(64) | — | no | — |
| `semilla_sellada` | VARCHAR(128) | — | no | — |
| `congelado_en` | TIMESTAMPTZ | — | no | — |
| `correlacion_id` | UUID | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_snapshot_sorteo_hash` | CHECK | `hash_snapshot` |
| `ck_snapshot_sorteo_roster` | CHECK | `roster` |

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
