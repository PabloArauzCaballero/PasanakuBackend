-- cesion_derecho · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.cesion_derecho (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  oferta_turno_id                    UUID NOT NULL,
  turno_id                           UUID NOT NULL,
  participante_origen_id             UUID NOT NULL,
  participante_destino_id            UUID NOT NULL,
  comprador_usuario_id               UUID NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_precio                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  estado                             VARCHAR(16) NOT NULL,
  retencion_ref                      UUID,
  liquidacion_ref                    UUID,
  motivo_fallo                       VARCHAR(200),
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  titulo_asignado_en                 TIMESTAMPTZ,
  liquidada_en                       TIMESTAMPTZ,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_cesion_derecho PRIMARY KEY (id),
  CONSTRAINT ck_cesion_derecho_monto_precio CHECK (monto_precio > 0),
  CONSTRAINT ck_cesion_derecho_estado CHECK (estado IN ('CREADA', 'VALIDADA', 'FONDOS_RETENIDOS', 'TITULO_ASIGNADO', 'LIQUIDADA', 'FALLIDA'))
);

COMMENT ON TABLE entregas.cesion_derecho IS 'Módulo 04 — Entregas de Fondo. Que la bolsa llegue completa, a la persona correcta, una sola vez';
COMMENT ON COLUMN entregas.cesion_derecho.id IS 'PK';
COMMENT ON COLUMN entregas.cesion_derecho.oferta_turno_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.cesion_derecho.turno_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.cesion_derecho.participante_origen_id IS 'FK';
COMMENT ON COLUMN entregas.cesion_derecho.participante_destino_id IS 'FK';
COMMENT ON COLUMN entregas.cesion_derecho.comprador_usuario_id IS 'IDX';
COMMENT ON COLUMN entregas.cesion_derecho.monto_precio IS 'CK: > 0';
COMMENT ON COLUMN entregas.cesion_derecho.estado IS 'CK: CREADA|VALIDADA|FONDOS_RETENIDOS|TITULO_ASIGNADO|LIQUIDADA|FALLIDA, IDX';
COMMENT ON COLUMN entregas.cesion_derecho.retencion_ref IS 'NULL';
COMMENT ON COLUMN entregas.cesion_derecho.liquidacion_ref IS 'NULL';
COMMENT ON COLUMN entregas.cesion_derecho.motivo_fallo IS 'NULL';
COMMENT ON COLUMN entregas.cesion_derecho.clave_idempotencia IS 'UQ';
COMMENT ON COLUMN entregas.cesion_derecho.titulo_asignado_en IS 'NULL';
COMMENT ON COLUMN entregas.cesion_derecho.liquidada_en IS 'NULL';
