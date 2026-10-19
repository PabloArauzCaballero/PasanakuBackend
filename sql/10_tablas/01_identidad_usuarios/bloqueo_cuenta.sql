-- bloqueo_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: BloqueoCuenta
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.bloqueo_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  motivo                             VARCHAR(30) NOT NULL,
  bloqueada_en                       TIMESTAMPTZ NOT NULL,
  desbloquea_en                      TIMESTAMPTZ,
  liberada_en                        TIMESTAMPTZ,
  liberada_por                       UUID,
  CONSTRAINT pk_bloqueo_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_bloqueo_cuenta_motivo CHECK (motivo IN ('FRAUDE', 'INTENTOS_FALLIDOS', 'ORDEN_ADMIN', 'SOLICITUD_USUARIO'))
);

COMMENT ON TABLE identidad.bloqueo_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El bloqueo de acceso, temporal o indefinido, con su motivo.';
COMMENT ON COLUMN identidad.bloqueo_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.bloqueo_cuenta.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.bloqueo_cuenta.motivo IS 'CK';
COMMENT ON COLUMN identidad.bloqueo_cuenta.desbloquea_en IS 'NULL';
COMMENT ON COLUMN identidad.bloqueo_cuenta.liberada_en IS 'NULL';
COMMENT ON COLUMN identidad.bloqueo_cuenta.liberada_por IS 'FK, NULL';
