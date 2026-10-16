-- comision_exito · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.comision_exito (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  posicion_inversion_id              UUID NOT NULL,
  rescate_inversion_id               UUID NOT NULL,
  cuotas                             NUMERIC(18,6) NOT NULL,
  valor_cuota                        NUMERIC(18,6) NOT NULL,
  marca_maxima_previa                NUMERIC(18,6) NOT NULL,
  tasa                               NUMERIC(7,6) NOT NULL,
  base_elegible                      NUMERIC(14,2) NOT NULL,
  comision                           NUMERIC(14,2) NOT NULL,
  cuota_neta                         NUMERIC(18,6) NOT NULL,
  marca_maxima_nueva                 NUMERIC(18,6) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  origen_datos                       VARCHAR(12) NOT NULL,
  calculada_en                       TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_comision_exito PRIMARY KEY (id),
  CONSTRAINT ck_comision_exito_cuotas CHECK (cuotas > 0),
  CONSTRAINT ck_comision_exito_valor_cuota CHECK (valor_cuota > 0),
  CONSTRAINT ck_comision_exito_marca_maxima_previa CHECK (marca_maxima_previa > 0),
  CONSTRAINT ck_comision_exito_tasa CHECK (tasa >= 0),
  CONSTRAINT ck_comision_exito_base_elegible CHECK (base_elegible >= 0),
  CONSTRAINT ck_comision_exito_comision CHECK (comision >= 0),
  CONSTRAINT ck_comision_exito_cuota_neta CHECK (cuota_neta > 0),
  CONSTRAINT ck_comision_exito_marca_maxima_nueva CHECK (marca_maxima_nueva > 0),
  CONSTRAINT ck_comision_exito_origen_datos CHECK (origen_datos IN ('SINTETICO', 'VERIFICADO'))
);

COMMENT ON TABLE inversiones.comision_exito IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.comision_exito.id IS 'PK';
COMMENT ON COLUMN inversiones.comision_exito.posicion_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.comision_exito.rescate_inversion_id IS 'FK, UQ';
COMMENT ON COLUMN inversiones.comision_exito.cuotas IS 'CK: > 0';
COMMENT ON COLUMN inversiones.comision_exito.valor_cuota IS 'CK: > 0';
COMMENT ON COLUMN inversiones.comision_exito.marca_maxima_previa IS 'CK: > 0';
COMMENT ON COLUMN inversiones.comision_exito.tasa IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comision_exito.base_elegible IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comision_exito.comision IS 'CK: >= 0';
COMMENT ON COLUMN inversiones.comision_exito.cuota_neta IS 'CK: > 0';
COMMENT ON COLUMN inversiones.comision_exito.marca_maxima_nueva IS 'CK: > 0';
COMMENT ON COLUMN inversiones.comision_exito.origen_datos IS 'CK: SINTETICO | VERIFICADO';
