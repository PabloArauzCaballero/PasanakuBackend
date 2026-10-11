-- comprobante_inversion · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.comprobante_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  usuario_id                         UUID NOT NULL,
  tipo                               VARCHAR(12) NOT NULL,
  origen_id                          UUID NOT NULL,
  sentido_titular                    VARCHAR(8) NOT NULL,
  principal                          NUMERIC(14,2) NOT NULL,
  interes                            NUMERIC(14,2) NOT NULL,
  impuesto                           NUMERIC(14,2) NOT NULL,
  comision                           NUMERIC(14,2) NOT NULL,
  perdida_realizada                  NUMERIC(14,2) DEFAULT 0 NOT NULL,
  neto                               NUMERIC(14,2) NOT NULL,
  costo_base                         NUMERIC(14,2) DEFAULT 0 NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  origen_datos                       VARCHAR(12) NOT NULL,
  emitido_en                         TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_comprobante_inversion PRIMARY KEY (id),
  CONSTRAINT ck_comprobante_inversion_tipo CHECK (tipo IN ('SUSCRIPCION', 'LIQUIDACION')),
  CONSTRAINT ck_comprobante_inversion_sentido_titular CHECK (sentido_titular IN ('DEBITO', 'CREDITO')),
  CONSTRAINT ck_comprobante_inversion_principal CHECK (principal >= 0),
  CONSTRAINT ck_comprobante_inversion_interes CHECK (interes >= 0),
  CONSTRAINT ck_comprobante_inversion_impuesto CHECK (impuesto >= 0),
  CONSTRAINT ck_comprobante_inversion_comision CHECK (comision >= 0),
  CONSTRAINT ck_comprobante_inversion_perdida_realizada CHECK (perdida_realizada >= 0),
  CONSTRAINT ck_comprobante_inversion_neto CHECK (neto >= 0),
  CONSTRAINT ck_comprobante_inversion_costo_base CHECK (costo_base >= 0),
  CONSTRAINT ck_comprobante_inversion_origen_datos CHECK (origen_datos IN ('SINTETICO', 'VERIFICADO'))
);

COMMENT ON TABLE inversiones.comprobante_inversion IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.comprobante_inversion.id IS 'PK';
COMMENT ON COLUMN inversiones.comprobante_inversion.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.comprobante_inversion.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.comprobante_inversion.tipo IS 'CK: SUSCRIPCION | LIQUIDACION';
COMMENT ON COLUMN inversiones.comprobante_inversion.origen_id IS 'UQ, polimorfica';
COMMENT ON COLUMN inversiones.comprobante_inversion.sentido_titular IS 'CK: DEBITO | CREDITO';
COMMENT ON COLUMN inversiones.comprobante_inversion.principal IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.interes IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.impuesto IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.comision IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.perdida_realizada IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.neto IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.costo_base IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comprobante_inversion.origen_datos IS 'CK: SINTETICO | VERIFICADO';
