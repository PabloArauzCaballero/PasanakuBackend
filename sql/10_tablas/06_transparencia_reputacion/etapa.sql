-- etapa · módulo 06 — Transparencia y Reputación
-- clase de dominio: Etapa
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.etapa (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  modelo_id                          UUID NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(40) NOT NULL,
  orden                              SMALLINT NOT NULL,
  desde                              NUMERIC(6,2) NOT NULL,
  hasta                              NUMERIC(6,2) NOT NULL,
  CONSTRAINT pk_etapa PRIMARY KEY (id),
  CONSTRAINT ck_etapa_orden CHECK (orden >= 0),
  CONSTRAINT ck_etapa_desde CHECK (desde >= 0),
  CONSTRAINT ck_etapa_hasta CHECK (hasta > 0)
);

COMMENT ON TABLE transparencia.etapa IS 'Módulo 06 — Transparencia y Reputación. Los tramos con nombre en que se parte la escala del puntaje de un modelo (con el v1: En recuperación, Etapa 1 · Inicial, Etapa 2 · En marcha… hasta Etapa 10 · Pilar). Cada etapa es un intervalo [desde, hasta) del puntaje; la última incluye su hasta.';
COMMENT ON COLUMN transparencia.etapa.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.etapa.modelo_id IS 'FK, IDX';
COMMENT ON COLUMN transparencia.etapa.codigo IS 'Código estable de la etapa dentro de su modelo (INICIAL, EN_MARCHA…). Es lo que viaja entre servicios y lo que guarda un grupo como etapa mínima.';
COMMENT ON COLUMN transparencia.etapa.orden IS 'Posición de la etapa en la escala, de la más baja (0) a la más alta. Ordena el camino de etapas en la app.';
COMMENT ON COLUMN transparencia.etapa.desde IS 'Puntaje desde el que se está en esta etapa, incluido.';
COMMENT ON COLUMN transparencia.etapa.hasta IS 'Puntaje donde empieza la siguiente, excluido; en la última etapa, el máximo del modelo, incluido.';
