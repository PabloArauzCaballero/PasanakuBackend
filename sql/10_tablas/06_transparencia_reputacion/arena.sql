-- arena · módulo 06 — Transparencia y Reputación
-- clase de dominio: Arena
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.arena (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  modelo_id                          UUID NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(40) NOT NULL,
  orden                              SMALLINT NOT NULL,
  desde                              NUMERIC(6,2) NOT NULL,
  hasta                              NUMERIC(6,2) NOT NULL,
  CONSTRAINT pk_arena PRIMARY KEY (id),
  CONSTRAINT ck_arena_orden CHECK (orden >= 0),
  CONSTRAINT ck_arena_desde CHECK (desde >= 0),
  CONSTRAINT ck_arena_hasta CHECK (hasta > 0)
);

COMMENT ON TABLE transparencia.arena IS 'Módulo 06 — Transparencia y Reputación. Los tramos con nombre en que se parte la escala del puntaje de un modelo (con el v1: Campamento, Semilla, Brote… hasta Inti). Cada arena es un intervalo [desde, hasta) del puntaje; la última incluye su hasta.';
COMMENT ON COLUMN transparencia.arena.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.arena.modelo_id IS 'FK, IDX';
COMMENT ON COLUMN transparencia.arena.codigo IS 'Código estable de la arena dentro de su modelo (SEMILLA, BROTE…). Es lo que viaja entre servicios y lo que guarda un grupo como arena mínima.';
COMMENT ON COLUMN transparencia.arena.orden IS 'Posición de la arena en la escala, de la más baja (0) a la más alta. Ordena el camino de arenas en la app.';
COMMENT ON COLUMN transparencia.arena.desde IS 'Puntaje desde el que se está en esta arena, incluido.';
COMMENT ON COLUMN transparencia.arena.hasta IS 'Puntaje donde empieza la siguiente, excluido; en la última arena, el máximo del modelo, incluido.';
