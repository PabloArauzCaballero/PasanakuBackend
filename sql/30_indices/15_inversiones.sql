-- Índices y restricciones de unicidad del módulo 15 — Inversiones voluntarias
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE UNIQUE INDEX IF NOT EXISTS uq_producto_inversion_codigo
  ON inversiones.producto_inversion (codigo);

CREATE INDEX IF NOT EXISTS ix_producto_inversion_tipo
  ON inversiones.producto_inversion (tipo);

CREATE INDEX IF NOT EXISTS ix_producto_inversion_estado
  ON inversiones.producto_inversion (estado);

CREATE INDEX IF NOT EXISTS ix_version_condiciones_producto_inversion_id
  ON inversiones.version_condiciones (producto_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_version_condiciones_producto_inversion_id_numero
  ON inversiones.version_condiciones (producto_inversion_id, numero);

CREATE INDEX IF NOT EXISTS ix_consentimiento_inversion_usuario_id
  ON inversiones.consentimiento_inversion (usuario_id);

CREATE INDEX IF NOT EXISTS ix_consentimiento_inversion_version_condiciones_id
  ON inversiones.consentimiento_inversion (version_condiciones_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_consentimiento_inversion_usuario_id_clave_idempotencia
  ON inversiones.consentimiento_inversion (usuario_id, clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_orden_inversion_usuario_id
  ON inversiones.orden_inversion (usuario_id);

CREATE INDEX IF NOT EXISTS ix_orden_inversion_producto_inversion_id
  ON inversiones.orden_inversion (producto_inversion_id);

CREATE INDEX IF NOT EXISTS ix_orden_inversion_version_condiciones_id
  ON inversiones.orden_inversion (version_condiciones_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orden_inversion_consentimiento_inversion_id
  ON inversiones.orden_inversion (consentimiento_inversion_id);

CREATE INDEX IF NOT EXISTS ix_orden_inversion_estado
  ON inversiones.orden_inversion (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uq_orden_inversion_usuario_id_clave_idempotencia
  ON inversiones.orden_inversion (usuario_id, clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_instruccion_libro_origen_tipo
  ON inversiones.instruccion_libro (origen_tipo);

CREATE INDEX IF NOT EXISTS ix_instruccion_libro_origen_id
  ON inversiones.instruccion_libro (origen_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_instruccion_libro_clave_idempotencia
  ON inversiones.instruccion_libro (clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_instruccion_libro_estado
  ON inversiones.instruccion_libro (estado);

CREATE INDEX IF NOT EXISTS ix_posicion_inversion_usuario_id
  ON inversiones.posicion_inversion (usuario_id);

CREATE INDEX IF NOT EXISTS ix_posicion_inversion_producto_inversion_id
  ON inversiones.posicion_inversion (producto_inversion_id);

CREATE INDEX IF NOT EXISTS ix_posicion_inversion_version_condiciones_id
  ON inversiones.posicion_inversion (version_condiciones_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_posicion_inversion_orden_inversion_id
  ON inversiones.posicion_inversion (orden_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_posicion_inversion_posicion_externa
  ON inversiones.posicion_inversion (posicion_externa);

CREATE INDEX IF NOT EXISTS ix_posicion_inversion_estado
  ON inversiones.posicion_inversion (estado);

CREATE INDEX IF NOT EXISTS ix_devengo_dpf_posicion_inversion_id
  ON inversiones.devengo_dpf (posicion_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_devengo_dpf_posicion_inversion_id_fecha
  ON inversiones.devengo_dpf (posicion_inversion_id, fecha);

CREATE INDEX IF NOT EXISTS ix_valor_cuota_producto_inversion_id
  ON inversiones.valor_cuota (producto_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_valor_cuota_producto_inversion_id_fecha
  ON inversiones.valor_cuota (producto_inversion_id, fecha);

CREATE INDEX IF NOT EXISTS ix_rescate_inversion_posicion_inversion_id
  ON inversiones.rescate_inversion (posicion_inversion_id);

CREATE INDEX IF NOT EXISTS ix_rescate_inversion_usuario_id
  ON inversiones.rescate_inversion (usuario_id);

CREATE INDEX IF NOT EXISTS ix_rescate_inversion_estado
  ON inversiones.rescate_inversion (estado);

CREATE UNIQUE INDEX IF NOT EXISTS uq_rescate_inversion_usuario_id_clave_idempotencia
  ON inversiones.rescate_inversion (usuario_id, clave_idempotencia);

CREATE INDEX IF NOT EXISTS ix_comprobante_inversion_posicion_inversion_id
  ON inversiones.comprobante_inversion (posicion_inversion_id);

CREATE INDEX IF NOT EXISTS ix_comprobante_inversion_usuario_id
  ON inversiones.comprobante_inversion (usuario_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_comprobante_inversion_origen_id
  ON inversiones.comprobante_inversion (origen_id);

CREATE INDEX IF NOT EXISTS ix_comision_exito_posicion_inversion_id
  ON inversiones.comision_exito (posicion_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_comision_exito_rescate_inversion_id
  ON inversiones.comision_exito (rescate_inversion_id);

CREATE INDEX IF NOT EXISTS ix_conciliacion_interes_posicion_inversion_id
  ON inversiones.conciliacion_interes (posicion_inversion_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_conciliacion_interes_rescate_inversion_id
  ON inversiones.conciliacion_interes (rescate_inversion_id);

CREATE INDEX IF NOT EXISTS ix_conciliacion_interes_estado
  ON inversiones.conciliacion_interes (estado);
