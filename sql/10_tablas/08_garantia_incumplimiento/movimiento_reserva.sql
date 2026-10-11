-- movimiento_reserva · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.movimiento_reserva (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  reserva_respaldo_id                UUID NOT NULL,
  tipo                               VARCHAR(25) NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  disponible_resultante              NUMERIC(16,2) NOT NULL,
  exposicion_resultante              NUMERIC(16,2) NOT NULL,
  referencia_tipo                    VARCHAR(30) NOT NULL,
  referencia_id                      UUID NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  registrado_por                     UUID NOT NULL,
  responsable_id                     UUID NOT NULL,
  fecha                              TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_movimiento_reserva PRIMARY KEY (id),
  CONSTRAINT ck_movimiento_reserva_tipo CHECK (tipo IN ('RESERVA', 'AMPLIACION', 'APLICACION', 'REVERSA_APLICACION', 'RECUPERACION', 'LIBERACION')),
  CONSTRAINT ck_movimiento_reserva_monto CHECK (monto > 0),
  CONSTRAINT ck_movimiento_reserva_disponible_resultante CHECK (disponible_resultante >= 0),
  CONSTRAINT ck_movimiento_reserva_exposicion_resultante CHECK (exposicion_resultante >= 0)
);

COMMENT ON TABLE garantia.movimiento_reserva IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. [append-only] El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.movimiento_reserva.id IS 'PK';
COMMENT ON COLUMN garantia.movimiento_reserva.reserva_respaldo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.movimiento_reserva.tipo IS 'CK: RESERVA|AMPLIACION|APLICACION|REVERSA_APLICACION|RECUPERACION|LIBERACION, IDX';
COMMENT ON COLUMN garantia.movimiento_reserva.monto IS 'CK: > 0';
COMMENT ON COLUMN garantia.movimiento_reserva.disponible_resultante IS 'CK: >= 0';
COMMENT ON COLUMN garantia.movimiento_reserva.exposicion_resultante IS 'CK: >= 0';
COMMENT ON COLUMN garantia.movimiento_reserva.referencia_id IS 'IDX';
COMMENT ON COLUMN garantia.movimiento_reserva.clave_idempotencia IS 'UQ';
COMMENT ON COLUMN garantia.movimiento_reserva.registrado_por IS 'FK';
COMMENT ON COLUMN garantia.movimiento_reserva.responsable_id IS 'FK';
COMMENT ON COLUMN garantia.movimiento_reserva.fecha IS 'IDX';
