-- linea_plantilla_asiento · módulo 13 — Contabilidad Financiera y ERP
-- clase de dominio: LineaPlantillaAsiento
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS erp.linea_plantilla_asiento (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  plantilla_id                       UUID NOT NULL,
  cuenta_contable_id                 UUID NOT NULL,
  tipo_movimiento                    VARCHAR(5) NOT NULL,
  monto_referencial                  NUMERIC(14,2),
  orden                              SMALLINT NOT NULL,
  CONSTRAINT pk_linea_plantilla_asiento PRIMARY KEY (id),
  CONSTRAINT ck_linea_plantilla_asiento_tipo_movimiento CHECK (tipo_movimiento IN ('DEBE', 'HABER'))
);

COMMENT ON TABLE erp.linea_plantilla_asiento IS 'Módulo 13 — Contabilidad Financiera y ERP. Una plantilla de asiento recurrente (alquiler, planilla mensual) para no recapturar el mismo asiento a mano cada mes.';
COMMENT ON COLUMN erp.linea_plantilla_asiento.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN erp.linea_plantilla_asiento.plantilla_id IS 'FK, IDX';
COMMENT ON COLUMN erp.linea_plantilla_asiento.cuenta_contable_id IS 'FK, IDX, M3';
COMMENT ON COLUMN erp.linea_plantilla_asiento.tipo_movimiento IS 'CK';
COMMENT ON COLUMN erp.linea_plantilla_asiento.monto_referencial IS 'NULL';
