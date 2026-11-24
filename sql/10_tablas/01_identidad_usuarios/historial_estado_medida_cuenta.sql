-- historial_estado_medida_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: HistorialEstadoMedidaCuenta
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.historial_estado_medida_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  expediente_medida_cuenta_id        UUID NOT NULL,
  estado_anterior                    VARCHAR(25),
  estado_nuevo                       VARCHAR(25) NOT NULL,
  ejecutado_por                      UUID,
  motivo                             VARCHAR(500),
  ocurrido_en                        TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_medida_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_historial_estado_medida_cuenta_estado_anterior CHECK (estado_anterior IN ('CAUTELAR', 'CUMPLIDA', 'DECIDIDA', 'DESESTIMADA', 'EN_APELACION', 'EN_DESCARGO', 'FIRME', 'NOTIFICACION_PENDIENTE', 'PRESCRITO', 'PROPUESTA', 'REVOCADA')),
  CONSTRAINT ck_historial_estado_medida_cuenta_estado_nuevo CHECK (estado_nuevo IN ('CAUTELAR', 'CUMPLIDA', 'DECIDIDA', 'DESESTIMADA', 'EN_APELACION', 'EN_DESCARGO', 'FIRME', 'NOTIFICACION_PENDIENTE', 'PRESCRITO', 'PROPUESTA', 'REVOCADA'))
);

COMMENT ON TABLE identidad.historial_estado_medida_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. [append-only] Cada cambio de estado del expediente, con quién lo hizo y por qué.';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.expediente_medida_cuenta_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.estado_anterior IS 'CK, NULL';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.estado_nuevo IS 'CK';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.ejecutado_por IS 'FK, NULL';
COMMENT ON COLUMN identidad.historial_estado_medida_cuenta.motivo IS 'NULL';
