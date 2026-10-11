-- consentimiento_inversion · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.consentimiento_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  version_condiciones_id             UUID NOT NULL,
  texto_hash                         CHAR(64) NOT NULL,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  hash_solicitud                     CHAR(64) NOT NULL,
  aceptado_en                        TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_consentimiento_inversion PRIMARY KEY (id)
);

COMMENT ON TABLE inversiones.consentimiento_inversion IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.consentimiento_inversion.id IS 'PK';
COMMENT ON COLUMN inversiones.consentimiento_inversion.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.consentimiento_inversion.version_condiciones_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.consentimiento_inversion.clave_idempotencia IS 'UQ+usuario_id';
