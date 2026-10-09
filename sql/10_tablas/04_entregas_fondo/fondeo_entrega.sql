-- fondeo_entrega · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.fondeo_entrega (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  entrega_id                         UUID NOT NULL,
  turno_id                           UUID NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_pozo                         NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_confirmado                   NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_cubierto_mutual              NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_faltante                     NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_cubierto_empresa             NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_pendiente                    NUMERIC(14,2) DEFAULT 0 NOT NULL,
  cobertura_respaldo_id              UUID,
  estado                             VARCHAR(15) NOT NULL,
  corte_en                           TIMESTAMPTZ NOT NULL,
  fondeada_en                        TIMESTAMPTZ,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_fondeo_entrega PRIMARY KEY (id),
  CONSTRAINT ck_fondeo_entrega_monto_pozo CHECK (monto_pozo > 0),
  CONSTRAINT ck_fondeo_entrega_monto_confirmado CHECK (monto_confirmado >= 0),
  CONSTRAINT ck_fondeo_entrega_monto_cubierto_mutual CHECK (monto_cubierto_mutual >= 0),
  CONSTRAINT ck_fondeo_entrega_monto_faltante CHECK (monto_faltante >= 0),
  CONSTRAINT ck_fondeo_entrega_monto_cubierto_empresa CHECK (monto_cubierto_empresa >= 0),
  CONSTRAINT ck_fondeo_entrega_monto_pendiente CHECK (monto_pendiente >= 0),
  CONSTRAINT ck_fondeo_entrega_estado CHECK (estado IN ('FONDEADO', 'CON_PENDIENTE'))
);

COMMENT ON TABLE entregas.fondeo_entrega IS 'Módulo 04 — Entregas de Fondo. Que la bolsa llegue completa, a la persona correcta, una sola vez';
COMMENT ON COLUMN entregas.fondeo_entrega.id IS 'PK';
COMMENT ON COLUMN entregas.fondeo_entrega.entrega_id IS 'FK, UQ';
COMMENT ON COLUMN entregas.fondeo_entrega.turno_id IS 'FK, UQ';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_pozo IS 'CK: > 0';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_confirmado IS 'CK: >= 0';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_cubierto_mutual IS 'CK: >= 0';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_faltante IS 'CK: >= 0';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_cubierto_empresa IS 'CK: >= 0';
COMMENT ON COLUMN entregas.fondeo_entrega.monto_pendiente IS 'CK: >= 0';
COMMENT ON COLUMN entregas.fondeo_entrega.cobertura_respaldo_id IS 'NULL';
COMMENT ON COLUMN entregas.fondeo_entrega.estado IS 'CK: FONDEADO|CON_PENDIENTE, IDX';
COMMENT ON COLUMN entregas.fondeo_entrega.fondeada_en IS 'NULL';
COMMENT ON COLUMN entregas.fondeo_entrega.clave_idempotencia IS 'UQ';
