-- criterio_emparejamiento · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: CriterioEmparejamiento
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.criterio_emparejamiento (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  peso_reputacion                    NUMERIC(4,3) NOT NULL,
  peso_monto                         NUMERIC(4,3) NOT NULL,
  peso_geografia                     NUMERIC(4,3) NOT NULL,
  peso_historial_comun               NUMERIC(4,3) NOT NULL,
  reputacion_minima                  NUMERIC(6,2) NOT NULL,
  max_morosos_por_grupo              SMALLINT NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_criterio_emparejamiento PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.criterio_emparejamiento IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. Los pesos con los que el motor decide qué tan compatibles son dos postulaciones: reputación, monto, geografía, historial común.';
COMMENT ON COLUMN grupos.criterio_emparejamiento.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.criterio_emparejamiento.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
