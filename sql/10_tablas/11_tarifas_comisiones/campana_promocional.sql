-- campana_promocional · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: CampanaPromocional
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.campana_promocional (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(120) NOT NULL,
  tipo                               VARCHAR(30) NOT NULL,
  presupuesto_maximo                 NUMERIC(16,2),
  presupuesto_consumido              NUMERIC(16,2) DEFAULT 0 NOT NULL,
  condiciones                        JSONB NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  vigente_hasta                      TIMESTAMPTZ NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  aprobada_por                       UUID NOT NULL,
  CONSTRAINT pk_campana_promocional PRIMARY KEY (id),
  CONSTRAINT ck_campana_promocional_tipo CHECK (tipo IN ('DESCUENTO_PORCENTUAL', 'PRIMER_CICLO_GRATIS', 'REFERIDOS', 'TOPE_REBAJADO')),
  CONSTRAINT ck_campana_promocional_estado CHECK (estado IN ('ACTIVA', 'AGOTADA', 'BORRADOR', 'FINALIZADA', 'PAUSADA'))
);

COMMENT ON TABLE tarifas.campana_promocional IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. No cobrar, con nombre y apellido.';
COMMENT ON COLUMN tarifas.campana_promocional.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.campana_promocional.codigo IS 'UQ';
COMMENT ON COLUMN tarifas.campana_promocional.tipo IS 'CK';
COMMENT ON COLUMN tarifas.campana_promocional.presupuesto_maximo IS 'NULL';
COMMENT ON COLUMN tarifas.campana_promocional.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN tarifas.campana_promocional.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
COMMENT ON COLUMN tarifas.campana_promocional.estado IS 'CK, IDX';
COMMENT ON COLUMN tarifas.campana_promocional.aprobada_por IS 'FK';
