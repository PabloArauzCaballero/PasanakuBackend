-- cobertura_respaldo · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.cobertura_respaldo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  reserva_respaldo_id                UUID NOT NULL,
  grupo_id                           UUID NOT NULL,
  periodo_id                         UUID NOT NULL,
  turno_id                           UUID NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_pozo                         NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_confirmado                   NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_cubierto_mutual              NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_faltante                     NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_recuperado                   NUMERIC(14,2) DEFAULT 0 NOT NULL,
  estado                             VARCHAR(20) NOT NULL,
  corte_en                           TIMESTAMPTZ NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  solicitada_por                     UUID NOT NULL,
  responsable_id                     UUID NOT NULL,
  aplicada_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_cobertura_respaldo PRIMARY KEY (id),
  CONSTRAINT ck_cobertura_respaldo_monto_pozo CHECK (monto_pozo > 0),
  CONSTRAINT ck_cobertura_respaldo_monto_confirmado CHECK (monto_confirmado >= 0),
  CONSTRAINT ck_cobertura_respaldo_monto_cubierto_mutual CHECK (monto_cubierto_mutual >= 0),
  CONSTRAINT ck_cobertura_respaldo_monto_faltante CHECK (monto_faltante > 0),
  CONSTRAINT ck_cobertura_respaldo_monto_recuperado CHECK (monto_recuperado >= 0),
  CONSTRAINT ck_cobertura_respaldo_estado CHECK (estado IN ('APLICADA', 'RECUPERADA_PARCIAL', 'RECUPERADA_TOTAL', 'REVERSADA'))
);

COMMENT ON TABLE garantia.cobertura_respaldo IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.cobertura_respaldo.id IS 'PK';
COMMENT ON COLUMN garantia.cobertura_respaldo.reserva_respaldo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.cobertura_respaldo.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.cobertura_respaldo.periodo_id IS 'FK';
COMMENT ON COLUMN garantia.cobertura_respaldo.turno_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.cobertura_respaldo.monto_pozo IS 'CK: > 0';
COMMENT ON COLUMN garantia.cobertura_respaldo.monto_confirmado IS 'CK: >= 0';
COMMENT ON COLUMN garantia.cobertura_respaldo.monto_cubierto_mutual IS 'CK: >= 0';
COMMENT ON COLUMN garantia.cobertura_respaldo.monto_faltante IS 'CK: > 0';
COMMENT ON COLUMN garantia.cobertura_respaldo.monto_recuperado IS 'CK: >= 0';
COMMENT ON COLUMN garantia.cobertura_respaldo.estado IS 'CK: APLICADA|RECUPERADA_PARCIAL|RECUPERADA_TOTAL|REVERSADA, IDX';
COMMENT ON COLUMN garantia.cobertura_respaldo.clave_idempotencia IS 'UQ';
COMMENT ON COLUMN garantia.cobertura_respaldo.solicitada_por IS 'FK';
COMMENT ON COLUMN garantia.cobertura_respaldo.responsable_id IS 'FK';
