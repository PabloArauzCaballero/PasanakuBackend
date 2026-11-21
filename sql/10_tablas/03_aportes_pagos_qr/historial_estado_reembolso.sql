-- historial_estado_reembolso · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: HistorialEstadoReembolso
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.historial_estado_reembolso (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  reembolso_id                       UUID NOT NULL,
  estado_anterior                    VARCHAR(15) NOT NULL,
  estado_nuevo                       VARCHAR(15) NOT NULL,
  ejecutado_por                      UUID NOT NULL,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_reembolso PRIMARY KEY (id)
);

COMMENT ON TABLE aportes.historial_estado_reembolso IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Cada cambio de estado de un reembolso: de qué estado a cuál, quién lo ejecutó y cuándo.';
COMMENT ON COLUMN aportes.historial_estado_reembolso.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.historial_estado_reembolso.reembolso_id IS 'FK, IDX';
COMMENT ON COLUMN aportes.historial_estado_reembolso.ejecutado_por IS 'FK';
