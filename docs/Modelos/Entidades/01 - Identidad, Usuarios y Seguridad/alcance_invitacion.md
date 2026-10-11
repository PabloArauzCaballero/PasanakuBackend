---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: alcance_invitacion
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 10
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `alcance_invitacion`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `token_id` | UUID | FK UQ | no | FK, UQ |
| `grupo_destino_id` | UUID | — | no | — |
| `emisor_id` | UUID | — | no | — |
| `telefono_destino` | VARCHAR(20) | — | no | — |
| `nonce` | VARCHAR(64) | — | no | — |
| `clave_emision` | UUID | UQ | no | UQ |
| `huella_solicitud` | VARCHAR(64) | — | no | — |
| `clave_consumo` | UUID | — | sí | NULL |
| `consumidor_id` | UUID | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `token_id` | [[token_verificacion]] | 01 | no | [[alcance_invitacion.token_id → token_verificacion]] |

## Entidades vecinas

[[token_verificacion]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
