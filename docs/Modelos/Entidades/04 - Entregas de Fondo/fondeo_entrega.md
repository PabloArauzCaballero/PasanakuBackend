---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: fondeo_entrega
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 16
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `fondeo_entrega`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `entrega_id` | UUID | FK UQ | no | FK, UQ |
| `turno_id` | UUID | FK UQ | no | FK, UQ |
| `moneda` | CHAR(3) | — | no | — |
| `monto_pozo` | DECIMAL(14,2) | — | no | CK: > 0 |
| `monto_confirmado` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_cubierto_mutual` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_faltante` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_cubierto_empresa` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `monto_pendiente` | DECIMAL(14,2) | — | no | CK: >= 0 |
| `cobertura_respaldo_id` | UUID | — | sí | NULL |
| `estado` | VARCHAR(15) | IDX | no | CK: FONDEADO|CON_PENDIENTE, IDX |
| `corte_en` | TIMESTAMPTZ | — | no | — |
| `fondeada_en` | TIMESTAMPTZ | — | sí | NULL |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_fondeo_entrega_cuadra` | CHECK | `monto_confirmado`, `monto_cubierto_mutual`, `monto_faltante`, `monto_pozo` |
| `ck_fondeo_entrega_estado` | CHECK | `estado`, `fondeada_en`, `monto_pendiente` |
| `ck_fondeo_entrega_pendiente` | CHECK | `monto_cubierto_empresa`, `monto_faltante`, `monto_pendiente` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `entrega_id` | [[entrega_fondo]] | 04 | no | [[fondeo_entrega.entrega_id → entrega_fondo]] |
| `turno_id` | [[turno]] | ↗ 02 | no | [[fondeo_entrega.turno_id → turno]] |

## Entidades vecinas

[[entrega_fondo]] · [[turno]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
