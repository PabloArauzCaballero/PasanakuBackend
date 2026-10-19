-- aviso_interno · módulo 05 — Notificaciones y Comunicaciones
-- clase de dominio: AvisoInterno
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS notificaciones.aviso_interno (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID,
  rol_destinatario                   VARCHAR(50),
  titulo                             VARCHAR(150) NOT NULL,
  mensaje                            TEXT NOT NULL,
  tipo                               VARCHAR(20) NOT NULL,
  enlace                             VARCHAR(300),
  leido_en                           TIMESTAMPTZ,
  creado_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_aviso_interno PRIMARY KEY (id),
  CONSTRAINT ck_aviso_interno_tipo CHECK (tipo IN ('INFO', 'ALERTA', 'CASO', 'REVISION', 'DESPLIEGUE', 'SISTEMA'))
);

COMMENT ON TABLE notificaciones.aviso_interno IS 'Módulo 05 — Notificaciones y Comunicaciones. El registro persistente de avisos dirigidos a operadores internos o a roles funcionales en el backoffice.';
COMMENT ON COLUMN notificaciones.aviso_interno.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN notificaciones.aviso_interno.usuario_id IS 'FK, NULL, IDX';
COMMENT ON COLUMN notificaciones.aviso_interno.rol_destinatario IS 'NULL, IDX';
COMMENT ON COLUMN notificaciones.aviso_interno.tipo IS 'CK: INFO|ALERTA|CASO|REVISION|DESPLIEGUE|SISTEMA';
COMMENT ON COLUMN notificaciones.aviso_interno.enlace IS 'NULL';
COMMENT ON COLUMN notificaciones.aviso_interno.leido_en IS 'NULL, IDX';
COMMENT ON COLUMN notificaciones.aviso_interno.creado_en IS 'IDX';
