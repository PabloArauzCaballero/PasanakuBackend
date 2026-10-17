-- restriccion_usuario · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: RestriccionUsuario
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.restriccion_usuario (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  tipo                               VARCHAR(30) NOT NULL,
  origen                             VARCHAR(20) NOT NULL,
  referencia_origen_id               UUID,
  valor_limite                       NUMERIC(14,2),
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  vigente_hasta                      TIMESTAMPTZ,
  levantada_por                      UUID,
  motivo_levantamiento               VARCHAR(160),
  CONSTRAINT pk_restriccion_usuario PRIMARY KEY (id),
  CONSTRAINT ck_restriccion_usuario_tipo CHECK (tipo IN ('LIMITE_MONTO', 'NO_CREAR_GRUPO', 'NO_SER_ORGANIZADOR', 'NO_UNIRSE')),
  CONSTRAINT ck_restriccion_usuario_origen CHECK (origen IN ('INCUMPLIMIENTO', 'LEGAL', 'RIESGO'))
);

COMMENT ON TABLE identidad.restriccion_usuario IS 'Módulo 01 — Identidad, Usuarios y Seguridad. Una limitación puntual sobre lo que el usuario puede hacer, sin bloquearle el acceso: no crear grupos, no unirse a nuevos, no ser organizador, o un tope de monto.';
COMMENT ON COLUMN identidad.restriccion_usuario.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.restriccion_usuario.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.restriccion_usuario.tipo IS 'CK';
COMMENT ON COLUMN identidad.restriccion_usuario.origen IS 'CK';
COMMENT ON COLUMN identidad.restriccion_usuario.referencia_origen_id IS 'NULL';
COMMENT ON COLUMN identidad.restriccion_usuario.valor_limite IS 'NULL';
COMMENT ON COLUMN identidad.restriccion_usuario.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN identidad.restriccion_usuario.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
COMMENT ON COLUMN identidad.restriccion_usuario.levantada_por IS 'FK, NULL';
COMMENT ON COLUMN identidad.restriccion_usuario.motivo_levantamiento IS 'NULL';
