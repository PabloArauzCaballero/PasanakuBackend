-- modelo_scoring · módulo 06 — Transparencia y Reputación
-- clase de dominio: ModeloScoring
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.modelo_scoring (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  version                            VARCHAR(20) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  puntaje_base                       NUMERIC(6,2) NOT NULL,
  puntaje_minimo                     NUMERIC(6,2) NOT NULL,
  puntaje_maximo                     NUMERIC(6,2) NOT NULL,
  factor_decaimiento_mensual         NUMERIC(5,4) NOT NULL,
  ventana_historica_meses            SMALLINT NOT NULL,
  min_eventos_para_score             SMALLINT NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  vigente_hasta                      TIMESTAMPTZ,
  es_produccion                      BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_modelo_scoring PRIMARY KEY (id)
);

COMMENT ON TABLE transparencia.modelo_scoring IS 'Módulo 06 — Transparencia y Reputación. La versión del algoritmo que convierte eventos en un puntaje: puntaje base, mínimo, máximo, decaimiento y ventana histórica.';
COMMENT ON COLUMN transparencia.modelo_scoring.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.modelo_scoring.version IS 'UQ';
COMMENT ON COLUMN transparencia.modelo_scoring.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN transparencia.modelo_scoring.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
