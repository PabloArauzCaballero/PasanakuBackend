---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: alta_grupo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
clave_primaria: [id]
columnas: 7
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `alta_grupo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | UQ | no | UQ |
| `creador_id` | UUID | IDX | no | IDX |
| `clave_idempotencia` | UUID | UQ | no | UQ |
| `huella_solicitud` | VARCHAR(64) | — | no | — |
| `fondo_por_periodo` | VARCHAR(60) | — | no | — |
| `creada_en` | TIMESTAMPTZ | — | no | — |

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
