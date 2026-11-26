-- solicitud_recuperacion_acceso · módulo 01 — Identidad, Usuarios y Seguridad
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.solicitud_recuperacion_acceso (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  alcance                            VARCHAR(10) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  solicitada_por                     UUID,
  solicitada_en                      TIMESTAMPTZ DEFAULT now() NOT NULL,
  motivo                             VARCHAR(200) NOT NULL,
  decidida_por                       UUID,
  decidida_en                        TIMESTAMPTZ,
  motivo_decision                    VARCHAR(200),
  enfriamiento_hasta                 TIMESTAMPTZ,
  token_id                           UUID,
  emitida_en                         TIMESTAMPTZ,
  completada_en                      TIMESTAMPTZ,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  CONSTRAINT pk_solicitud_recuperacion_acceso PRIMARY KEY (id),
  CONSTRAINT ck_solicitud_recuperacion_acceso_alcance CHECK (alcance IN ('CLAVE', 'FACTOR')),
  CONSTRAINT ck_solicitud_recuperacion_acceso_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA', 'EMITIDA', 'COMPLETADA', 'VENCIDA'))
);

COMMENT ON TABLE identidad.solicitud_recuperacion_acceso IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El expediente con el que un operador del backoffice recupera su clave o reinscribe su segundo factor: quién lo pidió, quién lo decidió, cuándo termina el enfriamiento y qué invitación se emitió.';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.alcance IS 'CK: CLAVE|FACTOR';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.estado IS 'CK: PENDIENTE|APROBADA|RECHAZADA|EMITIDA|COMPLETADA|VENCIDA';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.solicitada_por IS 'FK, NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.decidida_por IS 'FK, NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.decidida_en IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.motivo_decision IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.enfriamiento_hasta IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.token_id IS 'FK, NULL, UQ';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.emitida_en IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.completada_en IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_recuperacion_acceso.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
