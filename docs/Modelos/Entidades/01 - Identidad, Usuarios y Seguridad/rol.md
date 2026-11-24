---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: rol
clase: Rol
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 10
fk_salientes: 1
fk_entrantes: 4
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
| `creado_por` | UUID | FK | sí | NULL, FK, rol personalizado: quien lo creo (GU-16) |
| `creado_en` | TIMESTAMPTZ | — | sí | NULL |
| `retirado_en` | TIMESTAMPTZ | — | sí | NULL, un rol retirado no se asigna mas; no se borra (GU-16) |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `creado_por` | [[usuario]] | 01 | sí | [[rol.creado_por → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[asignacion_rol]] | `rol_id` | 01 | [[asignacion_rol.rol_id → rol]] |
| [[cuenta_servicio]] | `rol_id` | 01 | [[cuenta_servicio.rol_id → rol]] |
| [[rol_permiso]] | `rol_id` | 01 | [[rol_permiso.rol_id → rol]] |
| [[solicitud_cambio_rol]] | `rol_id` | 01 | [[solicitud_cambio_rol.rol_id → rol]] |

## Entidades vecinas

[[asignacion_rol]] · [[cuenta_servicio]] · [[rol_permiso]] · [[solicitud_cambio_rol]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
