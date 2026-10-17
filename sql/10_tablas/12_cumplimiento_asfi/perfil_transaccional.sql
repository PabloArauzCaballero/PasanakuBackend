-- perfil_transaccional · módulo 12 — Cumplimiento Regulatorio y Consumidor Financiero
-- clase de dominio: PerfilTransaccional
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS cumplimiento.perfil_transaccional (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  tipo                               VARCHAR(10) NOT NULL,
  monto_mensual_estimado             NUMERIC(16,2) DEFAULT 0 NOT NULL,
  cantidad_operaciones_estimada      INTEGER DEFAULT 0 NOT NULL,
  actividad_economica                VARCHAR(120) NOT NULL,
  codigo_ciiu                        VARCHAR(10),
  origen_fondos_declarado            VARCHAR(200) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  fuente                             VARCHAR(30) NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  actualizado_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_perfil_transaccional PRIMARY KEY (id),
  CONSTRAINT ck_perfil_transaccional_tipo CHECK (tipo IN ('DECLARADO', 'OBSERVADO'))
);

COMMENT ON TABLE cumplimiento.perfil_transaccional IS 'Módulo 12 — Cumplimiento Regulatorio y Consumidor Financiero. Lo que el cliente declaró que iba a mover, y lo que efectivamente movió.';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.usuario_id IS 'FK, IDX, M1';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.tipo IS 'CK';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.codigo_ciiu IS 'NULL';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN cumplimiento.perfil_transaccional.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
