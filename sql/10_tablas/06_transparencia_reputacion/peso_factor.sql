-- peso_factor · módulo 06 — Transparencia y Reputación
-- clase de dominio: PesoFactor
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.peso_factor (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  modelo_id                          UUID NOT NULL,
  codigo_factor                      VARCHAR(40) NOT NULL,
  descripcion                        VARCHAR(160) NOT NULL,
  peso                               NUMERIC(5,4) NOT NULL,
  tope_aporte_al_score               NUMERIC(6,2) NOT NULL,
  es_penalizador                     BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_peso_factor PRIMARY KEY (id)
);

COMMENT ON TABLE transparencia.peso_factor IS 'Módulo 06 — Transparencia y Reputación. Cuánto pesa cada dimensión dentro del modelo: puntualidad, permanencia, severidad de incumplimientos, volumen, antigüedad, comportamiento social.';
COMMENT ON COLUMN transparencia.peso_factor.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.peso_factor.modelo_id IS 'FK, IDX';
COMMENT ON COLUMN transparencia.peso_factor.codigo_factor IS 'UQ+modelo_id';
