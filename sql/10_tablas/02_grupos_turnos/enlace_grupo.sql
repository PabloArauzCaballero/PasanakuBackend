-- enlace_grupo · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: EnlaceGrupo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.enlace_grupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  emisor_id                          UUID NOT NULL,
  token_hash                         VARCHAR(64) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  fecha_creacion                     TIMESTAMPTZ NOT NULL,
  fecha_expiracion                   TIMESTAMPTZ NOT NULL,
  fecha_anulacion                    TIMESTAMPTZ,
  CONSTRAINT pk_enlace_grupo PRIMARY KEY (id),
  CONSTRAINT ck_enlace_grupo_estado CHECK (estado IN ('ACTIVO', 'ANULADO'))
);

COMMENT ON TABLE grupos.enlace_grupo IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. El enlace —y su QR— que el organizador comparte para que cualquier persona pida unirse al grupo, sin tener que invitar teléfono por teléfono.';
COMMENT ON COLUMN grupos.enlace_grupo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.enlace_grupo.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN grupos.enlace_grupo.emisor_id IS 'FK';
COMMENT ON COLUMN grupos.enlace_grupo.token_hash IS 'UQ';
COMMENT ON COLUMN grupos.enlace_grupo.estado IS 'CK';
COMMENT ON COLUMN grupos.enlace_grupo.fecha_anulacion IS 'NULL';
