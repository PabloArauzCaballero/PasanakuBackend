-- sancion_organizador · módulo 07 — Organizador y Automatización
-- clase de dominio: SancionOrganizador
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS organizador.sancion_organizador (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  organizador_id                     UUID NOT NULL,
  evaluacion_id                      UUID,
  tipo                               VARCHAR(25) NOT NULL,
  motivo                             VARCHAR(300) NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  vigente_hasta                      TIMESTAMPTZ,
  estado                             VARCHAR(15) NOT NULL,
  aplicada_por                       UUID NOT NULL,
  CONSTRAINT pk_sancion_organizador PRIMARY KEY (id),
  CONSTRAINT ck_sancion_organizador_tipo CHECK (tipo IN ('ADVERTENCIA', 'INHABILITACION', 'REDUCCION_LIMITE', 'SUSPENSION')),
  CONSTRAINT ck_sancion_organizador_estado CHECK (estado IN ('APELADA', 'CUMPLIDA', 'REVOCADA', 'VIGENTE'))
);

COMMENT ON TABLE organizador.sancion_organizador IS 'Módulo 07 — Organizador y Automatización. La consecuencia por mal desempeño: advertencia, reducción de límite, suspensión o inhabilitación.';
COMMENT ON COLUMN organizador.sancion_organizador.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN organizador.sancion_organizador.organizador_id IS 'FK, IDX';
COMMENT ON COLUMN organizador.sancion_organizador.evaluacion_id IS 'FK, NULL';
COMMENT ON COLUMN organizador.sancion_organizador.tipo IS 'CK';
COMMENT ON COLUMN organizador.sancion_organizador.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN organizador.sancion_organizador.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
COMMENT ON COLUMN organizador.sancion_organizador.estado IS 'CK, IDX';
COMMENT ON COLUMN organizador.sancion_organizador.aplicada_por IS 'FK';
