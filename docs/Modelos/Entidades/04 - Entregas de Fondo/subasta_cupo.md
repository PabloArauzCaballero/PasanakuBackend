---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: subasta_cupo
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 20
fk_salientes: 3
fk_entrantes: 1
append_only: false
---

# `subasta_cupo`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `cupo_id` | UUID | FK IDX | no | FK, IDX |
| `participante_origen_id` | UUID | FK | no | FK |
| `vendedor_usuario_id` | UUID | IDX | no | IDX |
| `nombre_grupo` | VARCHAR(120) | — | no | — |
| `periodicidad` | VARCHAR(15) | — | no | — |
| `moneda` | CHAR(3) | — | no | — |
| `monto_aporte` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_base` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_ganador` | DECIMAL(14,2) | — | sí | NULL |
| `estado` | VARCHAR(12) | IDX | no | CK: ABIERTA|ADJUDICADA|LIQUIDANDO|VENDIDA|DESIERTA|CANCELADA|FALLIDA, IDX |
| `abierta_en` | TIMESTAMPTZ | — | no | — |
| `cierra_en` | TIMESTAMPTZ | IDX | no | IDX |
| `puja_ganadora_id` | UUID | — | sí | NULL |
| `traspaso_ref` | UUID | — | sí | NULL |
| `liquidacion_ref` | UUID | — | sí | NULL |
| `motivo_fallo` | VARCHAR(200) | — | sí | NULL |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `version` | INTEGER | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `cupo_id` | [[cupo]] | ↗ 02 | no | [[subasta_cupo.cupo_id → cupo]] |
| `grupo_id` | [[grupo]] | ↗ 02 | no | [[subasta_cupo.grupo_id → grupo]] |
| `participante_origen_id` | [[participante]] | ↗ 02 | no | [[subasta_cupo.participante_origen_id → participante]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[puja_subasta]] | `subasta_cupo_id` | 04 | [[puja_subasta.subasta_cupo_id → subasta_cupo]] |

## Entidades vecinas

[[cupo]] · [[grupo]] · [[participante]] · [[puja_subasta]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
