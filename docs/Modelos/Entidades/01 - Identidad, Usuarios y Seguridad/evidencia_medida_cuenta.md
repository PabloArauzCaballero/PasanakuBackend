---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: evidencia_medida_cuenta
clase: EvidenciaMedidaCuenta
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 7
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `evidencia_medida_cuenta`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `EvidenciaMedidaCuenta`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `expediente_medida_cuenta_id` | UUID | FK IDX | no | FK, IDX |
| `tipo` | VARCHAR(25) | — | no | CK |
| `contenido` | TEXT | — | no | — |
| `referencia_id` | UUID | — | sí | NULL |
| `aportada_por` | UUID | FK | no | FK |
| `registrada_en` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `aportada_por` | [[usuario]] | 01 | no | [[evidencia_medida_cuenta.aportada_por → usuario]] |
| `expediente_medida_cuenta_id` | [[expediente_medida_cuenta]] | 01 | no | [[evidencia_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |

## Entidades vecinas

[[expediente_medida_cuenta]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
