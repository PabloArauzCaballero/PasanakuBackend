-- impuesto · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: Impuesto
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS catalogo.impuesto (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cuenta_contable_id                 UUID,
  codigo                             VARCHAR(15) NOT NULL,
  nombre                             VARCHAR(80) NOT NULL,
  alicuota                           NUMERIC(6,4) NOT NULL,
  tipo_calculo                       VARCHAR(20) NOT NULL,
  base_legal                         VARCHAR(120) NOT NULL,
  vigente_desde                      DATE NOT NULL,
  vigente_hasta                      DATE,
  CONSTRAINT pk_impuesto PRIMARY KEY (id),
  CONSTRAINT ck_impuesto_tipo_calculo CHECK (tipo_calculo IN ('INCLUIDO_EN_PRECIO', 'SOBRE_PRECIO'))
);

COMMENT ON TABLE catalogo.impuesto IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. Las alícuotas vigentes y su aplicación a cada devengo.';
COMMENT ON COLUMN catalogo.impuesto.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN catalogo.impuesto.cuenta_contable_id IS 'FK, NULL, M3';
COMMENT ON COLUMN catalogo.impuesto.codigo IS 'UQ+vigente_desde';
COMMENT ON COLUMN catalogo.impuesto.tipo_calculo IS 'CK';
COMMENT ON COLUMN catalogo.impuesto.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN catalogo.impuesto.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
