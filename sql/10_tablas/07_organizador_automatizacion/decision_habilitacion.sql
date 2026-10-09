-- decision_habilitacion · módulo 07 — Organizador y Automatización
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS organizador.decision_habilitacion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  solicitud_id                       UUID NOT NULL,
  clave_idempotencia                 UUID NOT NULL,
  fase                               VARCHAR(15) NOT NULL,
  decision                           VARCHAR(15) NOT NULL,
  actor_id                           UUID NOT NULL,
  motivo                             VARCHAR(1000) NOT NULL,
  revision                           INTEGER NOT NULL,
  organizador_id                     UUID,
  evidencia_requisitos               TEXT NOT NULL,
  ocurrida_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  correlacion_id                     UUID NOT NULL,
  CONSTRAINT pk_decision_habilitacion PRIMARY KEY (id)
);

COMMENT ON TABLE organizador.decision_habilitacion IS 'Módulo 07 — Organizador y Automatización. [append-only] Administrar es un rol, no un negocio: el organizador no cobra ni custodia';
COMMENT ON COLUMN organizador.decision_habilitacion.id IS 'PK';
COMMENT ON COLUMN organizador.decision_habilitacion.solicitud_id IS 'IDX';
COMMENT ON COLUMN organizador.decision_habilitacion.clave_idempotencia IS 'UQ';
COMMENT ON COLUMN organizador.decision_habilitacion.revision IS 'UQ+solicitud_id';
COMMENT ON COLUMN organizador.decision_habilitacion.organizador_id IS 'NULL';
