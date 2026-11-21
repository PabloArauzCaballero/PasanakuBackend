-- historial_estado_pago · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: HistorialEstadoPago
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.historial_estado_pago (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  pago_id                            UUID NOT NULL,
  estado_anterior                    VARCHAR(15) NOT NULL,
  estado_nuevo                       VARCHAR(15) NOT NULL,
  motivo                             VARCHAR(200) NOT NULL,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_pago PRIMARY KEY (id)
);

COMMENT ON TABLE aportes.historial_estado_pago IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Cada cambio de estado de un pago, como una fila nueva: de qué estado salió, a cuál llegó, por qué y cuándo.';
COMMENT ON COLUMN aportes.historial_estado_pago.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.historial_estado_pago.pago_id IS 'FK, IDX';
