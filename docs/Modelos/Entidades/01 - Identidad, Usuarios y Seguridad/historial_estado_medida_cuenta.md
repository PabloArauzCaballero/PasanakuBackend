---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: historial_estado_medida_cuenta
clase: HistorialEstadoMedidaCuenta
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 7
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `historial_estado_medida_cuenta`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `HistorialEstadoMedidaCuenta`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `expediente_medida_cuenta_id` | UUID | FK IDX | no | FK, IDX |
| `estado_anterior` | VARCHAR(25) | — | sí | CK, NULL |
| `estado_nuevo` | VARCHAR(25) | — | no | CK |
| `ejecutado_por` | UUID | FK | sí | FK, NULL |
| `motivo` | VARCHAR(500) | — | sí | NULL |
| `ocurrido_en` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `ejecutado_por` | [[usuario]] | 01 | sí | [[historial_estado_medida_cuenta.ejecutado_por → usuario]] |
| `expediente_medida_cuenta_id` | [[expediente_medida_cuenta]] | 01 | no | [[historial_estado_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |

## Entidades vecinas

[[expediente_medida_cuenta]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
