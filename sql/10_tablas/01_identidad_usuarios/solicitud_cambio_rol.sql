-- solicitud_cambio_rol · módulo 01 — Identidad, Usuarios y Seguridad
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.solicitud_cambio_rol (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tipo                               VARCHAR(10) NOT NULL,
  rol_id                             UUID,
  propuesta                          JSONB NOT NULL,
  motivo                             VARCHAR(160) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  propuesta_por                      UUID NOT NULL,
  propuesta_en                       TIMESTAMPTZ NOT NULL,
  resuelta_por                       UUID,
  resuelta_en                        TIMESTAMPTZ,
  motivo_resolucion                  VARCHAR(160),
  CONSTRAINT pk_solicitud_cambio_rol PRIMARY KEY (id),
  CONSTRAINT ck_solicitud_cambio_rol_tipo CHECK (tipo IN ('CREAR', 'EDITAR', 'RETIRAR')),
  CONSTRAINT ck_solicitud_cambio_rol_estado CHECK (estado IN ('PENDIENTE', 'APROBADA', 'RECHAZADA'))
);

COMMENT ON TABLE identidad.solicitud_cambio_rol IS 'Módulo 01 — Identidad, Usuarios y Seguridad. La propuesta de crear, editar o retirar un rol personalizado cuando el rol toca permisos de riesgo ALTO o CRITICO del catálogo: queda pendiente hasta que otra persona la aprueba o la rechaza (cuatro ojos).';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.tipo IS 'CK: CREAR|EDITAR|RETIRAR';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.rol_id IS 'NULL, FK, IDX';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.estado IS 'CK: PENDIENTE|APROBADA|RECHAZADA';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.propuesta_por IS 'FK, IDX';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.resuelta_por IS 'NULL, FK';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.resuelta_en IS 'NULL';
COMMENT ON COLUMN identidad.solicitud_cambio_rol.motivo_resolucion IS 'NULL';
