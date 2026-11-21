---
tags:
  - entidad
  - modulo/10-billetera-custodia-y-dinero-electronico
tabla: solicitud_reverso
clase: SolicitudReverso
modulo: "10 — Billetera, Custodia y Dinero Electrónico"
clave_primaria: [id]
columnas: 11
fk_salientes: 4
fk_entrantes: 0
append_only: false
---

# `solicitud_reverso`

> Módulo [[10_billetera_custodia|10 — Billetera, Custodia y Dinero Electrónico]] · clase `SolicitudReverso`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `transaccion_original_id` | UUID | FK IDX | no | FK, IDX |
| `solicitada_por` | UUID | FK | no | FK |
| `autorizada_por` | UUID | FK | sí | FK, NULL |
| `reverso_transaccion_id` | UUID | FK UQ | sí | FK, NULL, UQ |
| `clave_idempotencia` | VARCHAR(100) | UQ | no | UQ |
| `tipo` | VARCHAR(25) | — | no | CK |
| `motivo` | VARCHAR(300) | — | no | — |
| `estado` | VARCHAR(15) | — | no | CK |
| `solicitada_en` | TIMESTAMPTZ | — | no | — |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_solicitud_reverso_cuatro_ojos` | CHECK | `autorizada_por`, `solicitada_por` |
| `ck_solicitud_reverso_resuelta` | CHECK | `autorizada_por`, `estado`, `resuelta_en`, `reverso_transaccion_id` |
| `uq_solicitud_reverso_viva` | UNIQUE parcial | `transaccion_original_id` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `autorizada_por` | [[usuario]] | ↗ 01 | sí | [[solicitud_reverso.autorizada_por → usuario]] |
| `reverso_transaccion_id` | [[reverso_transaccion]] | 10 | sí | [[solicitud_reverso.reverso_transaccion_id → reverso_transaccion]] |
| `solicitada_por` | [[usuario]] | ↗ 01 | no | [[solicitud_reverso.solicitada_por → usuario]] |
| `transaccion_original_id` | [[transaccion_billetera]] | 10 | no | [[solicitud_reverso.transaccion_original_id → transaccion_billetera]] |

## Entidades vecinas

[[reverso_transaccion]] · [[transaccion_billetera]] · [[usuario]]

## Ver también

- Justificación de negocio: [[10_billetera_custodia]]
- Diagramas: `docs/entidades/10_billetera_custodia.puml`
- Índice: [[_Entidades]] · [[Index]]
