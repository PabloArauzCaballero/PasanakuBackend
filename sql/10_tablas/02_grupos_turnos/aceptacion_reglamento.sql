-- aceptacion_reglamento · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: AceptacionReglamento
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.aceptacion_reglamento (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  reglamento_id                      UUID NOT NULL,
  participante_id                    UUID NOT NULL,
  aceptado_en                        TIMESTAMPTZ NOT NULL,
  hash_firmado                       VARCHAR(64) NOT NULL,
  ip_origen                          INET NOT NULL,
  token_firma_id                     UUID,
  CONSTRAINT pk_aceptacion_reglamento PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.aceptacion_reglamento IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. La firma de un participante sobre una versión concreta del reglamento.';
COMMENT ON COLUMN grupos.aceptacion_reglamento.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.aceptacion_reglamento.reglamento_id IS 'FK';
COMMENT ON COLUMN grupos.aceptacion_reglamento.participante_id IS 'FK';
COMMENT ON COLUMN grupos.aceptacion_reglamento.token_firma_id IS 'FK, NULL, M1';
