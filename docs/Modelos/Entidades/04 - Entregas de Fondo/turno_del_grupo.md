---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: turno_del_grupo
modulo: "04 — Entregas de Fondo"
clave_primaria: [turno_id]
columnas: 7
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `turno_del_grupo`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `turno_id` | UUID | PK | no | PK |
| `grupo_id` | UUID | IDX | no | IDX |
| `periodo_id` | UUID | UQ | no | UQ |
| `cupo_id` | UUID | — | no | — |
| `titular_participante_id` | UUID | — | no | — |
| `version` | INTEGER | — | no | CK: >= 0 |
| `recibido_en` | TIMESTAMPTZ | — | no | — |

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
