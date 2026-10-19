-- rol · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: Rol
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.rol (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(30) NOT NULL,
  nombre                             VARCHAR(60) NOT NULL,
  ambito                             VARCHAR(15) NOT NULL,
  departamento                       VARCHAR(60),
  descripcion                        VARCHAR(255),
  es_sistema                         BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_rol PRIMARY KEY (id),
  CONSTRAINT ck_rol_ambito CHECK (ambito IN ('GLOBAL', 'GRUPO', 'ORGANIZACION'))
);

COMMENT ON TABLE identidad.rol IS 'Módulo 01 — Identidad, Usuarios y Seguridad. Un conjunto nombrado de permisos: administrador, soporte, auditor, organizador, participante, invitado.';
COMMENT ON COLUMN identidad.rol.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.rol.codigo IS 'UQ';
COMMENT ON COLUMN identidad.rol.ambito IS 'CK';
COMMENT ON COLUMN identidad.rol.departamento IS 'NULL';
COMMENT ON COLUMN identidad.rol.descripcion IS 'NULL';
