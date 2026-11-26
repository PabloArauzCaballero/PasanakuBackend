-- cuenta_servicio · módulo 01 — Identidad, Usuarios y Seguridad
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.cuenta_servicio (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  rol_id                             UUID NOT NULL,
  nombre                             VARCHAR(80) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  vence_en                           TIMESTAMPTZ NOT NULL,
  creada_por                         UUID NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  credencial_rotada_en               TIMESTAMPTZ NOT NULL,
  revocada_por                       UUID,
  revocada_en                        TIMESTAMPTZ,
  motivo_revocacion                  VARCHAR(160),
  CONSTRAINT pk_cuenta_servicio PRIMARY KEY (id),
  CONSTRAINT ck_cuenta_servicio_estado CHECK (estado IN ('ACTIVA', 'REVOCADA'))
);

COMMENT ON TABLE identidad.cuenta_servicio IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El gobierno de una identidad de servicio: la cuenta de usuario que porta un rol SERVICIO_*, con su vencimiento, el momento en que se rotó su credencial por última vez y, si se revocó, quién y por qué.';
COMMENT ON COLUMN identidad.cuenta_servicio.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.cuenta_servicio.usuario_id IS 'FK, UQ';
COMMENT ON COLUMN identidad.cuenta_servicio.rol_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.cuenta_servicio.estado IS 'CK: ACTIVA|REVOCADA';
COMMENT ON COLUMN identidad.cuenta_servicio.vence_en IS 'IDX';
COMMENT ON COLUMN identidad.cuenta_servicio.creada_por IS 'FK';
COMMENT ON COLUMN identidad.cuenta_servicio.revocada_por IS 'NULL, FK';
COMMENT ON COLUMN identidad.cuenta_servicio.revocada_en IS 'NULL';
COMMENT ON COLUMN identidad.cuenta_servicio.motivo_revocacion IS 'NULL';
