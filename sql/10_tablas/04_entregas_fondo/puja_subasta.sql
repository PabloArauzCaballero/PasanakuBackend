-- puja_subasta · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.puja_subasta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  subasta_cupo_id                    UUID NOT NULL,
  postor_usuario_id                  UUID NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  retencion_ref                      UUID,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_puja_subasta PRIMARY KEY (id),
  CONSTRAINT ck_puja_subasta_monto CHECK (monto > 0),
  CONSTRAINT ck_puja_subasta_estado CHECK (estado IN ('PENDIENTE', 'VIGENTE', 'SUPERADA', 'LIBERADA', 'GANADORA', 'RECHAZADA'))
);

COMMENT ON TABLE entregas.puja_subasta IS 'Módulo 04 — Entregas de Fondo. Una oferta por un cupo en subasta: quién ofrece, cuánto y en qué estado está (VIGENTE, SUPERADA, LIBERADA, GANADORA, RECHAZADA).';
COMMENT ON COLUMN entregas.puja_subasta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN entregas.puja_subasta.subasta_cupo_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.puja_subasta.postor_usuario_id IS 'IDX';
COMMENT ON COLUMN entregas.puja_subasta.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN entregas.puja_subasta.monto IS 'CK: > 0';
COMMENT ON COLUMN entregas.puja_subasta.estado IS 'CK: PENDIENTE|VIGENTE|SUPERADA|LIBERADA|GANADORA|RECHAZADA, IDX';
COMMENT ON COLUMN entregas.puja_subasta.retencion_ref IS 'NULL';
COMMENT ON COLUMN entregas.puja_subasta.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
