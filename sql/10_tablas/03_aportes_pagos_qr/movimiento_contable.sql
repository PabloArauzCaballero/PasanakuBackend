-- movimiento_contable · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: MovimientoContable
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.movimiento_contable (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  asiento_id                         UUID NOT NULL,
  cuenta_id                          UUID NOT NULL,
  debe                               NUMERIC(16,2) NOT NULL,
  haber                              NUMERIC(16,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  descripcion                        VARCHAR(160) NOT NULL,
  CONSTRAINT pk_movimiento_contable PRIMARY KEY (id)
);

COMMENT ON TABLE nucleo_financiero.movimiento_contable IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Cada línea de un asiento: qué cuenta, cuánto al debe, cuánto al haber.';
COMMENT ON COLUMN nucleo_financiero.movimiento_contable.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.movimiento_contable.asiento_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.movimiento_contable.cuenta_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.movimiento_contable.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
