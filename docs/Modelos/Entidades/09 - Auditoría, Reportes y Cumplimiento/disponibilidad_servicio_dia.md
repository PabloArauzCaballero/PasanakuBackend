---
tags:
  - entidad
  - modulo/09-auditoria-reportes-y-cumplimiento
tabla: disponibilidad_servicio_dia
clase: DisponibilidadServicioDia
modulo: "09 — Auditoría, Reportes y Cumplimiento"
clave_primaria: [id]
columnas: 7
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `disponibilidad_servicio_dia`

> Módulo [[09_auditoria_reportes|09 — Auditoría, Reportes y Cumplimiento]] · clase `DisponibilidadServicioDia`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `servicio` | VARCHAR(60) | UQ | no | UQ+dia |
| `dia` | DATE | — | no | — |
| `minutos_medidos` | INTEGER | — | no | — |
| `minutos_arriba` | INTEGER | — | no | — |
| `ultima_interrupcion_en` | TIMESTAMPTZ | — | sí | NULL |
| `ultimo_minuto_en` | TIMESTAMPTZ | — | no | — |

## Ver también

- Justificación de negocio: [[09_auditoria_reportes]]
- Diagramas: `docs/entidades/09_auditoria_reportes.puml`
- Índice: [[_Entidades]] · [[Index]]
