---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: solicitud_cambio_rol
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 11
fk_salientes: 3
fk_entrantes: 0
append_only: false
---

# `solicitud_cambio_rol`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `tipo` | VARCHAR(10) | — | no | CK: CREAR|EDITAR|RETIRAR |
| `rol_id` | UUID | FK IDX | sí | NULL, FK, IDX |
| `propuesta` | JSONB | — | no | — |
| `motivo` | VARCHAR(160) | — | no | — |
| `estado` | VARCHAR(10) | — | no | CK: PENDIENTE|APROBADA|RECHAZADA |
| `propuesta_por` | UUID | FK IDX | no | FK, IDX |
| `propuesta_en` | TIMESTAMPTZ | — | no | — |
| `resuelta_por` | UUID | FK | sí | NULL, FK |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |
| `motivo_resolucion` | VARCHAR(160) | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_solicitud_cambio_rol_cuatro_ojos` | CHECK | `propuesta_por`, `resuelta_por` |
| `ck_solicitud_cambio_rol_resolucion` | CHECK | `estado`, `motivo_resolucion`, `resuelta_en`, `resuelta_por` |
| `ck_solicitud_cambio_rol_rol_requerido` | CHECK | `rol_id`, `tipo` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `propuesta_por` | [[usuario]] | 01 | no | [[solicitud_cambio_rol.propuesta_por → usuario]] |
| `resuelta_por` | [[usuario]] | 01 | sí | [[solicitud_cambio_rol.resuelta_por → usuario]] |
| `rol_id` | [[rol]] | 01 | sí | [[solicitud_cambio_rol.rol_id → rol]] |

## Entidades vecinas

[[rol]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
