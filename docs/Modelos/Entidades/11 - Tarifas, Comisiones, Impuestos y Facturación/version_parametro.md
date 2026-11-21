---
tags:
  - entidad
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
tabla: version_parametro
clase: VersionParametro
modulo: "11 — Tarifas, Comisiones, Impuestos y Facturación"
clave_primaria: [id]
columnas: 15
fk_salientes: 1
fk_entrantes: 1
append_only: false
---

# `version_parametro`

> Módulo [[11_tarifas_comisiones|11 — Tarifas, Comisiones, Impuestos y Facturación]] · clase `VersionParametro`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `parametro_id` | UUID | FK IDX | no | FK, IDX |
| `nivel_codigo` | VARCHAR(30) | — | sí | NULL |
| `valor` | DECIMAL(16,6) | — | no | — |
| `moneda` | CHAR(3) | — | sí | NULL |
| `vigente_desde` | TIMESTAMPTZ | — | no | — |
| `procedencia_fuente` | VARCHAR(200) | — | no | — |
| `procedencia_referencia` | VARCHAR(300) | — | no | — |
| `procedencia_fecha` | DATE | — | no | — |
| `propuesto_por` | UUID | — | no | — |
| `propuesto_en` | TIMESTAMPTZ | — | no | — |
| `aprobado_por` | UUID | — | sí | NULL |
| `aprobado_en` | TIMESTAMPTZ | — | sí | NULL |
| `estado` | VARCHAR(12) | IDX | no | CK, IDX |
| `clave_idempotencia` | UUID | UQ | no | UQ |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_version_parametro_aprobada_completa` | CHECK | `aprobado_en`, `aprobado_por`, `estado` |
| `ck_version_parametro_cuatro_ojos` | CHECK | `aprobado_por`, `propuesto_por` |
| `ck_version_parametro_valor` | CHECK | `valor` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `parametro_id` | [[parametro_financiero]] | 11 | no | [[version_parametro.parametro_id → parametro_financiero]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[parametro_congelado_grupo]] | `version_parametro_id` | 11 | [[parametro_congelado_grupo.version_parametro_id → version_parametro]] |

## Entidades vecinas

[[parametro_congelado_grupo]] · [[parametro_financiero]]

## Ver también

- Justificación de negocio: [[11_tarifas_comisiones]]
- Diagramas: `docs/entidades/11_tarifas_comisiones.puml`
- Índice: [[_Entidades]] · [[Index]]
