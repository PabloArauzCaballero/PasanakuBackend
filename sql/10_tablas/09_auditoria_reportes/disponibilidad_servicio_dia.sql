-- disponibilidad_servicio_dia · módulo 09 — Auditoría, Reportes y Cumplimiento
-- clase de dominio: DisponibilidadServicioDia
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS auditoria.disponibilidad_servicio_dia (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  servicio                           VARCHAR(60) NOT NULL,
  dia                                DATE NOT NULL,
  minutos_medidos                    INTEGER NOT NULL,
  minutos_arriba                     INTEGER NOT NULL,
  ultima_interrupcion_en             TIMESTAMPTZ,
  ultimo_minuto_en                   TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_disponibilidad_servicio_dia PRIMARY KEY (id)
);

COMMENT ON TABLE auditoria.disponibilidad_servicio_dia IS 'Módulo 09 — Auditoría, Reportes y Cumplimiento. El resumen de un día UTC de un servicio: cuántos minutos se le preguntó si estaba listo (minutosMedidos), en cuántos contestó que sí (minutosArriba) y la última vez que no (ultimaInterrupcionEn). auditoria lo suma una vez por minuto.';
COMMENT ON COLUMN auditoria.disponibilidad_servicio_dia.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN auditoria.disponibilidad_servicio_dia.servicio IS 'UQ+dia';
COMMENT ON COLUMN auditoria.disponibilidad_servicio_dia.ultima_interrupcion_en IS 'NULL';
