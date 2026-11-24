---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: cuenta_servicio
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 12
fk_salientes: 4
fk_entrantes: 0
append_only: false
---

# `cuenta_servicio`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK UQ | no | FK, UQ |
| `rol_id` | UUID | FK IDX | no | FK, IDX |
| `nombre` | VARCHAR(80) | — | no | — |
| `estado` | VARCHAR(10) | — | no | CK: ACTIVA|REVOCADA |
| `vence_en` | TIMESTAMPTZ | IDX | no | IDX |
| `creada_por` | UUID | FK | no | FK |
| `creada_en` | TIMESTAMPTZ | — | no | — |
| `credencial_rotada_en` | TIMESTAMPTZ | — | no | — |
| `revocada_por` | UUID | FK | sí | NULL, FK |
| `revocada_en` | TIMESTAMPTZ | — | sí | NULL |
| `motivo_revocacion` | VARCHAR(160) | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_cuenta_servicio_revocacion` | CHECK | `estado`, `motivo_revocacion`, `revocada_en`, `revocada_por` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `creada_por` | [[usuario]] | 01 | no | [[cuenta_servicio.creada_por → usuario]] |
| `revocada_por` | [[usuario]] | 01 | sí | [[cuenta_servicio.revocada_por → usuario]] |
| `rol_id` | [[rol]] | 01 | no | [[cuenta_servicio.rol_id → rol]] |
| `usuario_id` | [[usuario]] | 01 | no | [[cuenta_servicio.usuario_id → usuario]] |

## Entidades vecinas

[[rol]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
