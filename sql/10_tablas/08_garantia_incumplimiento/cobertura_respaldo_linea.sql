-- cobertura_respaldo_linea · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.cobertura_respaldo_linea (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cobertura_respaldo_id              UUID NOT NULL,
  obligacion_id                      UUID NOT NULL,
  monto_cubierto                     NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_recuperado                   NUMERIC(14,2) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_cobertura_respaldo_linea PRIMARY KEY (id),
  CONSTRAINT ck_cobertura_respaldo_linea_monto_cubierto CHECK (monto_cubierto > 0),
  CONSTRAINT ck_cobertura_respaldo_linea_monto_recuperado CHECK (monto_recuperado >= 0)
);

COMMENT ON TABLE garantia.cobertura_respaldo_linea IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.cobertura_respaldo_linea.id IS 'PK';
COMMENT ON COLUMN garantia.cobertura_respaldo_linea.cobertura_respaldo_id IS 'FK, IDX';
COMMENT ON COLUMN garantia.cobertura_respaldo_linea.obligacion_id IS 'FK, IDX, UQ+cobertura_respaldo_id';
COMMENT ON COLUMN garantia.cobertura_respaldo_linea.monto_cubierto IS 'CK: > 0';
COMMENT ON COLUMN garantia.cobertura_respaldo_linea.monto_recuperado IS 'CK: >= 0';
