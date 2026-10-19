---
tags:
  - entidad
  - modulo/05-notificaciones-y-comunicaciones
tabla: aviso_interno
clase: AvisoInterno
modulo: "05 — Notificaciones y Comunicaciones"
clave_primaria: [id]
columnas: 9
fk_salientes: 1
fk_entrantes: 0
append_only: false
---

# `aviso_interno`

> Módulo [[05_notificaciones|05 — Notificaciones y Comunicaciones]] · clase `AvisoInterno`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK IDX | sí | FK, NULL, IDX |
| `rol_destinatario` | VARCHAR(50) | IDX | sí | NULL, IDX |
| `titulo` | VARCHAR(150) | — | no | — |
| `mensaje` | TEXT | — | no | — |
| `tipo` | VARCHAR(20) | — | no | CK: INFO|ALERTA|CASO|REVISION|DESPLIEGUE|SISTEMA |
| `enlace` | VARCHAR(300) | — | sí | NULL |
| `leido_en` | TIMESTAMPTZ | IDX | sí | NULL, IDX |
| `creado_en` | TIMESTAMPTZ | IDX | no | IDX |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `usuario_id` | [[usuario]] | ↗ 01 | sí | [[aviso_interno.usuario_id → usuario]] |

## Entidades vecinas

[[usuario]]

## Ver también

- Justificación de negocio: [[05_notificaciones]]
- Diagramas: `docs/entidades/05_notificaciones.puml`
- Índice: [[_Entidades]] · [[Index]]
