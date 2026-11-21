---
tags:
  - entidad
  - modulo/11-tarifas-comisiones-impuestos-y-facturacion
tabla: parametro_congelado_grupo
clase: ParametroCongeladoGrupo
modulo: "11 — Tarifas, Comisiones, Impuestos y Facturación"
estereotipo: Objeto de valor
clave_primaria: [id]
columnas: 6
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `parametro_congelado_grupo`

> Módulo [[11_tarifas_comisiones|11 — Tarifas, Comisiones, Impuestos y Facturación]] · clase `ParametroCongeladoGrupo` · Objeto de valor

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | IDX | no | IDX |
| `parametro_id` | UUID | FK | no | FK |
| `version_parametro_id` | UUID | FK | no | FK |
| `nivel_codigo` | VARCHAR(30) | — | sí | NULL |
| `congelado_en` | TIMESTAMPTZ | — | no | — |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `uq_parametro_congelado_grupo` | UNIQUE | expresión |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `parametro_id` | [[parametro_financiero]] | 11 | no | [[parametro_congelado_grupo.parametro_id → parametro_financiero]] |
| `version_parametro_id` | [[version_parametro]] | 11 | no | [[parametro_congelado_grupo.version_parametro_id → version_parametro]] |

## Entidades vecinas

[[parametro_financiero]] · [[version_parametro]]

## Ver también

- Justificación de negocio: [[11_tarifas_comisiones]]
- Diagramas: `docs/entidades/11_tarifas_comisiones.puml`
- Índice: [[_Entidades]] · [[Index]]
