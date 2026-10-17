-- contrato_organizador · módulo 07 — Organizador y Automatización
-- clase de dominio: ContratoOrganizador
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS organizador.contrato_organizador (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  organizador_id                     UUID NOT NULL,
  version                            VARCHAR(20) NOT NULL,
  contenido_hash                     VARCHAR(64) NOT NULL,
  obligaciones                       TEXT NOT NULL,
  causales_rescision                 TEXT NOT NULL,
  firmado_en                         TIMESTAMPTZ,
  token_firma_id                     UUID,
  vigente_desde                      DATE NOT NULL,
  vigente_hasta                      DATE,
  rescindido_en                      TIMESTAMPTZ,
  motivo_rescision                   VARCHAR(300),
  CONSTRAINT pk_contrato_organizador PRIMARY KEY (id)
);

COMMENT ON TABLE organizador.contrato_organizador IS 'Módulo 07 — Organizador y Automatización. El contrato firmado entre el organizador y la plataforma, versionado y sellado con hash.';
COMMENT ON COLUMN organizador.contrato_organizador.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN organizador.contrato_organizador.organizador_id IS 'FK, IDX';
COMMENT ON COLUMN organizador.contrato_organizador.firmado_en IS 'NULL';
COMMENT ON COLUMN organizador.contrato_organizador.token_firma_id IS 'FK, NULL, M1';
COMMENT ON COLUMN organizador.contrato_organizador.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN organizador.contrato_organizador.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
COMMENT ON COLUMN organizador.contrato_organizador.rescindido_en IS 'NULL';
COMMENT ON COLUMN organizador.contrato_organizador.motivo_rescision IS 'NULL';
