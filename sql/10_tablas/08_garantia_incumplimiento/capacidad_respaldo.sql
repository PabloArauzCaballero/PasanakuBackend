-- capacidad_respaldo · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.capacidad_respaldo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  ambito                             VARCHAR(30) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_tope                         NUMERIC(16,2) DEFAULT 0 NOT NULL,
  monto_comprometido                 NUMERIC(16,2) DEFAULT 0 NOT NULL,
  responsable_id                     UUID NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_capacidad_respaldo PRIMARY KEY (id),
  CONSTRAINT ck_capacidad_respaldo_monto_tope CHECK (monto_tope >= 0),
  CONSTRAINT ck_capacidad_respaldo_monto_comprometido CHECK (monto_comprometido >= 0)
);

COMMENT ON TABLE garantia.capacidad_respaldo IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. El grupo no se detiene, pero la deuda no se perdona sola';
COMMENT ON COLUMN garantia.capacidad_respaldo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN garantia.capacidad_respaldo.ambito IS 'UQ+moneda';
COMMENT ON COLUMN garantia.capacidad_respaldo.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN garantia.capacidad_respaldo.monto_tope IS 'CK: >= 0';
COMMENT ON COLUMN garantia.capacidad_respaldo.monto_comprometido IS 'CK: >= 0';
COMMENT ON COLUMN garantia.capacidad_respaldo.responsable_id IS 'FK';
COMMENT ON COLUMN garantia.capacidad_respaldo.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
