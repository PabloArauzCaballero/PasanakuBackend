---
tags:
  - entidad
  - modulo/04-entregas-de-fondo
tabla: incidencia_entrega
clase: IncidenciaEntrega
modulo: "04 — Entregas de Fondo"
clave_primaria: [id]
columnas: 14
fk_salientes: 3
fk_entrantes: 0
append_only: false
---

# `incidencia_entrega`

> Módulo [[04_entregas_fondo|04 — Entregas de Fondo]] · clase `IncidenciaEntrega`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `entrega_id` | UUID | FK IDX | no | FK, IDX |
| `tipo` | VARCHAR(35) | — | no | CK |
| `severidad` | VARCHAR(10) | — | no | CK |
| `descripcion` | TEXT | — | no | — |
| `reportada_por` | UUID | FK | no | FK |
| `asignada_a` | UUID | FK | sí | FK, NULL |
| `estado` | VARCHAR(15) | IDX | no | CK, IDX |
| `sla_horas` | SMALLINT | — | no | — |
| `fecha_limite_sla` | TIMESTAMPTZ | IDX | no | IDX |
| `resolucion` | VARCHAR(400) | — | sí | NULL |
| `evidencias` | JSONB | — | no | — |
| `abierta_en` | TIMESTAMPTZ | — | no | — |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `asignada_a` | [[usuario]] | ↗ 01 | sí | [[incidencia_entrega.asignada_a → usuario]] |
| `entrega_id` | [[entrega_fondo]] | 04 | no | [[incidencia_entrega.entrega_id → entrega_fondo]] |
| `reportada_por` | [[usuario]] | ↗ 01 | no | [[incidencia_entrega.reportada_por → usuario]] |

## Entidades vecinas

[[entrega_fondo]] · [[usuario]]

## Ver también

- Justificación de negocio: [[04_entregas_fondo]]
- Diagramas: `docs/entidades/04_entregas_fondo.puml`
- Índice: [[_Entidades]] · [[Index]]
