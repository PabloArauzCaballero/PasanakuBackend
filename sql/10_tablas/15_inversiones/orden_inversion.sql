-- orden_inversion · módulo 15 — Inversiones voluntarias
-- clase de dominio: OrdenInversion
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.orden_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  producto_inversion_id              UUID NOT NULL,
  version_condiciones_id             UUID NOT NULL,
  consentimiento_inversion_id        UUID NOT NULL,
  cuenta_billetera_id                UUID NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  hash_solicitud                     CHAR(64) NOT NULL,
  transaccion_externa                VARCHAR(80),
  fecha_valor                        DATE,
  motivo_rechazo                     VARCHAR(60),
  version                            INTEGER DEFAULT 0 NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  actualizada_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_orden_inversion PRIMARY KEY (id),
  CONSTRAINT ck_orden_inversion_monto CHECK (monto > 0),
  CONSTRAINT ck_orden_inversion_estado CHECK (estado IN ('CREADA', 'RETENIDA', 'ENVIADA', 'INCIERTA', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA'))
);

COMMENT ON TABLE inversiones.orden_inversion IS 'Módulo 15 — Inversiones voluntarias. Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.orden_inversion.id IS 'PK';
COMMENT ON COLUMN inversiones.orden_inversion.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.orden_inversion.producto_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.orden_inversion.version_condiciones_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.orden_inversion.consentimiento_inversion_id IS 'FK, UQ';
COMMENT ON COLUMN inversiones.orden_inversion.monto IS 'CK: > 0';
COMMENT ON COLUMN inversiones.orden_inversion.estado IS 'CK: CREADA | RETENIDA | ENVIADA | INCIERTA | CONFIRMADA | RECHAZADA | CANCELADA, IDX';
COMMENT ON COLUMN inversiones.orden_inversion.clave_idempotencia IS 'UQ+usuario_id';
COMMENT ON COLUMN inversiones.orden_inversion.transaccion_externa IS 'NULL';
COMMENT ON COLUMN inversiones.orden_inversion.fecha_valor IS 'NULL';
COMMENT ON COLUMN inversiones.orden_inversion.motivo_rechazo IS 'NULL';
