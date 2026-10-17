-- ejecucion_tarea · módulo 07 — Organizador y Automatización
-- clase de dominio: EjecucionTarea
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS organizador.ejecucion_tarea (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tarea_id                           UUID NOT NULL,
  iniciada_en                        TIMESTAMPTZ NOT NULL,
  finalizada_en                      TIMESTAMPTZ,
  resultado                          VARCHAR(10) NOT NULL,
  registros_afectados                INTEGER NOT NULL,
  detalle                            JSONB NOT NULL,
  mensaje_error                      TEXT,
  CONSTRAINT pk_ejecucion_tarea PRIMARY KEY (id),
  CONSTRAINT ck_ejecucion_tarea_resultado CHECK (resultado IN ('ERROR', 'EXITO', 'PARCIAL'))
);

COMMENT ON TABLE organizador.ejecucion_tarea IS 'Módulo 07 — Organizador y Automatización. El registro de cada ejecución: cuándo, con qué resultado, cuántos registros afectó.';
COMMENT ON COLUMN organizador.ejecucion_tarea.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN organizador.ejecucion_tarea.tarea_id IS 'FK, IDX';
COMMENT ON COLUMN organizador.ejecucion_tarea.finalizada_en IS 'NULL';
COMMENT ON COLUMN organizador.ejecucion_tarea.resultado IS 'CK';
COMMENT ON COLUMN organizador.ejecucion_tarea.mensaje_error IS 'NULL';
