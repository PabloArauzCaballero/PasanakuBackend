-- catalogo_hecho_generador · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: CatalogoHechoGenerador
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.catalogo_hecho_generador (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(40) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  entidad_evento                     VARCHAR(40) NOT NULL,
  campo_monto_base                   VARCHAR(40),
  unidad_conteo                      VARCHAR(20) NOT NULL,
  modulo_origen                      CHAR(2) NOT NULL,
  activo                             BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_catalogo_hecho_generador PRIMARY KEY (id)
);

COMMENT ON TABLE tarifas.catalogo_hecho_generador IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. El catálogo de eventos del sistema sobre los que se puede cobrar.';
COMMENT ON COLUMN tarifas.catalogo_hecho_generador.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.catalogo_hecho_generador.codigo IS 'UQ';
COMMENT ON COLUMN tarifas.catalogo_hecho_generador.campo_monto_base IS 'NULL';
