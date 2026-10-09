---
tags:
  - entidad
  - modulo/07-organizador-y-automatizacion
tabla: decision_habilitacion
modulo: "07 — Organizador y Automatización"
clave_primaria: [id]
columnas: 12
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `decision_habilitacion`

> Módulo [[07_organizador_automatizacion|07 — Organizador y Automatización]]

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
| `revision` | INTEGER | UQ | no | UQ+solicitud_id |
| `organizador_id` | UUID | — | sí | NULL |
| `evidencia_requisitos` | TEXT | — | no | — |
| `ocurrida_en` | TIMESTAMPTZ | — | no | — |
| `correlacion_id` | UUID | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_decision_habilitacion_coherencia` | CHECK | `decision`, `fase` |
| `ck_decision_habilitacion_fase` | CHECK | `fase` |
| `ck_decision_habilitacion_motivo` | CHECK | `motivo` |
| `ck_decision_habilitacion_resultado` | CHECK | `decision` |
| `ck_decision_habilitacion_revision` | CHECK | `revision` |
| `uq_decision_habilitacion_resolucion` | UNIQUE parcial | `solicitud_id` |

## Ver también

- Justificación de negocio: [[07_organizador_automatizacion]]
- Diagramas: `docs/entidades/07_organizador_automatizacion.puml`
- Índice: [[_Entidades]] · [[Index]]
