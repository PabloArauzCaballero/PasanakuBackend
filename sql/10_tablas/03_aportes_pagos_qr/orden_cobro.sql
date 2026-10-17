-- orden_cobro · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: OrdenCobro
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.orden_cobro (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  obligacion_id                      UUID NOT NULL,
  proveedor_id                       UUID NOT NULL,
  monto_exacto                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  permite_monto_abierto              BOOLEAN DEFAULT FALSE NOT NULL,
  referencia_unica                   VARCHAR(60) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  emitida_en                         TIMESTAMPTZ NOT NULL,
  expira_en                          TIMESTAMPTZ NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  CONSTRAINT pk_orden_cobro PRIMARY KEY (id),
  CONSTRAINT ck_orden_cobro_estado CHECK (estado IN ('ANULADA', 'ENTREGADA', 'ESCANEADA', 'EXPIRADA', 'GENERADA', 'PAGADA'))
);

COMMENT ON TABLE aportes.orden_cobro IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. La instrucción concreta de cobro para una obligación: monto exacto, proveedor, referencia única, vigencia.';
COMMENT ON COLUMN aportes.orden_cobro.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.orden_cobro.obligacion_id IS 'FK, IDX';
COMMENT ON COLUMN aportes.orden_cobro.proveedor_id IS 'FK';
COMMENT ON COLUMN aportes.orden_cobro.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN aportes.orden_cobro.referencia_unica IS 'UQ';
COMMENT ON COLUMN aportes.orden_cobro.estado IS 'CK, IDX';
COMMENT ON COLUMN aportes.orden_cobro.expira_en IS 'IDX';
COMMENT ON COLUMN aportes.orden_cobro.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
