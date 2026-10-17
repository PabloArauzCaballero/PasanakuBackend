-- consentimiento · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: Consentimiento
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.consentimiento (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  tipo                               VARCHAR(30) NOT NULL,
  version_documento                  VARCHAR(20) NOT NULL,
  hash_documento                     VARCHAR(64) NOT NULL,
  otorgado                           BOOLEAN DEFAULT FALSE NOT NULL,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  ip_origen                          INET NOT NULL,
  agente_usuario                     VARCHAR(255) NOT NULL,
  revocado_en                        TIMESTAMPTZ,
  CONSTRAINT pk_consentimiento PRIMARY KEY (id),
  CONSTRAINT ck_consentimiento_tipo CHECK (tipo IN ('MARKETING', 'PRIVACIDAD', 'REGLAMENTO_GRUPO', 'TERMINOS', 'TRATAMIENTO_DATOS'))
);

COMMENT ON TABLE identidad.consentimiento IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El registro de que el usuario aceptó un documento: términos, privacidad, tratamiento de datos, marketing, reglamento del grupo — con la versión y el hash del texto exacto que aceptó.';
COMMENT ON COLUMN identidad.consentimiento.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.consentimiento.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.consentimiento.tipo IS 'CK';
COMMENT ON COLUMN identidad.consentimiento.revocado_en IS 'NULL';
