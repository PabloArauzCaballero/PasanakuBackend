---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: oferta_turno
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 15
fk_salientes: 4
fk_entrantes: 1
append_only: false
---

# `oferta_turno`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `turno_id` | UUID | FK IDX | no | FK, IDX |
| `cupo_id` | UUID | FK | no | FK |
| `participante_origen_id` | UUID | FK | no | FK |
| `vendedor_usuario_id` | UUID | IDX | no | IDX |
| `moneda` | CHAR(3) | — | no | — |
| `monto_derecho` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_precio` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_cargos` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `estado` | VARCHAR(12) | IDX | no | CK: PUBLICADA|RESERVADA|LIQUIDANDO|VENDIDA|CANCELADA|VENCIDA, IDX |
| `vigente_hasta` | TIMESTAMPTZ | IDX | no | IDX |
| `publicada_en` | TIMESTAMPTZ | — | no | — |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_oferta_turno_cargos` | CHECK | `monto_cargos`, `monto_precio` |
| `uq_oferta_turno_activa` | UNIQUE parcial | `turno_id` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `cupo_id` | [[cupo]] | ↗ 02 | no | [[oferta_turno.cupo_id → cupo]] |
| `grupo_id` | [[grupo]] | ↗ 02 | no | [[oferta_turno.grupo_id → grupo]] |
| `participante_origen_id` | [[participante]] | ↗ 02 | no | [[oferta_turno.participante_origen_id → participante]] |
| `turno_id` | [[turno]] | ↗ 02 | no | [[oferta_turno.turno_id → turno]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[cesion_derecho]] | `oferta_turno_id` | 04 | [[cesion_derecho.oferta_turno_id → oferta_turno]] |

## Entidades vecinas

[[cesion_derecho]] · [[cupo]] · [[grupo]] · [[participante]] · [[turno]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
