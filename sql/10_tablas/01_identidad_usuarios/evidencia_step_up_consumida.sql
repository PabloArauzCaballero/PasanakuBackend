-- evidencia_step_up_consumida · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: EvidenciaStepUpConsumida
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.evidencia_step_up_consumida (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  jti                                UUID NOT NULL,
  usuario_id                         UUID NOT NULL,
  proposito                          VARCHAR(20) NOT NULL,
  consumida_en                       TIMESTAMPTZ NOT NULL,
  vence_en                           TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_evidencia_step_up_consumida PRIMARY KEY (id),
  CONSTRAINT ck_evidencia_step_up_consumida_proposito CHECK (proposito IN ('ADMIN'))
);

COMMENT ON TABLE identidad.evidencia_step_up_consumida IS 'Módulo 01 — Identidad, Usuarios y Seguridad. [append-only] La marca de que una evidencia de segundo factor (el jti del JWT de step-up con propósito ADMIN) ya se usó para un acto de gobierno de accesos: asignar o revocar un rol, dar de alta, suspender, reactivar, dar de baja o desbloquear a un operador.';
COMMENT ON COLUMN identidad.evidencia_step_up_consumida.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.evidencia_step_up_consumida.jti IS 'UQ, consumo de un solo uso';
COMMENT ON COLUMN identidad.evidencia_step_up_consumida.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.evidencia_step_up_consumida.proposito IS 'CK';
COMMENT ON COLUMN identidad.evidencia_step_up_consumida.vence_en IS 'IDX';
