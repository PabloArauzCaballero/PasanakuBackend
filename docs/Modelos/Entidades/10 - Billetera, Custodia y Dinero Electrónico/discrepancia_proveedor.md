---
tags:
  - entidad
  - modulo/10-billetera-custodia-y-dinero-electronico
tabla: discrepancia_proveedor
modulo: "10 — Billetera, Custodia y Dinero Electrónico"
clave_primaria: [id]
columnas: 11
fk_salientes: 0
fk_entrantes: 0
append_only: false
---

# `discrepancia_proveedor`

> Módulo [[10_billetera_custodia|10 — Billetera, Custodia y Dinero Electrónico]]

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `referencia_tipo` | VARCHAR(20) | — | no | CK: ORDEN_RECARGA|ORDEN_RETIRO |
| `referencia_id` | UUID | IDX | no | IDX, polimorfica |
| `tipo` | VARCHAR(30) | — | no | CK: FIRMA_INVALIDA|ESTADO_CONTRADICTORIO|MONTO_DISTINTO|REFERENCIA_DISTINTA|RESPUESTA_INVALIDA |
| `monto_esperado` | DECIMAL(16,2) | — | sí | NULL |
| `monto_informado` | DECIMAL(16,2) | — | sí | NULL |
| `moneda` | CHAR(3) | — | sí | NULL |
| `detalle` | VARCHAR(300) | — | no | — |
| `huella` | VARCHAR(64) | UQ | no | UQ+referencia_id+tipo |
| `correlacion_id` | UUID | — | no | — |
| `detectada_en` | TIMESTAMPTZ | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_discrepancia_moneda` | CHECK | `moneda`, `monto_esperado`, `monto_informado` |

## Ver también

- Justificación de negocio: [[10_billetera_custodia]]
- Diagramas: `docs/entidades/10_billetera_custodia.puml`
- Índice: [[_Entidades]] · [[Index]]
