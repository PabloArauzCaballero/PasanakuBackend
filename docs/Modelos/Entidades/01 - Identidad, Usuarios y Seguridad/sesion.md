---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: sesion
clase: Sesion
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 11
fk_salientes: 2
fk_entrantes: 1
append_only: false
---

# `sesion`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `Sesion`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `dispositivo_id` | UUID | FK | no | FK |
| `refresco_familia_id` | UUID | — | sí | NULL |
| `iniciada_en` | TIMESTAMPTZ | — | no | — |
| `ultima_actividad_en` | TIMESTAMPTZ | — | no | — |
| `expira_en` | TIMESTAMPTZ | IDX | no | IDX |
| `ip_origen` | INET | — | no | — |
| `geolocalizacion_aprox` | VARCHAR(80) | — | sí | NULL |
| `revocada_en` | TIMESTAMPTZ | — | sí | NULL |
| `motivo_revocacion` | VARCHAR(80) | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `dispositivo_id` | [[dispositivo]] | 01 | no | [[sesion.dispositivo_id → dispositivo]] |
| `usuario_id` | [[usuario]] | 01 | no | [[sesion.usuario_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[transaccion_billetera]] | `sesion_id` | ↗ 10 | [[transaccion_billetera.sesion_id → sesion]] |

## Entidades vecinas

[[dispositivo]] · [[transaccion_billetera]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
