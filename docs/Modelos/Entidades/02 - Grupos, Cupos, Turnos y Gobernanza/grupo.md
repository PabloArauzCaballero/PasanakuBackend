---
tags:
  - entidad
  - modulo/02-grupos-cupos-turnos-y-gobernanza
tabla: grupo
clase: Grupo
modulo: "02 — Grupos, Cupos, Turnos y Gobernanza"
estereotipo: Raíz de agregado
clave_primaria: [id]
columnas: 27
fk_salientes: 1
fk_entrantes: 45
append_only: false
---

# `grupo`

> Módulo [[02_grupos_turnos|02 — Grupos, Cupos, Turnos y Gobernanza]] · clase `Grupo` · Raíz de agregado

## Columnas

| Columna | Tipo | Clave | Nulo | Anotaciones |
| --- | --- | --- | :-: | --- |
| `id` | UUID | PK | no | PK |
| `codigo_publico` | VARCHAR(12) | UQ | no | UQ |
| `nombre` | VARCHAR(120) | — | no | — |
| `descripcion` | VARCHAR(400) | — | sí | NULL |
| `monto_aporte` | DECIMAL(14,2) | — | no | CK: > 0 |
| `moneda` | CHAR(3) | — | no | — |
| `periodicidad` | VARCHAR(15) | — | no | CK |
| `dia_cobro` | SMALLINT | — | no | — |
| `num_periodos` | SMALLINT | — | no | CK: >= 3 |
| `cupos_totales` | SMALLINT | — | no | — |
| `cupos_ocupados` | SMALLINT | — | no | — |
| `fecha_inicio` | DATE | — | no | — |
| `fecha_fin_estimada` | DATE | — | no | — |
| `estado` | VARCHAR(30) | IDX | no | CK, IDX |
| `tipo_conformacion` | VARCHAR(30) | — | no | CK |
| `modalidad_turnos` | VARCHAR(25) | — | no | CK |
| `visibilidad` | VARCHAR(20) | — | no | CK |
| `organizador_id` | UUID | FK | sí | FK, NULL |
| `es_autogestionado` | BOOLEAN | — | no | — |
| `requiere_kyc_minimo` | VARCHAR(15) | — | no | — |
| `reputacion_minima` | DECIMAL(6,2) | — | no | — |
| `dias_gracia` | SMALLINT | — | no | — |
| `aplica_recargo_mora` | BOOLEAN | — | no | — |
| `usa_fondo_garantia` | BOOLEAN | — | no | — |
| `porcentaje_fondo_garantia` | DECIMAL(5,2) | — | no | — |
| `quorum_decisiones` | DECIMAL(4,3) | — | no | — |
| `cancelado_en` | TIMESTAMPTZ | — | sí | NULL |

## Claves foráneas salientes

| Columna | Referencia a | Módulo | Opcional | Relación |
| --- | --- | :-: | :-: | --- |
| `organizador_id` | [[organizador]] | ↗ 07 | sí | [[grupo.organizador_id → organizador]] |

## Referenciada por

