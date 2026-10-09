---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: sustitucion_administrador
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 10
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `sustitucion_administrador`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | IDX | no | IDX |
| `saliente_participante_id` | UUID | — | no | — |
| `entrante_participante_id` | UUID | — | no | — |
| `clave_idempotencia` | UUID | UQ | no | UQ |
| `motivo` | VARCHAR(1000) | — | no | — |
| `actor_id` | UUID | — | no | — |
| `obligaciones_conservadas` | TEXT | — | no | — |
| `ocurrida_en` | TIMESTAMPTZ | — | no | — |
| `correlacion_id` | UUID | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_sustitucion_administrador_distintos` | CHECK | `entrante_participante_id`, `saliente_participante_id` |
| `ck_sustitucion_administrador_motivo` | CHECK | `motivo` |

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
