---
tags:
  - entidad
  - modulo/09-auditoria-reportes-y-cumplimiento
tabla: politica_retencion
clase: PoliticaRetencion
modulo: "09 — Auditoría, Reportes y Cumplimiento"
estereotipo: Política configurable
clave_primaria: [id]
columnas: 7
fk_salientes: 0
fk_entrantes: 1
append_only: false
---

# `politica_retencion`

> Módulo [[09_auditoria_reportes|09 — Auditoría, Reportes y Cumplimiento]] · clase `PoliticaRetencion` · Política configurable

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `entidad` | VARCHAR(50) | UQ | no | UQ |
| `meses_retencion_activa` | SMALLINT | — | no | — |
| `meses_retencion_historica` | SMALLINT | — | no | — |
| `accion_al_vencer` | VARCHAR(15) | — | no | CK |
| `base_legal` | VARCHAR(200) | — | no | — |
| `vigente_desde` | DATE | — | no | — |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[ficha_gobierno_tabla]] | `politica_retencion_id` | 09 | [[ficha_gobierno_tabla.politica_retencion_id → politica_retencion]] |

## Entidades vecinas

[[ficha_gobierno_tabla]]

## Ver también

- Justificación de negocio: [[09_auditoria_reportes]]
- Diagramas: `docs/entidades/09_auditoria_reportes.puml`
- Índice: [[_Entidades]] · [[Index]]
