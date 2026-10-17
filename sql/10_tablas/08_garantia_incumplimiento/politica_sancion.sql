-- politica_sancion · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- clase de dominio: PoliticaSancion
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.politica_sancion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID,
  version                            VARCHAR(20) NOT NULL,
  requiere_acuerdo_grupo             BOOLEAN DEFAULT FALSE NOT NULL,
  plazo_descargo_dias                SMALLINT NOT NULL,
  plazo_apelacion_dias               SMALLINT NOT NULL,
  prescribe_en_dias                  SMALLINT NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_politica_sancion PRIMARY KEY (id)
);

COMMENT ON TABLE garantia.politica_sancion IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. Las reglas del proceso sancionatorio: si requiere acuerdo del grupo, plazos de descargo y apelación, y prescripción.';
COMMENT ON COLUMN garantia.politica_sancion.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN garantia.politica_sancion.grupo_id IS 'FK, NULL';
COMMENT ON COLUMN garantia.politica_sancion.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
