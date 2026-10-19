-- historial_estado_entrega · módulo 04 — Entregas de Fondo
-- clase de dominio: HistorialEstadoEntrega
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.historial_estado_entrega (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  entrega_id                         UUID NOT NULL,
  estado_anterior                    VARCHAR(35) NOT NULL,
  estado_nuevo                       VARCHAR(35) NOT NULL,
  motivo                             VARCHAR(200) NOT NULL,
  ejecutado_por                      UUID,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_entrega PRIMARY KEY (id)
);

COMMENT ON TABLE entregas.historial_estado_entrega IS 'Módulo 04 — Entregas de Fondo. Cada transición de la entrega, con motivo y autor.';
COMMENT ON COLUMN entregas.historial_estado_entrega.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN entregas.historial_estado_entrega.entrega_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.historial_estado_entrega.ejecutado_por IS 'FK, NULL';
