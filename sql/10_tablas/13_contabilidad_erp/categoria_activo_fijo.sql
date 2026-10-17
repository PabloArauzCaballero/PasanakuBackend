-- categoria_activo_fijo · módulo 13 — Contabilidad Financiera y ERP
-- clase de dominio: CategoriaActivoFijo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS erp.categoria_activo_fijo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(20) NOT NULL,
  nombre                             VARCHAR(80) NOT NULL,
  vida_util_meses                    SMALLINT NOT NULL,
  metodo_depreciacion                VARCHAR(20) NOT NULL,
  cuenta_activo_id                   UUID NOT NULL,
  cuenta_depreciacion_id             UUID NOT NULL,
  cuenta_gasto_depreciacion_id       UUID NOT NULL,
  CONSTRAINT pk_categoria_activo_fijo PRIMARY KEY (id),
  CONSTRAINT ck_categoria_activo_fijo_vida_util_meses CHECK (vida_util_meses > 0),
  CONSTRAINT ck_categoria_activo_fijo_metodo_depreciacion CHECK (metodo_depreciacion IN ('LINEA_RECTA'))
);

COMMENT ON TABLE erp.categoria_activo_fijo IS 'Módulo 13 — Contabilidad Financiera y ERP. Catálogo: tipo de bien, vida útil, método de depreciación y las tres cuentas contables que le corresponden.';
COMMENT ON COLUMN erp.categoria_activo_fijo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN erp.categoria_activo_fijo.codigo IS 'UQ';
COMMENT ON COLUMN erp.categoria_activo_fijo.vida_util_meses IS 'CK: > 0';
COMMENT ON COLUMN erp.categoria_activo_fijo.metodo_depreciacion IS 'CK';
COMMENT ON COLUMN erp.categoria_activo_fijo.cuenta_activo_id IS 'FK, M3';
COMMENT ON COLUMN erp.categoria_activo_fijo.cuenta_depreciacion_id IS 'FK, M3';
COMMENT ON COLUMN erp.categoria_activo_fijo.cuenta_gasto_depreciacion_id IS 'FK, M3';
