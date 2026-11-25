---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: expediente_medida_cuenta
clase: ExpedienteMedidaCuenta
modulo: "01 — Identidad, Usuarios y Seguridad"
estereotipo: Raíz de agregado
clave_primaria: [id]
columnas: 24
fk_salientes: 4
fk_entrantes: 3
append_only: false
---

# `expediente_medida_cuenta`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `ExpedienteMedidaCuenta` · Raíz de agregado

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `politica_medida_cuenta_id` | UUID | FK | no | FK |
| `causal` | VARCHAR(40) | — | no | CK |
| `medida` | VARCHAR(25) | — | no | CK |
| `cautelar` | BOOLEAN | — | no | — |
| `motivo` | VARCHAR(500) | — | no | — |
| `estado` | VARCHAR(25) | IDX | no | CK, IDX |
| `estado_cuenta_previo` | VARCHAR(25) | — | no | CK |
| `propuesta_por` | UUID | FK | no | FK |
| `propuesta_en` | TIMESTAMPTZ | — | no | — |
| `notificada_en` | TIMESTAMPTZ | — | sí | NULL |
| `fecha_limite_descargo` | TIMESTAMPTZ | — | sí | NULL |
| `decidida_por` | UUID | FK | sí | FK, NULL |
| `decidida_en` | TIMESTAMPTZ | — | sí | NULL |
| `resultado_decision` | VARCHAR(15) | — | sí | CK, NULL |
| `fundamento_decision` | TEXT | — | sí | NULL |
| `fecha_limite_apelacion` | TIMESTAMPTZ | — | sí | NULL |
| `vigente_hasta` | TIMESTAMPTZ | — | sí | NULL |
| `prescribe_en` | TIMESTAMPTZ | — | no | — |
| `firme_en` | TIMESTAMPTZ | — | sí | NULL |
| `cerrada_en` | TIMESTAMPTZ | — | sí | NULL |
| `clave_idempotencia` | VARCHAR(80) | UQ | no | UQ |
| `version` | INTEGER | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_expediente_medida_decide_otro` | CHECK | `decidida_por`, `propuesta_por`, `usuario_id` |
| `ck_expediente_medida_decision_fundada` | CHECK | `decidida_en`, `decidida_por`, `fundamento_decision`, `resultado_decision` |
| `ck_expediente_medida_firme_confirmada` | CHECK | `firme_en`, `resultado_decision` |
| `ck_expediente_medida_no_autopropuesta` | CHECK | `propuesta_por`, `usuario_id` |
| `uq_expediente_medida_abierto` | UNIQUE parcial | `usuario_id`, `causal` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `decidida_por` | [[usuario]] | 01 | sí | [[expediente_medida_cuenta.decidida_por → usuario]] |
| `politica_medida_cuenta_id` | [[politica_medida_cuenta]] | 01 | no | [[expediente_medida_cuenta.politica_medida_cuenta_id → politica_medida_cuenta]] |
| `propuesta_por` | [[usuario]] | 01 | no | [[expediente_medida_cuenta.propuesta_por → usuario]] |
| `usuario_id` | [[usuario]] | 01 | no | [[expediente_medida_cuenta.usuario_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[apelacion_medida_cuenta]] | `expediente_medida_cuenta_id` | 01 | [[apelacion_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |
| [[evidencia_medida_cuenta]] | `expediente_medida_cuenta_id` | 01 | [[evidencia_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |
| [[historial_estado_medida_cuenta]] | `expediente_medida_cuenta_id` | 01 | [[historial_estado_medida_cuenta.expediente_medida_cuenta_id → expediente_medida_cuenta]] |

## Entidades vecinas

[[apelacion_medida_cuenta]] · [[evidencia_medida_cuenta]] · [[historial_estado_medida_cuenta]] · [[politica_medida_cuenta]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
