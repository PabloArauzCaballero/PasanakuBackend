-- parametro_financiero · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: ParametroFinanciero
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.parametro_financiero (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(50) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  unidad                             VARCHAR(12),
  por_nivel                          BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_parametro_financiero PRIMARY KEY (id),
  CONSTRAINT ck_parametro_financiero_unidad CHECK (unidad IN ('CONTEO', 'DIAS', 'MONTO', 'PORCENTAJE'))
);

COMMENT ON TABLE tarifas.parametro_financiero IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. El catálogo cerrado de los parámetros financieros del modelo A + D: su código, su unidad y si llevan un valor por nivel de puntualidad. No guarda valores.';
COMMENT ON COLUMN tarifas.parametro_financiero.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.parametro_financiero.codigo IS 'UQ';
COMMENT ON COLUMN tarifas.parametro_financiero.unidad IS 'CK, NULL';
