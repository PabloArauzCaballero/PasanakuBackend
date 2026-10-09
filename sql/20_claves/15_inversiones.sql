-- Claves foráneas del módulo 15 — Inversiones voluntarias
-- Generado por scripts/generar_ddl.py — no editar a mano.
-- Se aplican después de crear todas las tablas: el modelo tiene
-- referencias circulares entre módulos.
--
-- Cada una se borra si existe antes de crearse: PostgreSQL no tiene
-- ADD CONSTRAINT IF NOT EXISTS, y sql/aplicar.sql se aplica también
-- sobre una base que ya lo tiene. Borrar y volver a crear —en vez de
-- saltear si ya está— es lo que hace que un ON DELETE cambiado en el
-- modelo quede corregido al reaplicar.

ALTER TABLE inversiones.comision_exito DROP CONSTRAINT IF EXISTS fk_comision_exito_posicion_inversion_id;
ALTER TABLE inversiones.comision_exito
  ADD CONSTRAINT fk_comision_exito_posicion_inversion_id
  FOREIGN KEY (posicion_inversion_id) REFERENCES inversiones.posicion_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.comision_exito DROP CONSTRAINT IF EXISTS fk_comision_exito_rescate_inversion_id;
ALTER TABLE inversiones.comision_exito
  ADD CONSTRAINT fk_comision_exito_rescate_inversion_id
  FOREIGN KEY (rescate_inversion_id) REFERENCES inversiones.rescate_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.comprobante_inversion DROP CONSTRAINT IF EXISTS fk_comprobante_inversion_posicion_inversion_id;
ALTER TABLE inversiones.comprobante_inversion
  ADD CONSTRAINT fk_comprobante_inversion_posicion_inversion_id
  FOREIGN KEY (posicion_inversion_id) REFERENCES inversiones.posicion_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.conciliacion_interes DROP CONSTRAINT IF EXISTS fk_conciliacion_interes_posicion_inversion_id;
ALTER TABLE inversiones.conciliacion_interes
  ADD CONSTRAINT fk_conciliacion_interes_posicion_inversion_id
  FOREIGN KEY (posicion_inversion_id) REFERENCES inversiones.posicion_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.conciliacion_interes DROP CONSTRAINT IF EXISTS fk_conciliacion_interes_rescate_inversion_id;
ALTER TABLE inversiones.conciliacion_interes
  ADD CONSTRAINT fk_conciliacion_interes_rescate_inversion_id
  FOREIGN KEY (rescate_inversion_id) REFERENCES inversiones.rescate_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.consentimiento_inversion DROP CONSTRAINT IF EXISTS fk_consentimiento_inversion_version_condiciones_id;
ALTER TABLE inversiones.consentimiento_inversion
  ADD CONSTRAINT fk_consentimiento_inversion_version_condiciones_id
  FOREIGN KEY (version_condiciones_id) REFERENCES inversiones.version_condiciones (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.devengo_dpf DROP CONSTRAINT IF EXISTS fk_devengo_dpf_posicion_inversion_id;
ALTER TABLE inversiones.devengo_dpf
  ADD CONSTRAINT fk_devengo_dpf_posicion_inversion_id
  FOREIGN KEY (posicion_inversion_id) REFERENCES inversiones.posicion_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.orden_inversion DROP CONSTRAINT IF EXISTS fk_orden_inversion_consentimiento_inversion_id;
ALTER TABLE inversiones.orden_inversion
  ADD CONSTRAINT fk_orden_inversion_consentimiento_inversion_id
  FOREIGN KEY (consentimiento_inversion_id) REFERENCES inversiones.consentimiento_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.orden_inversion DROP CONSTRAINT IF EXISTS fk_orden_inversion_producto_inversion_id;
ALTER TABLE inversiones.orden_inversion
  ADD CONSTRAINT fk_orden_inversion_producto_inversion_id
  FOREIGN KEY (producto_inversion_id) REFERENCES inversiones.producto_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.orden_inversion DROP CONSTRAINT IF EXISTS fk_orden_inversion_version_condiciones_id;
ALTER TABLE inversiones.orden_inversion
  ADD CONSTRAINT fk_orden_inversion_version_condiciones_id
  FOREIGN KEY (version_condiciones_id) REFERENCES inversiones.version_condiciones (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.posicion_inversion DROP CONSTRAINT IF EXISTS fk_posicion_inversion_orden_inversion_id;
ALTER TABLE inversiones.posicion_inversion
  ADD CONSTRAINT fk_posicion_inversion_orden_inversion_id
  FOREIGN KEY (orden_inversion_id) REFERENCES inversiones.orden_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.posicion_inversion DROP CONSTRAINT IF EXISTS fk_posicion_inversion_producto_inversion_id;
ALTER TABLE inversiones.posicion_inversion
  ADD CONSTRAINT fk_posicion_inversion_producto_inversion_id
  FOREIGN KEY (producto_inversion_id) REFERENCES inversiones.producto_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.posicion_inversion DROP CONSTRAINT IF EXISTS fk_posicion_inversion_version_condiciones_id;
ALTER TABLE inversiones.posicion_inversion
  ADD CONSTRAINT fk_posicion_inversion_version_condiciones_id
  FOREIGN KEY (version_condiciones_id) REFERENCES inversiones.version_condiciones (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.rescate_inversion DROP CONSTRAINT IF EXISTS fk_rescate_inversion_posicion_inversion_id;
ALTER TABLE inversiones.rescate_inversion
  ADD CONSTRAINT fk_rescate_inversion_posicion_inversion_id
  FOREIGN KEY (posicion_inversion_id) REFERENCES inversiones.posicion_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.valor_cuota DROP CONSTRAINT IF EXISTS fk_valor_cuota_producto_inversion_id;
ALTER TABLE inversiones.valor_cuota
  ADD CONSTRAINT fk_valor_cuota_producto_inversion_id
  FOREIGN KEY (producto_inversion_id) REFERENCES inversiones.producto_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE inversiones.version_condiciones DROP CONSTRAINT IF EXISTS fk_version_condiciones_producto_inversion_id;
ALTER TABLE inversiones.version_condiciones
  ADD CONSTRAINT fk_version_condiciones_producto_inversion_id
  FOREIGN KEY (producto_inversion_id) REFERENCES inversiones.producto_inversion (id) ON DELETE RESTRICT ON UPDATE CASCADE;
