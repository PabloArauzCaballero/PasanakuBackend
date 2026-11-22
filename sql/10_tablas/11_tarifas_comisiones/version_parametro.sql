-- version_parametro · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: VersionParametro
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.version_parametro (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  parametro_id                       UUID NOT NULL,
  nivel_codigo                       VARCHAR(30),
  valor                              NUMERIC(16,6) NOT NULL,
  moneda                             CHAR(3),
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  procedencia_fuente                 VARCHAR(200) NOT NULL,
  procedencia_referencia             VARCHAR(300) NOT NULL,
  procedencia_fecha                  DATE NOT NULL,
  propuesto_por                      UUID NOT NULL,
  propuesto_en                       TIMESTAMPTZ NOT NULL,
  aprobado_por                       UUID,
  aprobado_en                        TIMESTAMPTZ,
  estado                             VARCHAR(12) NOT NULL,
  clave_idempotencia                 UUID NOT NULL,
  CONSTRAINT pk_version_parametro PRIMARY KEY (id),
  CONSTRAINT ck_version_parametro_estado CHECK (estado IN ('APROBADA', 'PROPUESTA', 'RECHAZADA'))
);

COMMENT ON TABLE tarifas.version_parametro IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. Un valor de un parámetro, con su vigencia, su procedencia, quién lo propuso y quién —otra persona— lo aprobó.';
COMMENT ON COLUMN tarifas.version_parametro.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.version_parametro.parametro_id IS 'FK, IDX';
COMMENT ON COLUMN tarifas.version_parametro.nivel_codigo IS 'NULL';
COMMENT ON COLUMN tarifas.version_parametro.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN tarifas.version_parametro.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN tarifas.version_parametro.aprobado_por IS 'NULL';
COMMENT ON COLUMN tarifas.version_parametro.aprobado_en IS 'NULL';
COMMENT ON COLUMN tarifas.version_parametro.estado IS 'CK, IDX';
COMMENT ON COLUMN tarifas.version_parametro.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
