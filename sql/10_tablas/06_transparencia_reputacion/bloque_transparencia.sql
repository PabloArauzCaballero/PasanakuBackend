-- bloque_transparencia · módulo 06 — Transparencia y Reputación
-- clase de dominio: BloqueTransparencia
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.bloque_transparencia (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  numero_bloque                      BIGINT NOT NULL,
  hash_bloque_anterior               VARCHAR(64) NOT NULL,
  raiz_merkle                        VARCHAR(64) NOT NULL,
  hash_bloque                        VARCHAR(64) NOT NULL,
  cantidad_eventos                   INTEGER DEFAULT 0 NOT NULL,
  periodo_cubierto_desde             TIMESTAMPTZ NOT NULL,
  periodo_cubierto_hasta             TIMESTAMPTZ NOT NULL,
  sellado_en                         TIMESTAMPTZ NOT NULL,
  sello_externo                      VARCHAR(255),
  CONSTRAINT pk_bloque_transparencia PRIMARY KEY (id)
);

COMMENT ON TABLE transparencia.bloque_transparencia IS 'Módulo 06 — Transparencia y Reputación. Un bloque sellado de la historia del grupo, encadenado por hash al anterior y con raíz Merkle de los eventos que contiene.';
COMMENT ON COLUMN transparencia.bloque_transparencia.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.bloque_transparencia.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN transparencia.bloque_transparencia.hash_bloque IS 'UQ';
COMMENT ON COLUMN transparencia.bloque_transparencia.sello_externo IS 'NULL';
