-- ficha_gobierno_tabla · módulo 09 — Auditoría, Reportes y Cumplimiento
-- clase de dominio: FichaGobiernoTabla
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS auditoria.ficha_gobierno_tabla (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  esquema                            VARCHAR(63) NOT NULL,
  tabla                              VARCHAR(63) NOT NULL,
  estado                             VARCHAR(20) NOT NULL,
  estado_revision                    VARCHAR(20) NOT NULL,
  responsable                        VARCHAR(80),
  politica_retencion_id              UUID,
  critica_para_auditoria             BOOLEAN DEFAULT FALSE NOT NULL,
  nota_revision                      VARCHAR(500),
  revisado_por                       UUID,
  actualizado_en                     TIMESTAMPTZ NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_ficha_gobierno_tabla PRIMARY KEY (id),
  CONSTRAINT ck_ficha_gobierno_tabla_estado CHECK (estado IN ('ACTIVA', 'CANDIDATA_A_RETIRO', 'DESHABILITADA', 'PLANIFICADA', 'RETIRADA')),
  CONSTRAINT ck_ficha_gobierno_tabla_estado_revision CHECK (estado_revision IN ('APROBADA', 'DETECTADA', 'NECESITA_REVISION', 'RECHAZADA'))
);

COMMENT ON TABLE auditoria.ficha_gobierno_tabla IS 'Módulo 09 — Auditoría, Reportes y Cumplimiento. La ficha de gobierno de cada tabla del modelo: si está en uso (estado), si alguien ya revisó su descripción (revisión), quién responde por ella y qué política de retención le aplica.';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.esquema IS 'UQ+tabla';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.estado IS 'CK';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.estado_revision IS 'CK';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.responsable IS 'NULL';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.politica_retencion_id IS 'FK, NULL';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.nota_revision IS 'NULL';
COMMENT ON COLUMN auditoria.ficha_gobierno_tabla.revisado_por IS 'NULL';
