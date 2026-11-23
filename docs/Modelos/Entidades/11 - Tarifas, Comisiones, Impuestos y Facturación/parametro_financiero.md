---
tags:
  - entidad
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
tabla: parametro_financiero
clase: ParametroFinanciero
modulo: "11 — Tarifas, Comisiones, Impuestos y Facturación"
clave_primaria: [id]
columnas: 5
fk_salientes: 0
fk_entrantes: 2
append_only: false
---

# `parametro_financiero`

> Módulo [[11_tarifas_comisiones|11 — Tarifas, Comisiones, Impuestos y Facturación]] · clase `ParametroFinanciero`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `codigo` | VARCHAR(50) | UQ | no | UQ |
| `descripcion` | VARCHAR(200) | — | no | — |
| `unidad` | VARCHAR(12) | — | sí | CK, NULL |
| `por_nivel` | BOOLEAN | — | no | — |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[parametro_congelado_grupo]] | `parametro_id` | 11 | [[parametro_congelado_grupo.parametro_id → parametro_financiero]] |
| [[version_parametro]] | `parametro_id` | 11 | [[version_parametro.parametro_id → parametro_financiero]] |

## Entidades vecinas

[[parametro_congelado_grupo]] · [[version_parametro]]

## Ver también

- Justificación de negocio: [[11_tarifas_comisiones]]
- Diagramas: `docs/entidades/11_tarifas_comisiones.puml`
- Índice: [[_Entidades]] · [[Index]]
