---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: decision_ingreso
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 16
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `decision_ingreso`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `solicitud_id` | UUID | IDX | no | IDX |
| `clave_idempotencia` | UUID | UQ | no | UQ |
| `fase` | VARCHAR(15) | — | no | — |
| `decision` | VARCHAR(15) | — | no | — |
| `actor_id` | UUID | — | no | — |
| `motivo` | VARCHAR(1000) | — | no | — |
| `propuesta_id` | UUID | — | sí | NULL |
| `participante_id` | UUID | — | sí | NULL |
| `revision` | INTEGER | UQ | no | UQ+solicitud_id |
| `evidencia_algoritmo` | TEXT | — | no | — |
| `version_motor` | VARCHAR(60) | — | no | — |
| `recomendacion_algoritmo` | VARCHAR(20) | — | no | — |
| `apartamiento` | BOOLEAN | — | no | — |
| `ocurrida_en` | TIMESTAMPTZ | — | no | — |
| `correlacion_id` | UUID | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_decision_ingreso_fase` | CHECK | `fase` |
| `ck_decision_ingreso_motivo` | CHECK | `motivo` |
| `ck_decision_ingreso_propuesta` | CHECK | `fase`, `participante_id`, `propuesta_id` |
| `ck_decision_ingreso_recomendacion` | CHECK | `recomendacion_algoritmo` |
| `ck_decision_ingreso_resultado` | CHECK | `decision` |
| `ck_decision_ingreso_revision` | CHECK | `revision` |
| `uq_decision_ingreso_resolucion` | UNIQUE parcial | `solicitud_id` |

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
