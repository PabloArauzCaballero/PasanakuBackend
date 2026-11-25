---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: politica_medida_cuenta
clase: PoliticaMedidaCuenta
modulo: "01 — Identidad, Usuarios y Seguridad"
estereotipo: Política configurable
clave_primaria: [id]
columnas: 10
fk_salientes: 1
fk_entrantes: 1
append_only: false
---

# `politica_medida_cuenta`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `PoliticaMedidaCuenta` · Política configurable

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `version` | VARCHAR(20) | UQ | no | UQ |
| `plazo_descargo_dias_habiles` | SMALLINT | — | no | CK: > 0 |
| `plazo_apelacion_dias_habiles` | SMALLINT | — | no | CK: > 0 |
| `plazo_resolucion_apelacion_dias_habiles` | SMALLINT | — | sí | NULL |
| `prescribe_en_meses` | SMALLINT | — | no | CK: > 0 |
| `duracion_maxima_suspension_dias` | SMALLINT | — | no | CK: > 0 |
| `fuente` | VARCHAR(200) | — | no | — |
| `aprobada_por` | UUID | FK | sí | FK, NULL |
| `vigente_desde` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `aprobada_por` | [[usuario]] | 01 | sí | [[politica_medida_cuenta.aprobada_por → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[expediente_medida_cuenta]] | `politica_medida_cuenta_id` | 01 | [[expediente_medida_cuenta.politica_medida_cuenta_id → politica_medida_cuenta]] |

## Entidades vecinas

[[expediente_medida_cuenta]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
