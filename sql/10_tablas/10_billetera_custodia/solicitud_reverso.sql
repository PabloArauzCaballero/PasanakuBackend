-- solicitud_reverso · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: SolicitudReverso
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.solicitud_reverso (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  transaccion_original_id            UUID NOT NULL,
  solicitada_por                     UUID NOT NULL,
  autorizada_por                     UUID,
  reverso_transaccion_id             UUID,
  clave_idempotencia                 VARCHAR(100) NOT NULL,
  tipo                               VARCHAR(25) NOT NULL,
  motivo                             VARCHAR(300) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  solicitada_en                      TIMESTAMPTZ DEFAULT now() NOT NULL,
  resuelta_en                        TIMESTAMPTZ,
  CONSTRAINT pk_solicitud_reverso PRIMARY KEY (id),
  CONSTRAINT ck_solicitud_reverso_tipo CHECK (tipo IN ('ANULACION', 'CONTRACARGO', 'ERROR_OPERATIVO', 'ORDEN_AUTORIDAD')),
  CONSTRAINT ck_solicitud_reverso_estado CHECK (estado IN ('EJECUTADA', 'SOLICITADA'))
);

COMMENT ON TABLE nucleo_financiero.solicitud_reverso IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. El primer acto de un reverso: quién lo pidió, sobre qué transacción, de qué tipo y por qué. No mueve saldo.';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.transaccion_original_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.solicitada_por IS 'FK';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.autorizada_por IS 'FK, NULL';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.reverso_transaccion_id IS 'FK, NULL, UQ';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.tipo IS 'CK';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.estado IS 'CK';
COMMENT ON COLUMN nucleo_financiero.solicitud_reverso.resuelta_en IS 'NULL';
