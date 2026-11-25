---
tags:
  - entidad
  - modulo/06-transparencia-y-reputacion
tabla: modelo_scoring
clase: ModeloScoring
modulo: "06 — Transparencia y Reputación"
estereotipo: Política configurable
clave_primaria: [id]
columnas: 12
fk_salientes: 0
fk_entrantes: 3
append_only: false
---

# `modelo_scoring`

> Módulo [[06_transparencia_reputacion|06 — Transparencia y Reputación]] · clase `ModeloScoring` · Política configurable

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `version` | VARCHAR(20) | UQ | no | UQ |
| `descripcion` | VARCHAR(200) | — | no | — |
| `puntaje_base` | DECIMAL(6,2) | — | no | — |
| `puntaje_minimo` | DECIMAL(6,2) | — | no | — |
| `puntaje_maximo` | DECIMAL(6,2) | — | no | — |
| `factor_decaimiento_mensual` | DECIMAL(5,4) | — | no | — |
| `ventana_historica_meses` | SMALLINT | — | no | — |
| `min_eventos_para_score` | SMALLINT | — | no | — |
| `vigente_desde` | TIMESTAMPTZ | — | no | — |
| `vigente_hasta` | TIMESTAMPTZ | — | sí | NULL |
| `es_produccion` | BOOLEAN | — | no | — |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[peso_factor]] | `modelo_id` | 06 | [[peso_factor.modelo_id → modelo_scoring]] |
| [[puntaje_reputacion]] | `modelo_id` | 06 | [[puntaje_reputacion.modelo_id → modelo_scoring]] |
| [[regla_impacto_evento]] | `modelo_id` | 06 | [[regla_impacto_evento.modelo_id → modelo_scoring]] |

## Entidades vecinas

[[peso_factor]] · [[puntaje_reputacion]] · [[regla_impacto_evento]]

## Ver también

- Justificación de negocio: [[06_transparencia_reputacion]]
- Diagramas: `docs/entidades/06_transparencia_reputacion.puml`
- Índice: [[_Entidades]] · [[Index]]
