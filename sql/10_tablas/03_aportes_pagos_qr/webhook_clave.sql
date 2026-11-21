-- webhook_clave · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: WebhookClave
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.webhook_clave (
  proveedor_id                       UUID DEFAULT gen_random_uuid() NOT NULL,
  clave_idempotencia                 VARCHAR(120) NOT NULL,
  webhook_id                         UUID NOT NULL,
  webhook_recibido_en                TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_webhook_clave PRIMARY KEY (proveedor_id, clave_idempotencia)
);

COMMENT ON TABLE aportes.webhook_clave IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. La clave de idempotencia que emitió la pasarela para cada aviso, junto con el proveedor que la emitió y el aviso al que corresponde.';
COMMENT ON COLUMN aportes.webhook_clave.proveedor_id IS 'PK, FK';
COMMENT ON COLUMN aportes.webhook_clave.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN aportes.webhook_clave.webhook_recibido_en IS 'IDX';
