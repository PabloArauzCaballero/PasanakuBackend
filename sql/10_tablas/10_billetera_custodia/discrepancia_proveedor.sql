-- discrepancia_proveedor · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.discrepancia_proveedor (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  referencia_tipo                    VARCHAR(20) NOT NULL,
  referencia_id                      UUID NOT NULL,
  tipo                               VARCHAR(30) NOT NULL,
  monto_esperado                     NUMERIC(16,2),
  monto_informado                    NUMERIC(16,2),
  moneda                             CHAR(3),
  detalle                            VARCHAR(300) NOT NULL,
  huella                             VARCHAR(64) NOT NULL,
  correlacion_id                     UUID NOT NULL,
  detectada_en                       TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_discrepancia_proveedor PRIMARY KEY (id),
  CONSTRAINT ck_discrepancia_proveedor_referencia_tipo CHECK (referencia_tipo IN ('ORDEN_RECARGA', 'ORDEN_RETIRO')),
  CONSTRAINT ck_discrepancia_proveedor_tipo CHECK (tipo IN ('FIRMA_INVALIDA', 'ESTADO_CONTRADICTORIO', 'MONTO_DISTINTO', 'REFERENCIA_DISTINTA', 'RESPUESTA_INVALIDA'))
);

COMMENT ON TABLE nucleo_financiero.discrepancia_proveedor IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. [append-only] El saldo no se guarda: se deriva, y todos los días cuadra contra el banco';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.id IS 'PK';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.referencia_tipo IS 'CK: ORDEN_RECARGA|ORDEN_RETIRO';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.referencia_id IS 'IDX, polimorfica';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.tipo IS 'CK: FIRMA_INVALIDA|ESTADO_CONTRADICTORIO|MONTO_DISTINTO|REFERENCIA_DISTINTA|RESPUESTA_INVALIDA';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.monto_esperado IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.monto_informado IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.moneda IS 'NULL';
COMMENT ON COLUMN nucleo_financiero.discrepancia_proveedor.huella IS 'UQ+referencia_id+tipo';
