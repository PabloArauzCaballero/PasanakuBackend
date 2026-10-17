-- postulacion_emparejamiento · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: PostulacionEmparejamiento
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS grupos.postulacion_emparejamiento (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  monto_deseado                      NUMERIC(14,2) DEFAULT 0 NOT NULL,
  rango_monto_min                    NUMERIC(14,2) NOT NULL,
  rango_monto_max                    NUMERIC(14,2) NOT NULL,
  periodicidad_deseada               VARCHAR(15) NOT NULL,
  fecha_inicio_deseada               DATE NOT NULL,
  preferencia_turno                  VARCHAR(15) NOT NULL,
  tolerancia_riesgo                  VARCHAR(15) NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  vigente_hasta                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_postulacion_emparejamiento PRIMARY KEY (id),
  CONSTRAINT ck_postulacion_emparejamiento_preferencia_turno CHECK (preferencia_turno IN ('INDIFERENTE', 'TARDIO', 'TEMPRANO')),
  CONSTRAINT ck_postulacion_emparejamiento_estado CHECK (estado IN ('ACTIVA', 'CANCELADA', 'EMPAREJADA', 'EXPIRADA'))
);

COMMENT ON TABLE grupos.postulacion_emparejamiento IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. La declaración de un usuario de que quiere entrar a *algún* grupo con ciertas características, sin tener uno concreto en mente.';
COMMENT ON COLUMN grupos.postulacion_emparejamiento.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN grupos.postulacion_emparejamiento.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN grupos.postulacion_emparejamiento.preferencia_turno IS 'CK';
COMMENT ON COLUMN grupos.postulacion_emparejamiento.estado IS 'CK, IDX';
COMMENT ON COLUMN grupos.postulacion_emparejamiento.vigente_hasta IS 'Hasta cuándo rigió esta versión; vacía mientras sigue vigente.';
