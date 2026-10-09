---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: cesion_derecho
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 17
fk_salientes: 4
fk_entrantes: 0
append_only: false
---

# `cesion_derecho`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `oferta_turno_id` | UUID | FK IDX | no | FK, IDX |
| `turno_id` | UUID | FK IDX | no | FK, IDX |
| `participante_origen_id` | UUID | FK | no | FK |
| `participante_destino_id` | UUID | FK | no | FK |
| `comprador_usuario_id` | UUID | IDX | no | IDX |
| `moneda` | CHAR(3) | — | no | — |
| `monto_precio` | DECIMAL(14,2) | — | no | CK: > 0 |
| `estado` | VARCHAR(16) | IDX | no | CK: CREADA|VALIDADA|FONDOS_RETENIDOS|TITULO_ASIGNADO|LIQUIDADA|FALLIDA, IDX |
| `retencion_ref` | UUID | — | sí | NULL |
| `liquidacion_ref` | UUID | — | sí | NULL |
| `motivo_fallo` | VARCHAR(200) | — | sí | NULL |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `creada_en` | TIMESTAMPTZ | — | no | — |
| `titulo_asignado_en` | TIMESTAMPTZ | — | sí | NULL |
| `liquidada_en` | TIMESTAMPTZ | — | sí | NULL |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_cesion_derecho_fallo` | CHECK | `estado`, `motivo_fallo` |
| `ck_cesion_derecho_liquidada` | CHECK | `estado`, `liquidacion_ref`, `liquidada_en` |
| `ck_cesion_derecho_partes` | CHECK | `participante_destino_id`, `participante_origen_id` |
| `ck_cesion_derecho_retencion` | CHECK | `estado`, `retencion_ref` |
| `ck_cesion_derecho_titulo` | CHECK | `estado`, `titulo_asignado_en` |
| `uq_cesion_derecho_turno_viva` | UNIQUE parcial | `turno_id` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `oferta_turno_id` | [[oferta_turno]] | 04 | no | [[cesion_derecho.oferta_turno_id → oferta_turno]] |
| `participante_destino_id` | [[participante]] | ↗ 02 | no | [[cesion_derecho.participante_destino_id → participante]] |
| `participante_origen_id` | [[participante]] | ↗ 02 | no | [[cesion_derecho.participante_origen_id → participante]] |
| `turno_id` | [[turno]] | ↗ 02 | no | [[cesion_derecho.turno_id → turno]] |

## Entidades vecinas

[[oferta_turno]] · [[participante]] · [[turno]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
