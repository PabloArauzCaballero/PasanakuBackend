-- conciliacion_interes · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.conciliacion_interes (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  rescate_inversion_id               UUID NOT NULL,
  periodo_desde                      DATE NOT NULL,
  periodo_hasta                      DATE NOT NULL,
  interes_devengado                  NUMERIC(14,2) NOT NULL,
  interes_externo                    NUMERIC(14,2) NOT NULL,
  retencion_calculada                NUMERIC(14,2) NOT NULL,
  retencion_externa                  NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  conciliada_en                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_conciliacion_interes PRIMARY KEY (id),
  CONSTRAINT ck_conciliacion_interes_interes_devengado CHECK (interes_devengado >= 0),
  CONSTRAINT ck_conciliacion_interes_interes_externo CHECK (interes_externo >= 0),
  CONSTRAINT ck_conciliacion_interes_retencion_calculada CHECK (retencion_calculada >= 0),
  CONSTRAINT ck_conciliacion_interes_retencion_externa CHECK (retencion_externa >= 0),
  CONSTRAINT ck_conciliacion_interes_estado CHECK (estado IN ('CONCILIADA', 'DISCREPANCIA'))
);

COMMENT ON TABLE inversiones.conciliacion_interes IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.conciliacion_interes.id IS 'PK';
COMMENT ON COLUMN inversiones.conciliacion_interes.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.conciliacion_interes.rescate_inversion_id IS 'FK, UQ';
COMMENT ON COLUMN inversiones.conciliacion_interes.interes_devengado IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.conciliacion_interes.interes_externo IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.conciliacion_interes.retencion_calculada IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.conciliacion_interes.retencion_externa IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.conciliacion_interes.estado IS 'CK: CONCILIADA | DISCREPANCIA, IDX';
