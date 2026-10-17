-- distribucion_interes · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.distribucion_interes (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  rescate_inversion_id               UUID NOT NULL,
  usuario_id                         UUID NOT NULL,
  periodo_desde                      DATE NOT NULL,
  periodo_hasta                      DATE NOT NULL,
  interes_devengado                  NUMERIC(14,2) NOT NULL,
  penalizacion                       NUMERIC(14,2) NOT NULL,
  interes_esperado                   NUMERIC(14,2) NOT NULL,
  interes_pagado                     NUMERIC(14,2) NOT NULL,
  retencion                          NUMERIC(14,2) NOT NULL,
  interes_neto                       NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  motivo_discrepancia                VARCHAR(40),
  distribuida_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_distribucion_interes PRIMARY KEY (id),
  CONSTRAINT ck_distribucion_interes_interes_devengado CHECK (interes_devengado >= 0),
  CONSTRAINT ck_distribucion_interes_penalizacion CHECK (penalizacion >= 0),
  CONSTRAINT ck_distribucion_interes_interes_esperado CHECK (interes_esperado >= 0),
  CONSTRAINT ck_distribucion_interes_interes_pagado CHECK (interes_pagado >= 0),
  CONSTRAINT ck_distribucion_interes_retencion CHECK (retencion >= 0),
  CONSTRAINT ck_distribucion_interes_interes_neto CHECK (interes_neto >= 0),
  CONSTRAINT ck_distribucion_interes_estado CHECK (estado IN ('CONCILIADA', 'DISCREPANCIA'))
);

COMMENT ON TABLE inversiones.distribucion_interes IS 'Módulo 15 — Inversiones voluntarias. [append-only] El reparto al titular del interés pagado por el emisor, por el período de su posición y con la retención del comprobante.';
COMMENT ON COLUMN inversiones.distribucion_interes.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN inversiones.distribucion_interes.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.distribucion_interes.rescate_inversion_id IS 'FK, UQ';
COMMENT ON COLUMN inversiones.distribucion_interes.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.distribucion_interes.interes_devengado IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.penalizacion IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.interes_esperado IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.interes_pagado IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.retencion IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.interes_neto IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.distribucion_interes.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN inversiones.distribucion_interes.estado IS 'CK: CONCILIADA | DISCREPANCIA, IDX';
COMMENT ON COLUMN inversiones.distribucion_interes.motivo_discrepancia IS 'NULL';
