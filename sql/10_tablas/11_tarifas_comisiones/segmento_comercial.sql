-- segmento_comercial · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: SegmentoComercial
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.segmento_comercial (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  criterio                           JSONB NOT NULL,
  prioridad                          SMALLINT NOT NULL,
  activo                             BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_segmento_comercial PRIMARY KEY (id)
);

COMMENT ON TABLE tarifas.segmento_comercial IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. A quién se le aplica cada tarifario y con qué prioridad.';
COMMENT ON COLUMN tarifas.segmento_comercial.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.segmento_comercial.codigo IS 'UQ';
