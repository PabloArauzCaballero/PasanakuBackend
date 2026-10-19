---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: enlace_grupo
clase: EnlaceGrupo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 8
fk_salientes: 2
fk_entrantes: 1
append_only: false
---

# `enlace_grupo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]] · clase `EnlaceGrupo`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `emisor_id` | UUID | FK | no | FK |
| `token_hash` | VARCHAR(64) | UQ | no | UQ |
| `estado` | VARCHAR(15) | — | no | CK |
| `fecha_creacion` | TIMESTAMPTZ | — | no | — |
| `fecha_expiracion` | TIMESTAMPTZ | — | no | — |
| `fecha_anulacion` | TIMESTAMPTZ | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `emisor_id` | [[usuario]] | ↗ 01 | no | [[enlace_grupo.emisor_id → usuario]] |
| `grupo_id` | [[grupo]] | 02 | no | [[enlace_grupo.grupo_id → grupo]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[solicitud_ingreso]] | `enlace_grupo_id` | 02 | [[solicitud_ingreso.enlace_grupo_id → enlace_grupo]] |

## Entidades vecinas

[[grupo]] · [[solicitud_ingreso]] · [[usuario]]

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
