---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: evidencia_step_up_consumida
clase: EvidenciaStepUpConsumida
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 6
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `evidencia_step_up_consumida`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `EvidenciaStepUpConsumida`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `jti` | UUID | UQ | no | UQ, consumo de un solo uso |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `proposito` | VARCHAR(20) | — | no | CK |
| `consumida_en` | TIMESTAMPTZ | — | no | — |
| `vence_en` | TIMESTAMPTZ | IDX | no | IDX |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `usuario_id` | [[usuario]] | 01 | no | [[evidencia_step_up_consumida.usuario_id → usuario]] |

## Entidades vecinas

[[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
