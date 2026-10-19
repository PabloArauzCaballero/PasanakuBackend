---
tags:
  - entidad
  - modulo/09-auditoria-reportes-y-cumplimiento
tabla: revision_columna
clase: RevisionColumna
modulo: "09 — Auditoría, Reportes y Cumplimiento"
estereotipo: Política configurable
clave_primaria: [id]
columnas: 10
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `revision_columna`

> Módulo [[09_auditoria_reportes|09 — Auditoría, Reportes y Cumplimiento]] · clase `RevisionColumna` · Política configurable

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `esquema` | VARCHAR(63) | UQ | no | UQ+tabla+columna |
| `tabla` | VARCHAR(63) | — | no | — |
| `columna` | VARCHAR(63) | — | no | — |
| `decision` | VARCHAR(20) | — | no | CK |
| `confianza` | VARCHAR(10) | — | sí | CK, NULL |
| `motivo` | VARCHAR(500) | — | sí | NULL |
| `revisado_por` | UUID | — | no | — |
| `revisado_en` | TIMESTAMPTZ | — | no | — |
| `version` | INTEGER | — | no | — |

## Ver también

- Justificación de negocio: [[09_auditoria_reportes]]
- Diagramas: `docs/entidades/09_auditoria_reportes.puml`
- Índice: [[_Entidades]] · [[Index]]
