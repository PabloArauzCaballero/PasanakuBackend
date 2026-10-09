-- reserva_respaldo · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.reserva_respaldo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  capacidad_respaldo_id              UUID NOT NULL,
  grupo_id                           UUID NOT NULL,
  ciclo_numero                       SMALLINT NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_reservado                    NUMERIC(16,2) DEFAULT 0 NOT NULL,
  monto_aplicado                     NUMERIC(16,2) DEFAULT 0 NOT NULL,
  monto_recuperado                   NUMERIC(16,2) DEFAULT 0 NOT NULL,
  monto_liberado                     NUMERIC(16,2) DEFAULT 0 NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  responsable_id                     UUID NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  reservada_en                       TIMESTAMPTZ NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_reserva_respaldo PRIMARY KEY (id),
  CONSTRAINT ck_reserva_respaldo_ciclo_numero CHECK (ciclo_numero >= 1),
  CONSTRAINT ck_reserva_respaldo_monto_reservado CHECK (monto_reservado > 0),
  CONSTRAINT ck_reserva_respaldo_monto_aplicado CHECK (monto_aplicado >= 0),
  CONSTRAINT ck_reserva_respaldo_monto_recuperado CHECK (monto_recuperado >= 0),
  CONSTRAINT ck_reserva_respaldo_monto_liberado CHECK (monto_liberado >= 0),
  CONSTRAINT ck_reserva_respaldo_estado CHECK (estado IN ('VIGENTE', 'LIBERADA'))
);

COMMENT ON TABLE garantia.reserva_respaldo IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.reserva_respaldo.id IS 'PK';
COMMENT ON COLUMN garantia.reserva_respaldo.capacidad_respaldo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.reserva_respaldo.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.reserva_respaldo.ciclo_numero IS 'CK: >= 1, UQ+grupo_id';
COMMENT ON COLUMN garantia.reserva_respaldo.monto_reservado IS 'CK: > 0';
COMMENT ON COLUMN garantia.reserva_respaldo.monto_aplicado IS 'CK: >= 0';
COMMENT ON COLUMN garantia.reserva_respaldo.monto_recuperado IS 'CK: >= 0';
COMMENT ON COLUMN garantia.reserva_respaldo.monto_liberado IS 'CK: >= 0';
COMMENT ON COLUMN garantia.reserva_respaldo.estado IS 'CK: VIGENTE|LIBERADA, IDX';
COMMENT ON COLUMN garantia.reserva_respaldo.responsable_id IS 'FK';
COMMENT ON COLUMN garantia.reserva_respaldo.clave_idempotencia IS 'UQ';
