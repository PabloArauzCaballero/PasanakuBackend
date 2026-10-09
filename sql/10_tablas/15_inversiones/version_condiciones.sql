-- version_condiciones · módulo 15 — Inversiones voluntarias
-- clase de dominio: VersionCondiciones
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.version_condiciones (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  producto_inversion_id              UUID NOT NULL,
  numero                             SMALLINT NOT NULL,
  plazo_dias                         INTEGER,
  base_dias                          SMALLINT,
  tasa_nominal_anual                 NUMERIC(9,6),
  permite_rescate_anticipado         BOOLEAN DEFAULT FALSE NOT NULL,
  penalizacion_anticipo              NUMERIC(7,6),
  dias_rescate                       SMALLINT,
  hora_corte                         VARCHAR(5),
  monto_minimo                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  tasa_retencion                     NUMERIC(7,6),
  tasa_comision_exito                NUMERIC(7,6),
  costos_texto                       VARCHAR(300) NOT NULL,
  texto_condiciones                  VARCHAR(2000) NOT NULL,
  texto_hash                         CHAR(64) NOT NULL,
  fuente                             VARCHAR(200) NOT NULL,
  fecha_cotizacion                   TIMESTAMPTZ NOT NULL,
  origen_datos                       VARCHAR(12) NOT NULL,
  apto_produccion                    BOOLEAN DEFAULT FALSE NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_version_condiciones PRIMARY KEY (id),
  CONSTRAINT ck_version_condiciones_plazo_dias CHECK (plazo_dias > 0),
  CONSTRAINT ck_version_condiciones_base_dias CHECK (base_dias > 0),
  CONSTRAINT ck_version_condiciones_tasa_nominal_anual CHECK (tasa_nominal_anual >= 0),
  CONSTRAINT ck_version_condiciones_penalizacion_anticipo CHECK (penalizacion_anticipo >= 0),
  CONSTRAINT ck_version_condiciones_dias_rescate CHECK (dias_rescate >= 0),
  CONSTRAINT ck_version_condiciones_monto_minimo CHECK (monto_minimo > 0),
  CONSTRAINT ck_version_condiciones_tasa_retencion CHECK (tasa_retencion >= 0),
  CONSTRAINT ck_version_condiciones_tasa_comision_exito CHECK (tasa_comision_exito >= 0),
  CONSTRAINT ck_version_condiciones_origen_datos CHECK (origen_datos IN ('SINTETICO', 'VERIFICADO'))
);

COMMENT ON TABLE inversiones.version_condiciones IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.version_condiciones.id IS 'PK';
COMMENT ON COLUMN inversiones.version_condiciones.producto_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.version_condiciones.numero IS 'UQ+producto_inversion_id';
COMMENT ON COLUMN inversiones.version_condiciones.plazo_dias IS 'NULL, CK: > 0';
COMMENT ON COLUMN inversiones.version_condiciones.base_dias IS 'NULL, CK: > 0';
COMMENT ON COLUMN inversiones.version_condiciones.tasa_nominal_anual IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.version_condiciones.penalizacion_anticipo IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.version_condiciones.dias_rescate IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.version_condiciones.hora_corte IS 'NULL';
COMMENT ON COLUMN inversiones.version_condiciones.monto_minimo IS 'CK: > 0';
COMMENT ON COLUMN inversiones.version_condiciones.tasa_retencion IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.version_condiciones.tasa_comision_exito IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.version_condiciones.origen_datos IS 'CK: SINTETICO | VERIFICADO';
