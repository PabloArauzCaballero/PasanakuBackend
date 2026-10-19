---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: rol
clase: Rol
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 7
fk_salientes: 0
fk_entrantes: 2
append_only: false
---

# `rol`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `Rol`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `codigo` | VARCHAR(30) | UQ | no | UQ |
| `nombre` | VARCHAR(60) | — | no | — |
| `ambito` | VARCHAR(15) | — | no | CK |
| `departamento` | VARCHAR(60) | — | sí | NULL |
| `descripcion` | VARCHAR(255) | — | sí | NULL |
| `es_sistema` | BOOLEAN | — | no | — |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[asignacion_rol]] | `rol_id` | 01 | [[asignacion_rol.rol_id → rol]] |
| [[rol_permiso]] | `rol_id` | 01 | [[rol_permiso.rol_id → rol]] |

## Entidades vecinas

[[asignacion_rol]] · [[rol_permiso]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
