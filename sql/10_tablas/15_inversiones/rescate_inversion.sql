-- rescate_inversion · módulo 15 — Inversiones voluntarias
-- clase de dominio: RescateInversion
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.rescate_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  usuario_id                         UUID NOT NULL,
  tipo                               VARCHAR(12) NOT NULL,
  cuotas                             NUMERIC(18,6),
  estado                             VARCHAR(14) NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  hash_solicitud                     CHAR(64) NOT NULL,
  fecha_valor                        DATE,
  liquida_en                         TIMESTAMPTZ,
  transaccion_externa                VARCHAR(80),
  motivo_rechazo                     VARCHAR(60),
  version                            INTEGER DEFAULT 0 NOT NULL,
  solicitada_en                      TIMESTAMPTZ DEFAULT now() NOT NULL,
  actualizada_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_rescate_inversion PRIMARY KEY (id),
  CONSTRAINT ck_rescate_inversion_tipo CHECK (tipo IN ('PARCIAL', 'TOTAL', 'VENCIMIENTO', 'ANTICIPADO')),
  CONSTRAINT ck_rescate_inversion_cuotas CHECK (cuotas > 0),
  CONSTRAINT ck_rescate_inversion_estado CHECK (estado IN ('SOLICITADO', 'PENDIENTE', 'INCIERTO', 'POR_ACREDITAR', 'LIQUIDADO', 'RECHAZADO'))
);

COMMENT ON TABLE inversiones.rescate_inversion IS 'Módulo 15 — Inversiones voluntarias. El pedido de sacar dinero de una posición: parcial, total, al vencimiento o anticipado, con sus estados hasta liquidarse.';
COMMENT ON COLUMN inversiones.rescate_inversion.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN inversiones.rescate_inversion.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.rescate_inversion.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.rescate_inversion.tipo IS 'CK: PARCIAL | TOTAL | VENCIMIENTO | ANTICIPADO';
COMMENT ON COLUMN inversiones.rescate_inversion.cuotas IS 'NULL, CK: > 0';
COMMENT ON COLUMN inversiones.rescate_inversion.estado IS 'CK: SOLICITADO | PENDIENTE | INCIERTO | POR_ACREDITAR | LIQUIDADO | RECHAZADO, IDX';
COMMENT ON COLUMN inversiones.rescate_inversion.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN inversiones.rescate_inversion.fecha_valor IS 'NULL';
COMMENT ON COLUMN inversiones.rescate_inversion.liquida_en IS 'NULL';
COMMENT ON COLUMN inversiones.rescate_inversion.transaccion_externa IS 'NULL';
COMMENT ON COLUMN inversiones.rescate_inversion.motivo_rechazo IS 'NULL';
