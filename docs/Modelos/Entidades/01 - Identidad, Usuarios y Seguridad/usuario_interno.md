---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: usuario_interno
clase: UsuarioInterno
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 7
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `usuario_interno`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `UsuarioInterno`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK UQ | no | FK, UQ |
| `departamento` | VARCHAR(60) | — | no | — |
| `cargo` | VARCHAR(80) | — | no | — |
| `debe_cambiar_contrasena` | BOOLEAN | — | no | — |
| `bloqueado_hasta` | TIMESTAMPTZ | — | sí | NULL |
| `motivo_bloqueo` | VARCHAR(160) | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `usuario_id` | [[usuario]] | 01 | no | [[usuario_interno.usuario_id → usuario]] |

## Entidades vecinas

[[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
