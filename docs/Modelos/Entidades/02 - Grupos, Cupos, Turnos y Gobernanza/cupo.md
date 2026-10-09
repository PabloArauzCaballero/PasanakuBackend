---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: cupo
clase: Cupo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 8
fk_salientes: 2
fk_entrantes: 6
append_only: false
---

# `cupo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]] · clase `Cupo`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `numero` | SMALLINT | UQ | no | UQ+grupo_id |
| `participante_id` | UUID | FK | sí | FK, NULL |
| `estado` | VARCHAR(30) | — | no | CK |
| `fraccion` | DECIMAL(3,2) | — | no | CK: > 0 AND <= 1 |
| `asignado_en` | TIMESTAMPTZ | — | sí | NULL |
| `liberado_en` | TIMESTAMPTZ | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `grupo_id` | [[grupo]] | 02 | no | [[cupo.grupo_id → grupo]] |
| `participante_id` | [[participante]] | 02 | sí | [[cupo.participante_id → participante]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[entrega_fondo]] | `cupo_id` | ↗ 04 | [[entrega_fondo.cupo_id → cupo]] |
| [[obligacion_aporte]] | `cupo_id` | ↗ 03 | [[obligacion_aporte.cupo_id → cupo]] |
| [[reemplazo_participante]] | `cupo_id` | ↗ 08 | [[reemplazo_participante.cupo_id → cupo]] |
| [[registro_incumplimiento]] | `cupo_id` | ↗ 08 | [[registro_incumplimiento.cupo_id → cupo]] |
| [[traspaso_cupo]] | `cupo_id` | 02 | [[traspaso_cupo.cupo_id → cupo]] |
| [[turno]] | `cupo_id` | 02 | [[turno.cupo_id → cupo]] |

## Entidades vecinas

[[entrega_fondo]] · [[grupo]] · [[obligacion_aporte]] · [[participante]] · [[reemplazo_participante]] · [[registro_incumplimiento]] · [[traspaso_cupo]] · [[turno]]

## Notas del modelo

> **UNIQUE (grupo_id, numero)** y
> **UNIQUE (grupo_id, cupo_id) en turno**:
> un cupo cobra exactamente una vez por ciclo.
> Restriccion adicional por trigger:
> SUM(fraccion) por grupo = cupos_totales.

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
