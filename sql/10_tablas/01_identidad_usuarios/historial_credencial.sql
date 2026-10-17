-- historial_credencial · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: HistorialCredencial
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.historial_credencial (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  hash_contrasena                    VARCHAR(255) NOT NULL,
  reemplazada_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_credencial PRIMARY KEY (id)
);

COMMENT ON TABLE identidad.historial_credencial IS 'Módulo 01 — Identidad, Usuarios y Seguridad. Los hashes de las contraseñas anteriores.';
COMMENT ON COLUMN identidad.historial_credencial.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.historial_credencial.usuario_id IS 'FK, IDX';
