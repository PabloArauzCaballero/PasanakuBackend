---
tags:
  - entidad
  - modulo/01-identidad-usuarios-y-seguridad
tabla: verificacion_kyc
clase: VerificacionKYC
modulo: "01 — Identidad, Usuarios y Seguridad"
clave_primaria: [id]
columnas: 16
fk_salientes: 3
fk_entrantes: 2
append_only: false
---

# `verificacion_kyc`

> Módulo [[01_identidad_usuarios|01 — Identidad, Usuarios y Seguridad]] · clase `VerificacionKYC`

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `documento_id` | UUID | FK | sí | FK, NULL |
| `nivel_solicitado` | VARCHAR(15) | — | no | CK |
| `estado` | VARCHAR(20) | — | no | CK |
| `proveedor` | VARCHAR(40) | — | sí | NULL |
| `referencia_proveedor` | VARCHAR(80) | — | sí | NULL |
| `puntaje_biometrico` | DECIMAL(5,2) | — | sí | NULL |
| `url_selfie` | VARCHAR(255) | — | sí | NULL |
| `url_perfil_izquierdo` | VARCHAR(255) | — | sí | NULL, perfil izquierdo de la prueba de vida |
| `url_perfil_derecho` | VARCHAR(255) | — | sí | NULL, perfil derecho de la prueba de vida |
| `motivo_rechazo` | VARCHAR(160) | — | sí | NULL |
| `revisada_por` | UUID | FK | sí | FK, NULL |
| `iniciada_en` | TIMESTAMPTZ | — | no | — |
| `resuelta_en` | TIMESTAMPTZ | — | sí | NULL |
| `vigente_hasta` | DATE | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `documento_id` | [[documento_identidad]] | 01 | sí | [[verificacion_kyc.documento_id → documento_identidad]] |
| `revisada_por` | [[usuario]] | 01 | sí | [[verificacion_kyc.revisada_por → usuario]] |
| `usuario_id` | [[usuario]] | 01 | no | [[verificacion_kyc.usuario_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[debida_diligencia]] | `verificacion_kyc_id` | ↗ 12 | [[debida_diligencia.verificacion_kyc_id → verificacion_kyc]] |
| [[solicitud_organizador]] | `kyc_reforzado_id` | ↗ 07 | [[solicitud_organizador.kyc_reforzado_id → verificacion_kyc]] |

## Entidades vecinas

[[debida_diligencia]] · [[documento_identidad]] · [[solicitud_organizador]] · [[usuario]]

## Ver también

- Justificación de negocio: [[01_identidad_usuarios]]
- Diagramas: `docs/entidades/01_identidad_usuarios.puml`
- Índice: [[_Entidades]] · [[Index]]
