---
tags:
  - entidad
  - modulo/03-aportes-pagos-qr-y-conciliacion
tabla: excepcion_conciliacion
clase: ExcepcionConciliacion
modulo: "03 — Aportes, Pagos QR y Conciliación"
clave_primaria: [id]
columnas: 11
fk_salientes: 2
fk_entrantes: 0
append_only: false
---

# `excepcion_conciliacion`

> Módulo [[03_aportes_pagos_qr|03 — Aportes, Pagos QR y Conciliación]] · clase `ExcepcionConciliacion`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `conciliacion_id` | UUID | FK IDX | no | FK, IDX |
| `tipo` | VARCHAR(30) | — | no | CK |
| `descripcion` | VARCHAR(300) | — | no | — |
| `monto_diferencia` | DECIMAL(14,2) | — | no | — |
| `moneda` | CHAR(3) | — | sí | NULL |
| `estado` | VARCHAR(15) | IDX | no | CK, IDX |
| `asignada_a` | UUID | FK | sí | FK, NULL |
| `resolucion` | VARCHAR(300) | — | sí | NULL |
| `abierta_en` | TIMESTAMPTZ | — | no | — |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |

## Reglas del catálogo

> Declaradas en [[Restricciones]], no en el modelo. El nombre es el que devuelve la base al rechazar.

| Regla | Tipo | Columnas |
| --- | :-: | --- |
| `ck_excepcion_conciliacion_moneda` | CHECK | `moneda` |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `asignada_a` | [[usuario]] | ↗ 01 | sí | [[excepcion_conciliacion.asignada_a → usuario]] |
| `conciliacion_id` | [[conciliacion]] | 03 | no | [[excepcion_conciliacion.conciliacion_id → conciliacion]] |

## Entidades vecinas

[[conciliacion]] · [[usuario]]

## Notas del modelo

> Toda excepcion abierta bloquea el cierre_diario:
> cuadrado = FALSE mientras exista al menos una
> excepcion sin resolver de esa fecha.

## Ver también

- Justificación de negocio: [[03_aportes_pagos_qr]]
- Diagramas: `docs/entidades/03_aportes_pagos_qr.puml`
- Índice: [[_Entidades]] · [[Index]]
