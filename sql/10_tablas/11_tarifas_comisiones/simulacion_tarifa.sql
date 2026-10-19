-- simulacion_tarifa · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: SimulacionTarifa
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.simulacion_tarifa (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tarifario_id                       UUID NOT NULL,
  ejecutada_por                      UUID NOT NULL,
  escenario                          JSONB NOT NULL,
  resultado                          JSONB NOT NULL,
  delta_ingreso_estimado             NUMERIC(16,2) NOT NULL,
  usuarios_impactados                INTEGER NOT NULL,
  ejecutada_en                       TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_simulacion_tarifa PRIMARY KEY (id)
);

COMMENT ON TABLE tarifas.simulacion_tarifa IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. El ensayo previo y el expediente del cambio.';
COMMENT ON COLUMN tarifas.simulacion_tarifa.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.simulacion_tarifa.tarifario_id IS 'FK, IDX';
COMMENT ON COLUMN tarifas.simulacion_tarifa.ejecutada_por IS 'FK';
