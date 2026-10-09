---
tags:
  - entidad
  - modulo/10-billetera-custodia-y-dinero-electronico
tabla: evidencia_mfa_consumida
clase: EvidenciaMfaConsumida
modulo: "10 — Billetera, Custodia y Dinero Electrónico"
clave_primaria: [id]
columnas: 5
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `evidencia_mfa_consumida`

> Módulo [[10_billetera_custodia|10 — Billetera, Custodia y Dinero Electrónico]] · clase `EvidenciaMfaConsumida`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `jti` | UUID | UQ | no | UQ, consumo de un solo uso |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `proposito` | VARCHAR(20) | — | no | CK |
| `consumida_en` | TIMESTAMPTZ | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `usuario_id` | [[usuario]] | ↗ 01 | no | [[evidencia_mfa_consumida.usuario_id → usuario]] |

## Entidades vecinas

[[usuario]]

## Ver también

- Justificación de negocio: [[10_billetera_custodia]]
- Diagramas: `docs/entidades/10_billetera_custodia.puml`
- Índice: [[_Entidades]] · [[Index]]
