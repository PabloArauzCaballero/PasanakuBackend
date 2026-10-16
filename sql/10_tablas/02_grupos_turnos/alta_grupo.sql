-- alta_grupo · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.alta_grupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  creador_id                         UUID NOT NULL,
  clave_idempotencia                 UUID NOT NULL,
  huella_solicitud                   VARCHAR(64) NOT NULL,
  fondo_por_periodo                  VARCHAR(60) NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_alta_grupo PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.alta_grupo IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. [append-only] Reglas del juego, orden de cobro y decisiones colectivas';
COMMENT ON COLUMN grupos.alta_grupo.id IS 'PK';
COMMENT ON COLUMN grupos.alta_grupo.grupo_id IS 'UQ';
COMMENT ON COLUMN grupos.alta_grupo.creador_id IS 'IDX';
COMMENT ON COLUMN grupos.alta_grupo.clave_idempotencia IS 'UQ';
