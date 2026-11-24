---
tags:
  - entidad
  - modulo/06-transparencia-y-reputacion
tabla: etapa
clase: Etapa
modulo: "06 — Transparencia y Reputación"
clave_primaria: [id]
columnas: 7
fk_salientes: 1
fk_entrantes: 1
append_only: false
---

# `etapa`

> Módulo [[06_transparencia_reputacion|06 — Transparencia y Reputación]] · clase `Etapa`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `modelo_id` | UUID | FK IDX | no | FK, IDX |
| `codigo` | VARCHAR(30) | UQ | no | UQ+modelo_id |
| `nombre` | VARCHAR(40) | — | no | — |
| `orden` | SMALLINT | UQ | no | UQ+modelo_id, CK: >= 0 |
| `desde` | DECIMAL(6,2) | — | no | CK: >= 0 |
| `hasta` | DECIMAL(6,2) | — | no | CK: > 0 |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_etapa_tramo` | CHECK | `desde`, `hasta` |
| `ex_etapa_sin_solape` | EXCLUDE | `desde`, `hasta`, `modelo_id` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `modelo_id` | [[modelo_scoring]] | 06 | no | [[etapa.modelo_id → modelo_scoring]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[desbloqueo_etapa]] | `etapa_id` | 06 | [[desbloqueo_etapa.etapa_id → etapa]] |

## Entidades vecinas

[[desbloqueo_etapa]] · [[modelo_scoring]]

## Notas del modelo

> Tramos CONTINUOS del puntaje de su modelo:
> [desde, hasta), y la ultima incluye su hasta.
> Sin solape por modelo (R-REP-07); un hueco
> entre dos etapas lo delata la verificacion.
> La etapa no se guarda en el puntaje: se deriva.
> El desbloqueo solo limita en estado VIGENTE.

## Ver también

- Justificación de negocio: [[06_transparencia_reputacion]]
- Diagramas: `docs/entidades/06_transparencia_reputacion.puml`
- Índice: [[_Entidades]] · [[Index]]
