---
tags:
  - entidad
  - modulo/06-transparencia-y-reputacion
tabla: desbloqueo_etapa
clase: DesbloqueoEtapa
modulo: "06 — Transparencia y Reputación"
clave_primaria: [id]
columnas: 6
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `desbloqueo_etapa`

> Módulo [[06_transparencia_reputacion|06 — Transparencia y Reputación]] · clase `DesbloqueoEtapa`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `etapa_id` | UUID | FK UQ | no | FK, UQ |
| `cupos_max` | SMALLINT | — | no | CK: 3-40 |
| `aporte_max` | DECIMAL(14,2) | — | no | CK: > 0 |
| `moneda` | CHAR(3) | — | no | CK: BOB|USD |
| `estado` | VARCHAR(10) | IDX | no | CK, IDX |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `etapa_id` | [[etapa]] | 06 | no | [[desbloqueo_etapa.etapa_id → etapa]] |

## Entidades vecinas

[[etapa]]

## Ver también

- Justificación de negocio: [[06_transparencia_reputacion]]
- Diagramas: `docs/entidades/06_transparencia_reputacion.puml`
- Índice: [[_Entidades]] · [[Index]]
