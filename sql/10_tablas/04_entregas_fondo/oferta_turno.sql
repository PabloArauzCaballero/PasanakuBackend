-- oferta_turno · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.oferta_turno (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  turno_id                           UUID NOT NULL,
  cupo_id                            UUID NOT NULL,
  participante_origen_id             UUID NOT NULL,
  vendedor_usuario_id                UUID NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_derecho                      NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_precio                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_cargos                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  vigente_hasta                      TIMESTAMPTZ NOT NULL,
  publicada_en                       TIMESTAMPTZ NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_oferta_turno PRIMARY KEY (id),
  CONSTRAINT ck_oferta_turno_monto_derecho CHECK (monto_derecho > 0),
  CONSTRAINT ck_oferta_turno_monto_precio CHECK (monto_precio > 0),
  CONSTRAINT ck_oferta_turno_monto_cargos CHECK (monto_cargos >= 0),
  CONSTRAINT ck_oferta_turno_estado CHECK (estado IN ('PUBLICADA', 'RESERVADA', 'LIQUIDANDO', 'VENDIDA', 'CANCELADA', 'VENCIDA'))
);

COMMENT ON TABLE entregas.oferta_turno IS 'Módulo 04 — Entregas de Fondo. Que la bolsa llegue completa, a la persona correcta, una sola vez';
COMMENT ON COLUMN entregas.oferta_turno.id IS 'PK';
COMMENT ON COLUMN entregas.oferta_turno.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.oferta_turno.turno_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.oferta_turno.cupo_id IS 'FK';
COMMENT ON COLUMN entregas.oferta_turno.participante_origen_id IS 'FK';
COMMENT ON COLUMN entregas.oferta_turno.vendedor_usuario_id IS 'IDX';
COMMENT ON COLUMN entregas.oferta_turno.monto_derecho IS 'CK: > 0';
COMMENT ON COLUMN entregas.oferta_turno.monto_precio IS 'CK: > 0';
COMMENT ON COLUMN entregas.oferta_turno.monto_cargos IS 'CK: >= 0';
COMMENT ON COLUMN entregas.oferta_turno.estado IS 'CK: PUBLICADA|RESERVADA|LIQUIDANDO|VENDIDA|CANCELADA|VENCIDA, IDX';
COMMENT ON COLUMN entregas.oferta_turno.vigente_hasta IS 'IDX';
COMMENT ON COLUMN entregas.oferta_turno.clave_idempotencia IS 'UQ';
