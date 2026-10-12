---
tags:
  - entidad
  - modulo/13-contabilidad-financiera-y-erp
  - append-only
tabla: pago_a_proveedor
clase: PagoAProveedor
modulo: "13 — Contabilidad Financiera y ERP"
clave_primaria: [id]
columnas: 9
fk_salientes: 3
fk_entrantes: 0
append_only: true
---

# `pago_a_proveedor`

> Módulo [[13_contabilidad_erp|13 — Contabilidad Financiera y ERP]] · clase `PagoAProveedor` · **append-only** (sin `UPDATE`/`DELETE`)

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `factura_proveedor_id` | UUID | FK IDX | no | FK, IDX |
| `monto` | DECIMAL(14,2) | — | no | CK: > 0 |
| `moneda` | CHAR(3) | — | no | — |
| `fecha_pago` | TIMESTAMPTZ | — | no | — |
| `forma_pago` | VARCHAR(20) | — | no | CK |
| `autorizado_por` | UUID | FK | no | FK |
| `clave_idempotencia` | UUID | — | sí | NULL |
| `asiento_contable_id` | UUID | FK | sí | FK, NULL, M3 |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_pago_a_proveedor_clave` | UNIQUE | `factura_proveedor_id`, `clave_idempotencia` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `asiento_contable_id` | [[asiento_contable]] | ↗ 03 | sí | [[pago_a_proveedor.asiento_contable_id → asiento_contable]] |
| `autorizado_por` | [[usuario]] | ↗ 01 | no | [[pago_a_proveedor.autorizado_por → usuario]] |
| `factura_proveedor_id` | [[factura_proveedor]] | 13 | no | [[pago_a_proveedor.factura_proveedor_id → factura_proveedor]] |

## Entidades vecinas

[[asiento_contable]] · [[factura_proveedor]] · [[usuario]]

## Ver también

- Justificación de negocio: [[13_contabilidad_erp]]
- Diagramas: `docs/entidades/13_contabilidad_erp.puml`
- Índice: [[_Entidades]] · [[Index]]
