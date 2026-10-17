-- historial_estado_grupo · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: HistorialEstadoGrupo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.historial_estado_grupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  estado_anterior                    VARCHAR(30) NOT NULL,
  estado_nuevo                       VARCHAR(30) NOT NULL,
  motivo                             VARCHAR(200) NOT NULL,
  ejecutado_por                      UUID NOT NULL,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_grupo PRIMARY KEY (id)
);

COMMENT ON TABLE grupos.historial_estado_grupo IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. Cada transición de estado del grupo, con motivo y autor.';
COMMENT ON COLUMN grupos.historial_estado_grupo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.historial_estado_grupo.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN grupos.historial_estado_grupo.ejecutado_por IS 'FK';
