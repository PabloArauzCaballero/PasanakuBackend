-- usuario_interno · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: UsuarioInterno
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.usuario_interno (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  departamento                       VARCHAR(60) NOT NULL,
  cargo                              VARCHAR(80) NOT NULL,
  debe_cambiar_contrasena            BOOLEAN DEFAULT FALSE NOT NULL,
  bloqueado_hasta                    TIMESTAMPTZ,
  motivo_bloqueo                     VARCHAR(160),
  CONSTRAINT pk_usuario_interno PRIMARY KEY (id)
);

COMMENT ON TABLE identidad.usuario_interno IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El perfil laboral y operativo del usuario que desempeña funciones internas en la plataforma.';
COMMENT ON COLUMN identidad.usuario_interno.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.usuario_interno.usuario_id IS 'FK, UQ';
COMMENT ON COLUMN identidad.usuario_interno.bloqueado_hasta IS 'NULL';
COMMENT ON COLUMN identidad.usuario_interno.motivo_bloqueo IS 'NULL';
