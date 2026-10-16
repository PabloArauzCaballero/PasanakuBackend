-- recuperacion_respaldo · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.recuperacion_respaldo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cobertura_respaldo_linea_id        UUID NOT NULL,
  pago_id                            UUID NOT NULL,
  movimiento_reserva_id              UUID NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  recuperada_en                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_recuperacion_respaldo PRIMARY KEY (id),
  CONSTRAINT ck_recuperacion_respaldo_monto CHECK (monto > 0)
);

COMMENT ON TABLE garantia.recuperacion_respaldo IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. [append-only] El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.recuperacion_respaldo.id IS 'PK';
COMMENT ON COLUMN garantia.recuperacion_respaldo.cobertura_respaldo_linea_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.recuperacion_respaldo.pago_id IS 'FK, UQ';
COMMENT ON COLUMN garantia.recuperacion_respaldo.movimiento_reserva_id IS 'FK';
COMMENT ON COLUMN garantia.recuperacion_respaldo.monto IS 'CK: > 0';
