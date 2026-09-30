-- evidencia_mfa_consumida · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: EvidenciaMfaConsumida
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.evidencia_mfa_consumida (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  jti                                UUID NOT NULL,
  usuario_id                         UUID NOT NULL,
  proposito                          VARCHAR(20) NOT NULL,
  consumida_en                       TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_evidencia_mfa_consumida PRIMARY KEY (id),
  CONSTRAINT ck_evidencia_mfa_consumida_proposito CHECK (proposito IN ('ADMIN', 'CAMBIO_CUENTA', 'RETIRO'))
);

COMMENT ON TABLE nucleo_financiero.evidencia_mfa_consumida IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. [append-only] El saldo no se guarda: se deriva, y todos los días cuadra contra el banco';
COMMENT ON COLUMN nucleo_financiero.evidencia_mfa_consumida.id IS 'PK';
COMMENT ON COLUMN nucleo_financiero.evidencia_mfa_consumida.jti IS 'UQ, consumo de un solo uso';
COMMENT ON COLUMN nucleo_financiero.evidencia_mfa_consumida.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.evidencia_mfa_consumida.proposito IS 'CK';
