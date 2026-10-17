-- qr_cobro · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: QRCobro
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.qr_cobro (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  orden_cobro_id                     UUID NOT NULL,
  payload_emv                        TEXT NOT NULL,
  url_imagen                         VARCHAR(255) NOT NULL,
  crc                                VARCHAR(8) NOT NULL,
  banco_emisor                       VARCHAR(60) NOT NULL,
  cuenta_abono                       VARCHAR(40) NOT NULL,
  es_reutilizable                    BOOLEAN DEFAULT FALSE NOT NULL,
  escaneos                           SMALLINT DEFAULT 0 NOT NULL,
  CONSTRAINT pk_qr_cobro PRIMARY KEY (id)
);

COMMENT ON TABLE aportes.qr_cobro IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. El código QR interoperable propiamente dicho: el payload EMVCo, la imagen, el CRC, el banco emisor.';
COMMENT ON COLUMN aportes.qr_cobro.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.qr_cobro.orden_cobro_id IS 'FK, UQ';
