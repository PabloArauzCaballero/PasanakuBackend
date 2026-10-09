---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: participante
clase: Participante
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
estereotipo: Raíz de agregado
clave_primaria: [id]
columnas: 13
fk_salientes: 3
fk_entrantes: 25
append_only: false
---

# `participante`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]] · clase `Participante` · Raíz de agregado

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `grupo_id` | UUID | FK IDX | no | FK, IDX |
| `usuario_id` | UUID | FK IDX | no | FK, IDX |
| `alias` | VARCHAR(60) | — | sí | NULL |
| `estado` | VARCHAR(30) | IDX | no | CK, IDX |
| `es_organizador` | BOOLEAN | — | no | — |
| `invitado_por_id` | UUID | FK | sí | FK, NULL |
| `fecha_ingreso` | TIMESTAMPTZ | — | no | — |
| `fecha_salida` | TIMESTAMPTZ | — | sí | NULL |
| `motivo_salida` | VARCHAR(160) | — | sí | NULL |
| `reputacion_al_ingresar` | DECIMAL(6,2) | — | no | — |
| `aportes_realizados` | SMALLINT | — | no | — |
| `aportes_en_mora` | SMALLINT | — | no | — |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `grupo_id` | [[grupo]] | 02 | no | [[participante.grupo_id → grupo]] |
| `invitado_por_id` | [[participante]] | 02 | sí | [[participante.invitado_por_id → participante]] |
| `usuario_id` | [[usuario]] | ↗ 01 | no | [[participante.usuario_id → usuario]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[aceptacion_reglamento]] | `participante_id` | 02 | [[aceptacion_reglamento.participante_id → participante]] |
| [[aval_participante]] | `participante_avalado_id` | ↗ 08 | [[aval_participante.participante_avalado_id → participante]] |
| [[cuenta_contable]] | `participante_id` | ↗ 03 | [[cuenta_contable.participante_id → participante]] |
| [[cupo]] | `participante_id` | 02 | [[cupo.participante_id → participante]] |
| [[descargo_participante]] | `participante_id` | ↗ 08 | [[descargo_participante.participante_id → participante]] |
| [[deuda_participante]] | `participante_id` | ↗ 08 | [[deuda_participante.participante_id → participante]] |
| [[devengo_comision]] | `participante_id` | ↗ 11 | [[devengo_comision.participante_id → participante]] |
| [[devolucion_fondo]] | `participante_id` | ↗ 08 | [[devolucion_fondo.participante_id → participante]] |
| [[entrega_fondo]] | `beneficiario_participante_id` | ↗ 04 | [[entrega_fondo.beneficiario_participante_id → participante]] |
| [[evento_reputacion]] | `participante_id` | ↗ 06 | [[evento_reputacion.participante_id → participante]] |
| [[liquidacion_participante]] | `participante_id` | ↗ 08 | [[liquidacion_participante.participante_id → participante]] |
| [[obligacion_aporte]] | `participante_id` | ↗ 03 | [[obligacion_aporte.participante_id → participante]] |
| [[participante]] | `invitado_por_id` | 02 | [[participante.invitado_por_id → participante]] |
| [[plan_regularizacion]] | `participante_id` | ↗ 03 | [[plan_regularizacion.participante_id → participante]] |
| [[reemplazo_participante]] | `participante_entrante_id` | ↗ 08 | [[reemplazo_participante.participante_entrante_id → participante]] |
| [[reemplazo_participante]] | `participante_saliente_id` | ↗ 08 | [[reemplazo_participante.participante_saliente_id → participante]] |
| [[registro_incumplimiento]] | `participante_id` | ↗ 08 | [[registro_incumplimiento.participante_id → participante]] |
| [[resena_participante]] | `autor_participante_id` | ↗ 06 | [[resena_participante.autor_participante_id → participante]] |
| [[sancion]] | `participante_id` | ↗ 08 | [[sancion.participante_id → participante]] |
| [[solicitud_permuta]] | `contraparte_id` | 02 | [[solicitud_permuta.contraparte_id → participante]] |
| [[solicitud_permuta]] | `solicitante_id` | 02 | [[solicitud_permuta.solicitante_id → participante]] |
| [[solicitud_retiro]] | `participante_id` | 02 | [[solicitud_retiro.participante_id → participante]] |
| [[traspaso_cupo]] | `participante_destino_id` | 02 | [[traspaso_cupo.participante_destino_id → participante]] |
| [[traspaso_cupo]] | `participante_origen_id` | 02 | [[traspaso_cupo.participante_origen_id → participante]] |
| [[voto_participante]] | `participante_id` | 02 | [[voto_participante.participante_id → participante]] |

## Entidades vecinas

[[aceptacion_reglamento]] · [[aval_participante]] · [[cuenta_contable]] · [[cupo]] · [[descargo_participante]] · [[deuda_participante]] · [[devengo_comision]] · [[devolucion_fondo]] · [[entrega_fondo]] · [[evento_reputacion]] · [[grupo]] · [[liquidacion_participante]] · [[obligacion_aporte]] · [[participante]] · [[plan_regularizacion]] · [[reemplazo_participante]] · [[registro_incumplimiento]] · [[resena_participante]] · [[sancion]] · [[solicitud_permuta]] · [[solicitud_retiro]] · [[traspaso_cupo]] · [[usuario]] · [[voto_participante]]

## Notas del modelo

> usuario_id -> usuario.id (modulo 1).
> UNIQUE (grupo_id, usuario_id) evita que la
> misma persona figure dos veces; para varias
> manos se crean varios registros en "cupo".

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
