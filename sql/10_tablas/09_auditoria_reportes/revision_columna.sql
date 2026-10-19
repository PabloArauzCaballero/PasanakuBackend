-- revision_columna · módulo 09 — Auditoría, Reportes y Cumplimiento
-- clase de dominio: RevisionColumna
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS auditoria.revision_columna (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  esquema                            VARCHAR(63) NOT NULL,
  tabla                              VARCHAR(63) NOT NULL,
  columna                            VARCHAR(63) NOT NULL,
  decision                           VARCHAR(20) NOT NULL,
  confianza                          VARCHAR(10),
  motivo                             VARCHAR(500),
  revisado_por                       UUID NOT NULL,
  revisado_en                        TIMESTAMPTZ NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_revision_columna PRIMARY KEY (id),
  CONSTRAINT ck_revision_columna_decision CHECK (decision IN ('APROBADA', 'NECESITA_REVISION', 'RECHAZADA')),
  CONSTRAINT ck_revision_columna_confianza CHECK (confianza IN ('ALTA', 'BAJA', 'MEDIA'))
);

COMMENT ON TABLE auditoria.revision_columna IS 'Módulo 09 — Auditoría, Reportes y Cumplimiento. El veredicto de una persona sobre lo que el modelo dice de una columna: aprobada, rechazada o pendiente, con qué confianza y por qué.';
COMMENT ON COLUMN auditoria.revision_columna.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN auditoria.revision_columna.esquema IS 'UQ+tabla+columna';
COMMENT ON COLUMN auditoria.revision_columna.decision IS 'CK';
COMMENT ON COLUMN auditoria.revision_columna.confianza IS 'CK, NULL';
COMMENT ON COLUMN auditoria.revision_columna.motivo IS 'NULL';