| Entidad | Columna | Módulo | Relación |
| --- | --- | :-: | --- |
| [[acuerdo]] | `grupo_id` | 02 | [[acuerdo.grupo_id → grupo]] |
| [[alerta_cumplimiento]] | `grupo_id` | ↗ 09 | [[alerta_cumplimiento.grupo_id → grupo]] |
| [[alerta_temprana]] | `grupo_id` | ↗ 08 | [[alerta_temprana.grupo_id → grupo]] |
| [[asiento_contable]] | `grupo_id` | ↗ 03 | [[asiento_contable.grupo_id → grupo]] |
| [[asignacion_tarifario]] | `grupo_id` | ↗ 11 | [[asignacion_tarifario.grupo_id → grupo]] |
| [[aval_participante]] | `grupo_id` | ↗ 08 | [[aval_participante.grupo_id → grupo]] |
| [[bitacora_evento]] | `grupo_id` | ↗ 09 | [[bitacora_evento.grupo_id → grupo]] |
| [[bloque_transparencia]] | `grupo_id` | ↗ 06 | [[bloque_transparencia.grupo_id → grupo]] |
| [[configuracion_grupo]] | `grupo_id` | 02 | [[configuracion_grupo.grupo_id → grupo]] |
| [[cuenta_billetera]] | `grupo_id` | ↗ 10 | [[cuenta_billetera.grupo_id → grupo]] |
| [[cuenta_contable]] | `grupo_id` | ↗ 03 | [[cuenta_contable.grupo_id → grupo]] |
| [[cupo]] | `grupo_id` | 02 | [[cupo.grupo_id → grupo]] |
| [[deuda_participante]] | `grupo_id` | ↗ 08 | [[deuda_participante.grupo_id → grupo]] |
| [[devengo_comision]] | `grupo_id` | ↗ 11 | [[devengo_comision.grupo_id → grupo]] |
| [[dia_no_habil]] | `grupo_id` | 02 | [[dia_no_habil.grupo_id → grupo]] |
| [[disolucion_anticipada]] | `grupo_id` | ↗ 08 | [[disolucion_anticipada.grupo_id → grupo]] |
| [[ejecucion_reporte]] | `grupo_id` | ↗ 09 | [[ejecucion_reporte.grupo_id → grupo]] |
| [[entrega_fondo]] | `grupo_id` | ↗ 04 | [[entrega_fondo.grupo_id → grupo]] |
| [[evento_reputacion]] | `grupo_id` | ↗ 06 | [[evento_reputacion.grupo_id → grupo]] |
| [[exencion_comision]] | `grupo_id` | ↗ 11 | [[exencion_comision.grupo_id → grupo]] |
| [[fondo_garantia]] | `grupo_id` | ↗ 08 | [[fondo_garantia.grupo_id → grupo]] |
| [[historial_estado_grupo]] | `grupo_id` | 02 | [[historial_estado_grupo.grupo_id → grupo]] |
| [[invitacion]] | `grupo_id` | 02 | [[invitacion.grupo_id → grupo]] |
| [[metrica_grupo]] | `grupo_id` | ↗ 06 | [[metrica_grupo.grupo_id → grupo]] |
| [[obligacion_aporte]] | `grupo_id` | ↗ 03 | [[obligacion_aporte.grupo_id → grupo]] |
| [[participante]] | `grupo_id` | 02 | [[participante.grupo_id → grupo]] |
| [[periodo]] | `grupo_id` | 02 | [[periodo.grupo_id → grupo]] |
| [[plan_contingencia]] | `grupo_id` | ↗ 08 | [[plan_contingencia.grupo_id → grupo]] |
| [[politica_cobertura]] | `grupo_id` | ↗ 08 | [[politica_cobertura.grupo_id → grupo]] |
| [[politica_mora]] | `grupo_id` | ↗ 03 | [[politica_mora.grupo_id → grupo]] |
| [[politica_sancion]] | `grupo_id` | ↗ 08 | [[politica_sancion.grupo_id → grupo]] |
| [[programacion_recordatorio]] | `grupo_id` | ↗ 05 | [[programacion_recordatorio.grupo_id → grupo]] |
| [[propuesta_grupo]] | `grupo_materializado_id` | 02 | [[propuesta_grupo.grupo_materializado_id → grupo]] |
| [[reemplazo_participante]] | `grupo_id` | ↗ 08 | [[reemplazo_participante.grupo_id → grupo]] |
| [[registro_incumplimiento]] | `grupo_id` | ↗ 08 | [[registro_incumplimiento.grupo_id → grupo]] |
| [[reglamento_grupo]] | `grupo_id` | 02 | [[reglamento_grupo.grupo_id → grupo]] |
| [[resena_participante]] | `grupo_id` | ↗ 06 | [[resena_participante.grupo_id → grupo]] |
| [[score_riesgo_incumplimiento]] | `grupo_id` | ↗ 08 | [[score_riesgo_incumplimiento.grupo_id → grupo]] |
| [[solicitud_ingreso]] | `grupo_id` | 02 | [[solicitud_ingreso.grupo_id → grupo]] |
| [[sorteo_turnos]] | `grupo_id` | 02 | [[sorteo_turnos.grupo_id → grupo]] |
| [[tarea_automatizada]] | `grupo_id` | ↗ 07 | [[tarea_automatizada.grupo_id → grupo]] |
| [[tarifa_congelada_grupo]] | `grupo_id` | ↗ 11 | [[tarifa_congelada_grupo.grupo_id → grupo]] |
| [[transaccion_billetera]] | `grupo_id` | ↗ 10 | [[transaccion_billetera.grupo_id → grupo]] |
| [[transferencia_p2p]] | `grupo_id` | ↗ 10 | [[transferencia_p2p.grupo_id → grupo]] |
| [[turno]] | `grupo_id` | 02 | [[turno.grupo_id → grupo]] |

## Entidades vecinas

[[acuerdo]] · [[alerta_cumplimiento]] · [[alerta_temprana]] · [[asiento_contable]] · [[asignacion_tarifario]] · [[aval_participante]] · [[bitacora_evento]] · [[bloque_transparencia]] · [[configuracion_grupo]] · [[cuenta_billetera]] · [[cuenta_contable]] · [[cupo]] · [[deuda_participante]] · [[devengo_comision]] · [[dia_no_habil]] · [[disolucion_anticipada]] · [[ejecucion_reporte]] · [[entrega_fondo]] · [[evento_reputacion]] · [[exencion_comision]] · [[fondo_garantia]] · [[historial_estado_grupo]] · [[invitacion]] · [[metrica_grupo]] · [[obligacion_aporte]] · [[organizador]] · [[participante]] · [[periodo]] · [[plan_contingencia]] · [[politica_cobertura]] · [[politica_mora]] · [[politica_sancion]] · [[programacion_recordatorio]] · [[propuesta_grupo]] · [[reemplazo_participante]] · [[registro_incumplimiento]] · [[reglamento_grupo]] · [[resena_participante]] · [[score_riesgo_incumplimiento]] · [[solicitud_ingreso]] · [[sorteo_turnos]] · [[tarea_automatizada]] · [[tarifa_congelada_grupo]] · [[transaccion_billetera]] · [[transferencia_p2p]] · [[turno]]

## Ver también

- Justificación de negocio: [[02_grupos_turnos]]
- Diagramas: `docs/entidades/02_grupos_turnos.puml`
- Índice: [[_Entidades]] · [[Index]]
