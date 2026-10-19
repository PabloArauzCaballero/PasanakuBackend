-- devengo_dpf · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.devengo_dpf (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  fecha                              DATE NOT NULL,
  dias_acumulados                    INTEGER NOT NULL,
  interes_acumulado                  NUMERIC(14,2) NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  creado_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_devengo_dpf PRIMARY KEY (id),
  CONSTRAINT ck_devengo_dpf_dias_acumulados CHECK (dias_acumulados >= 0),
  CONSTRAINT ck_devengo_dpf_interes_acumulado CHECK (interes_acumulado >= 0),
  CONSTRAINT ck_devengo_dpf_monto CHECK (monto >= 0)
);

COMMENT ON TABLE inversiones.devengo_dpf IS 'Módulo 15 — Inversiones voluntarias. [append-only] El interés acumulado de un depósito a plazo fijo, día por día, calculado por la plataforma.';
COMMENT ON COLUMN inversiones.devengo_dpf.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN inversiones.devengo_dpf.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.devengo_dpf.fecha IS 'UQ+posicion_inversion_id';
COMMENT ON COLUMN inversiones.devengo_dpf.dias_acumulados IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.devengo_dpf.interes_acumulado IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.devengo_dpf.monto IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.devengo_dpf.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
