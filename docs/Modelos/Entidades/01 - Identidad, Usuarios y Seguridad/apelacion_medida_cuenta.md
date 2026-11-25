---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: apelacion_medida_cuenta
clase: ApelacionMedidaCuenta
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 10
fk_salientes: 3
fk_entrantes: 0
append_only: false
---

# `apelacion_medida_cuenta`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `ApelacionMedidaCuenta`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `expediente_medida_cuenta_id` | UUID | FK UQ | no | FK, UQ |
| `apelante_id` | UUID | FK | no | FK |
| `argumento` | TEXT | — | no | — |
| `estado` | VARCHAR(25) | — | no | CK |
| `presentada_en` | TIMESTAMPTZ | — | no | — |
| `fecha_limite_resolucion` | TIMESTAMPTZ | — | sí | NULL |
| `resuelta_por` | UUID | FK | sí | FK, NULL |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |
| `resolucion` | TEXT | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `apelante_id` | [[usuario]] | 01 | no | [[apelacion_medida_cuenta.apelante_id → usuario]] |
| `expediente_medida_cuenta_id` | [[expediente_medida_cuenta]] | 01 | no | [[apelacion_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |
| `resuelta_por` | [[usuario]] | 01 | sí | [[apelacion_medida_cuenta.resuelta_por → usuario]] |

## Entidades vecinas

[[expediente_medida_cuenta]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
