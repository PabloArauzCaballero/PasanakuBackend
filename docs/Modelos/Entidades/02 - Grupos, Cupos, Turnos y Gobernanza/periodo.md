---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: periodo
clase: Periodo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 11
fk_salientes: 1
fk_entrantes: 6
append_only: false
---

# `periodo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]] · clase `Periodo`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `numero` | SMALLINT | UQ | no | UQ+grupo_id |
| `fecha_inicio` | DATE | — | no | — |
| `fecha_limite_pago` | DATE | IDX | no | IDX |
| `fecha_fin_gracia` | DATE | — | no | — |
| `fecha_entrega_prevista` | DATE | — | no | — |
| `estado` | VARCHAR(15) | IDX | no | CK, IDX |
| `monto_objetivo` | DECIMAL(14,2) | — | no | — |
| `monto_recaudado` | DECIMAL(14,2) | — | no | — |
| `cupos_morosos` | SMALLINT | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `grupo_id` | [[grupo]] | 02 | no | [[periodo.grupo_id → grupo]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[cobertura_incumplimiento]] | `periodo_id` | ↗ 08 | [[cobertura_incumplimiento.periodo_id → periodo]] |
| [[entrega_fondo]] | `periodo_id` | ↗ 04 | [[entrega_fondo.periodo_id → periodo]] |
| [[metrica_grupo]] | `periodo_id` | ↗ 06 | [[metrica_grupo.periodo_id → periodo]] |
| [[obligacion_aporte]] | `periodo_id` | ↗ 03 | [[obligacion_aporte.periodo_id → periodo]] |
| [[registro_incumplimiento]] | `periodo_id` | ↗ 08 | [[registro_incumplimiento.periodo_id → periodo]] |
| [[turno]] | `periodo_id` | 02 | [[turno.periodo_id → periodo]] |

## Entidades vecinas

[[cobertura_incumplimiento]] · [[entrega_fondo]] · [[grupo]] · [[metrica_grupo]] · [[obligacion_aporte]] · [[registro_incumplimiento]] · [[turno]]

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
