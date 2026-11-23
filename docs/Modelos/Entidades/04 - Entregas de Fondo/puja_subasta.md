---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: puja_subasta
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 10
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `puja_subasta`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `subasta_cupo_id` | UUID | FK IDX | no | FK, IDX |
| `postor_usuario_id` | UUID | IDX | no | IDX |
| `moneda` | CHAR(3) | — | no | — |
| `monto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `estado` | VARCHAR(12) | IDX | no | CK: PENDIENTE|VIGENTE|SUPERADA|LIBERADA|GANADORA|RECHAZADA, IDX |
| `retencion_ref` | UUID | — | sí | NULL |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `creada_en` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `subasta_cupo_id` | [[subasta_cupo]] | 04 | no | [[puja_subasta.subasta_cupo_id → subasta_cupo]] |

## Entidades vecinas

[[subasta_cupo]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
