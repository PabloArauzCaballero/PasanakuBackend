-- snapshot_sorteo · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.snapshot_sorteo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  sorteo_id                          UUID NOT NULL,
  grupo_id                           UUID NOT NULL,
  roster                             TEXT NOT NULL,
  periodos                           TEXT NOT NULL,
  reglas                             TEXT NOT NULL,
  hash_snapshot                      VARCHAR(64) NOT NULL,
  semilla_sellada                    VARCHAR(128) NOT NULL,
  congelado_en                       TIMESTAMPTZ NOT NULL,
  correlacion_id                     UUID NOT NULL,
  CONSTRAINT pk_snapshot_sorteo PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.snapshot_sorteo IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. [append-only] Reglas del juego, orden de cobro y decisiones colectivas';
COMMENT ON COLUMN grupos.snapshot_sorteo.id IS 'PK';
COMMENT ON COLUMN grupos.snapshot_sorteo.sorteo_id IS 'UQ';
COMMENT ON COLUMN grupos.snapshot_sorteo.grupo_id IS 'IDX';
