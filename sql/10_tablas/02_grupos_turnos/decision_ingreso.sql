-- decision_ingreso · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.decision_ingreso (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  solicitud_id                       UUID NOT NULL,
  clave_idempotencia                 UUID NOT NULL,
  fase                               VARCHAR(15) NOT NULL,
  decision                           VARCHAR(15) NOT NULL,
  actor_id                           UUID NOT NULL,
  motivo                             VARCHAR(1000) NOT NULL,
  propuesta_id                       UUID,
  participante_id                    UUID,
  revision                           INTEGER NOT NULL,
  evidencia_algoritmo                TEXT NOT NULL,
  version_motor                      VARCHAR(60) NOT NULL,
  recomendacion_algoritmo            VARCHAR(20) NOT NULL,
  apartamiento                       BOOLEAN DEFAULT FALSE NOT NULL,
  ocurrida_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  correlacion_id                     UUID NOT NULL,
  CONSTRAINT pk_decision_ingreso PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.decision_ingreso IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. [append-only] Cada decisión humana sobre una solicitud de ingreso a un grupo, en dos fases: la propuesta y la resolución.';
COMMENT ON COLUMN grupos.decision_ingreso.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.decision_ingreso.solicitud_id IS 'IDX';
COMMENT ON COLUMN grupos.decision_ingreso.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
COMMENT ON COLUMN grupos.decision_ingreso.propuesta_id IS 'NULL';
COMMENT ON COLUMN grupos.decision_ingreso.participante_id IS 'NULL';
COMMENT ON COLUMN grupos.decision_ingreso.revision IS 'UQ+solicitud_id';
