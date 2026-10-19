---
tags:
  - entidad
  - modulo/09-auditoria-reportes-y-cumplimiento
tabla: ficha_gobierno_tabla
clase: FichaGobiernoTabla
modulo: "09 — Auditoría, Reportes y Cumplimiento"
estereotipo: Política configurable
clave_primaria: [id]
columnas: 12
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `ficha_gobierno_tabla`

> Módulo [[09_auditoria_reportes|09 — Auditoría, Reportes y Cumplimiento]] · clase `FichaGobiernoTabla` · Política configurable

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `esquema` | VARCHAR(63) | UQ | no | UQ+tabla |
| `tabla` | VARCHAR(63) | — | no | — |
| `estado` | VARCHAR(20) | — | no | CK |
| `estado_revision` | VARCHAR(20) | — | no | CK |
| `responsable` | VARCHAR(80) | — | sí | NULL |
| `politica_retencion_id` | UUID | FK | sí | FK, NULL |
| `critica_para_auditoria` | BOOLEAN | — | no | — |
| `nota_revision` | VARCHAR(500) | — | sí | NULL |
| `revisado_por` | UUID | — | sí | NULL |
| `actualizado_en` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `politica_retencion_id` | [[politica_retencion]] | 09 | sí | [[ficha_gobierno_tabla.politica_retencion_id → politica_retencion]] |

## Entidades vecinas

[[politica_retencion]]

## Ver también

- Justificación de negocio: [[09_auditoria_reportes]]
- Diagramas: `docs/entidades/09_auditoria_reportes.puml`
- Índice: [[_Entidades]] · [[Index]]
