-- sustitucion_administrador · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.sustitucion_administrador (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  saliente_participante_id           UUID NOT NULL,
  entrante_participante_id           UUID NOT NULL,
  clave_idempotencia                 UUID NOT NULL,
  motivo                             VARCHAR(1000) NOT NULL,
  actor_id                           UUID NOT NULL,
  obligaciones_conservadas           TEXT NOT NULL,
  ocurrida_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  correlacion_id                     UUID NOT NULL,
  CONSTRAINT pk_sustitucion_administrador PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.sustitucion_administrador IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. [append-only] Reglas del juego, orden de cobro y decisiones colectivas';
COMMENT ON COLUMN grupos.sustitucion_administrador.id IS 'PK';
COMMENT ON COLUMN grupos.sustitucion_administrador.grupo_id IS 'IDX';
COMMENT ON COLUMN grupos.sustitucion_administrador.clave_idempotencia IS 'UQ';
