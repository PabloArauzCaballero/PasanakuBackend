-- qr_transferencia · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.qr_transferencia (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cuenta_billetera_id                UUID NOT NULL,
  transaccion_id                     UUID,
  modalidad                          VARCHAR(10) NOT NULL,
  monto                              NUMERIC(16,2),
  moneda                             CHAR(3) NOT NULL,
  concepto                           VARCHAR(140),
  estado                             VARCHAR(10) NOT NULL,
  expira_en                          TIMESTAMPTZ,
  creado_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  usado_en                           TIMESTAMPTZ,
  CONSTRAINT pk_qr_transferencia PRIMARY KEY (id),
  CONSTRAINT ck_qr_transferencia_modalidad CHECK (modalidad IN ('ESTATICO', 'DINAMICO')),
  CONSTRAINT ck_qr_transferencia_monto CHECK (monto > 0),
  CONSTRAINT ck_qr_transferencia_estado CHECK (estado IN ('VIGENTE', 'USADO', 'ANULADO'))
);

COMMENT ON TABLE nucleo_financiero.qr_transferencia IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. El saldo no se guarda: se deriva, y todos los días cuadra contra el banco';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.id IS 'PK';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.cuenta_billetera_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.transaccion_id IS 'FK, NULL, UQ';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.modalidad IS 'CK: ESTATICO|DINAMICO';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.monto IS 'NULL, CK: > 0';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.concepto IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.estado IS 'CK: VIGENTE|USADO|ANULADO';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.expira_en IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.qr_transferencia.usado_en IS 'NULL';
