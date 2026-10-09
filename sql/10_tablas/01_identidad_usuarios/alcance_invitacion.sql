-- alcance_invitacion · módulo 01 — Identidad, Usuarios y Seguridad
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.alcance_invitacion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  token_id                           UUID NOT NULL,
  grupo_destino_id                   UUID NOT NULL,
  emisor_id                          UUID NOT NULL,
  telefono_destino                   VARCHAR(20) NOT NULL,
  nonce                              VARCHAR(64) NOT NULL,
  clave_emision                      UUID NOT NULL,
  huella_solicitud                   VARCHAR(64) NOT NULL,
  clave_consumo                      UUID,
  consumidor_id                      UUID,
  CONSTRAINT pk_alcance_invitacion PRIMARY KEY (id)
);

COMMENT ON TABLE identidad.alcance_invitacion IS 'Módulo 01 — Identidad, Usuarios y Seguridad. Saber con certeza a quién le estás confiando plata ajena';
COMMENT ON COLUMN identidad.alcance_invitacion.id IS 'PK';
COMMENT ON COLUMN identidad.alcance_invitacion.token_id IS 'FK, UQ';
COMMENT ON COLUMN identidad.alcance_invitacion.clave_emision IS 'UQ';
COMMENT ON COLUMN identidad.alcance_invitacion.clave_consumo IS 'NULL';
COMMENT ON COLUMN identidad.alcance_invitacion.consumidor_id IS 'NULL';
