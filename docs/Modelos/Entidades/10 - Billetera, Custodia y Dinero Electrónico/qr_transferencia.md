---
tags:
  - entidad
  - modulo/10-billetera-custodia-y-dinero-electronico
tabla: qr_transferencia
modulo: "10 — Billetera, Custodia y Dinero Electrónico"
clave_primaria: [id]
columnas: 11
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `qr_transferencia`

> Módulo [[10_billetera_custodia|10 — Billetera, Custodia y Dinero Electrónico]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `cuenta_billetera_id` | UUID | FK IDX | no | FK, IDX |
| `transaccion_id` | UUID | FK UQ | sí | FK, NULL, UQ |
| `modalidad` | VARCHAR(10) | — | no | CK: ESTATICO|DINAMICO |
| `monto` | DECIMAL(16,2) | — | sí | NULL, CK: > 0 |
| `moneda` | CHAR(3) | — | no | — |
| `concepto` | VARCHAR(140) | — | sí | NULL |
| `estado` | VARCHAR(10) | — | no | CK: VIGENTE|USADO|ANULADO |
| `expira_en` | TIMESTAMPTZ | — | sí | NULL |
| `creado_en` | TIMESTAMPTZ | — | no | — |
| `usado_en` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_qr_estatico_no_se_consume` | CHECK | `estado`, `modalidad` |
| `ck_qr_modalidad` | CHECK | `expira_en`, `modalidad`, `monto` |
| `ck_qr_uso` | CHECK | `estado`, `transaccion_id`, `usado_en` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `cuenta_billetera_id` | [[cuenta_billetera]] | 10 | no | [[qr_transferencia.cuenta_billetera_id → cuenta_billetera]] |
| `transaccion_id` | [[transaccion_billetera]] | 10 | sí | [[qr_transferencia.transaccion_id → transaccion_billetera]] |

## Entidades vecinas

[[cuenta_billetera]] · [[transaccion_billetera]]

## Ver también

- Justificación de negocio: [[10_billetera_custodia]]
- Diagramas: `docs/entidades/10_billetera_custodia.puml`
- Índice: [[_Entidades]] · [[Index]]
