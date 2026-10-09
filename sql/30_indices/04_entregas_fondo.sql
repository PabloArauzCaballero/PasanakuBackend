-- Índices y restricciones de unicidad del módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE INDEX IF NOT EXISTS ix_entrega_fondo_grupo_id
  ON entregas.entrega_fondo (grupo_id);

CREATE INDEX IF NOT EXISTS ix_entrega_fondo_beneficiario_participante_id
  ON entregas.entrega_fondo (beneficiario_participante_id);

CREATE INDEX IF NOT EXISTS ix_entrega_fondo_estado
  ON entregas.entrega_fondo (estado);

CREATE INDEX IF NOT EXISTS ix_entrega_fondo_fecha_programada
  ON entregas.entrega_fondo (fecha_programada);

CREATE INDEX IF NOT EXISTS ix_deduccion_entrega_entrega_id
  ON entregas.deduccion_entrega (entrega_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_regla_entrega_codigo
  ON entregas.regla_entrega (codigo);

CREATE INDEX IF NOT EXISTS ix_validacion_pre_entrega_entrega_id
  ON entregas.validacion_pre_entrega (entrega_id);

CREATE INDEX IF NOT EXISTS ix_cuenta_bancaria_beneficiario_usuario_id
  ON entregas.cuenta_bancaria_beneficiario (usuario_id);

CREATE INDEX IF NOT EXISTS ix_orden_desembolso_entrega_id
  ON entregas.orden_desembolso (entrega_id);

CREATE INDEX IF NOT EXISTS ix_orden_desembolso_estado
  ON entregas.orden_desembolso (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orden_desembolso_referencia_proveedor
  ON entregas.orden_desembolso (referencia_proveedor);

CREATE INDEX IF NOT EXISTS ix_intento_desembolso_orden_desembolso_id
  ON entregas.intento_desembolso (orden_desembolso_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_confirmacion_recepcion_entrega_id
  ON entregas.confirmacion_recepcion (entrega_id);

CREATE INDEX IF NOT EXISTS ix_confirmacion_recepcion_estado
  ON entregas.confirmacion_recepcion (estado);

CREATE INDEX IF NOT EXISTS ix_confirmacion_recepcion_plazo_limite
  ON entregas.confirmacion_recepcion (plazo_limite);

CREATE INDEX IF NOT EXISTS ix_incidencia_entrega_entrega_id
  ON entregas.incidencia_entrega (entrega_id);

CREATE INDEX IF NOT EXISTS ix_incidencia_entrega_estado
  ON entregas.incidencia_entrega (estado);

CREATE INDEX IF NOT EXISTS ix_incidencia_entrega_fecha_limite_sla
  ON entregas.incidencia_entrega (fecha_limite_sla);

CREATE INDEX IF NOT EXISTS ix_historial_estado_entrega_entrega_id
  ON entregas.historial_estado_entrega (entrega_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_fondeo_entrega_entrega_id
  ON entregas.fondeo_entrega (entrega_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_fondeo_entrega_turno_id
  ON entregas.fondeo_entrega (turno_id);

CREATE INDEX IF NOT EXISTS ix_fondeo_entrega_estado
  ON entregas.fondeo_entrega (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uq_fondeo_entrega_clave_idempotencia
  ON entregas.fondeo_entrega (clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_oferta_turno_grupo_id
  ON entregas.oferta_turno (grupo_id);

CREATE INDEX IF NOT EXISTS ix_oferta_turno_turno_id
  ON entregas.oferta_turno (turno_id);

CREATE INDEX IF NOT EXISTS ix_oferta_turno_vendedor_usuario_id
  ON entregas.oferta_turno (vendedor_usuario_id);

CREATE INDEX IF NOT EXISTS ix_oferta_turno_estado
  ON entregas.oferta_turno (estado);

CREATE INDEX IF NOT EXISTS ix_oferta_turno_vigente_hasta
  ON entregas.oferta_turno (vigente_hasta);

CREATE UNIQUE INDEX IF NOT EXISTS uq_oferta_turno_clave_idempotencia
  ON entregas.oferta_turno (clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_cesion_derecho_oferta_turno_id
  ON entregas.cesion_derecho (oferta_turno_id);

CREATE INDEX IF NOT EXISTS ix_cesion_derecho_turno_id
  ON entregas.cesion_derecho (turno_id);

CREATE INDEX IF NOT EXISTS ix_cesion_derecho_comprador_usuario_id
  ON entregas.cesion_derecho (comprador_usuario_id);

CREATE INDEX IF NOT EXISTS ix_cesion_derecho_estado
  ON entregas.cesion_derecho (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uq_cesion_derecho_clave_idempotencia
  ON entregas.cesion_derecho (clave_idempotencia);
