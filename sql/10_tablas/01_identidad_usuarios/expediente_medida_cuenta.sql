-- expediente_medida_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: ExpedienteMedidaCuenta
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.expediente_medida_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  politica_medida_cuenta_id          UUID NOT NULL,
  causal                             VARCHAR(40) NOT NULL,
  medida                             VARCHAR(25) NOT NULL,
  cautelar                           BOOLEAN DEFAULT FALSE NOT NULL,
  motivo                             VARCHAR(500) NOT NULL,
  estado                             VARCHAR(25) NOT NULL,
  estado_cuenta_previo               VARCHAR(25) NOT NULL,
  propuesta_por                      UUID NOT NULL,
  propuesta_en                       TIMESTAMPTZ NOT NULL,
  notificada_en                      TIMESTAMPTZ,
  fecha_limite_descargo              TIMESTAMPTZ,
  decidida_por                       UUID,
  decidida_en                        TIMESTAMPTZ,
  resultado_decision                 VARCHAR(15),
  fundamento_decision                TEXT,
  fecha_limite_apelacion             TIMESTAMPTZ,
  vigente_hasta                      TIMESTAMPTZ,
  prescribe_en                       TIMESTAMPTZ NOT NULL,
  firme_en                           TIMESTAMPTZ,
  cerrada_en                         TIMESTAMPTZ,
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_expediente_medida_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_expediente_medida_cuenta_causal CHECK (causal IN ('ABUSO_CONTRA_PARTICIPANTES', 'CUENTA_COMPROMETIDA', 'CUENTA_DUPLICADA_O_DE_TERCERO', 'FRAUDE_CON_EVIDENCIA')),
  CONSTRAINT ck_expediente_medida_cuenta_medida CHECK (medida IN ('BLOQUEO_CAUTELAR', 'SUSPENSION_TEMPORAL')),
  CONSTRAINT ck_expediente_medida_cuenta_estado CHECK (estado IN ('CAUTELAR', 'CUMPLIDA', 'DECIDIDA', 'DESESTIMADA', 'EN_APELACION', 'EN_DESCARGO', 'FIRME', 'NOTIFICACION_PENDIENTE', 'PRESCRITO', 'PROPUESTA', 'REVOCADA')),
  CONSTRAINT ck_expediente_medida_cuenta_estado_cuenta_previo CHECK (estado_cuenta_previo IN ('ACTIVO', 'INCOMPLETO_KYC', 'PENDIENTE_VERIFICACION', 'PRE_REGISTRO')),
  CONSTRAINT ck_expediente_medida_cuenta_resultado_decision CHECK (resultado_decision IN ('CONFIRMADA', 'DESESTIMADA'))
);

COMMENT ON TABLE identidad.expediente_medida_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. El expediente de una medida de seguridad sobre la cuenta de un participante (bloqueo cautelar o suspensión temporal), con su causal cerrada, sus plazos guardados y su decisión motivada.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.usuario_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.politica_medida_cuenta_id IS 'FK';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.causal IS 'motivo del catálogo cerrado; mora, orden de autoridad y sospecha de lavado van por otros casos de uso.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.medida IS 'bloqueo cautelar inmediato o suspensión temporal.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.cautelar IS 'si la medida se aplicó antes del descargo, cosa que solo admiten la cuenta comprometida y el fraude con evidencia.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.estado IS 'CK, IDX';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.estado_cuenta_previo IS 'estado de la cuenta antes de la medida, al que vuelve cuando la medida termina.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.propuesta_por IS 'FK';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.notificada_en IS 'NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.fecha_limite_descargo IS 'plazo de descargo calculado en días hábiles al notificar y guardado; no se recalcula.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.decidida_por IS 'FK, NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.decidida_en IS 'NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.resultado_decision IS 'CK, NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.fundamento_decision IS 'NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.fecha_limite_apelacion IS 'plazo de apelación calculado al decidir.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.vigente_hasta IS 'fin de la suspensión temporal.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.prescribe_en IS 'fecha en la que el expediente prescribe si nadie decidió.';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.firme_en IS 'NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.cerrada_en IS 'NULL';
COMMENT ON COLUMN identidad.expediente_medida_cuenta.clave_idempotencia IS 'el mismo pedido reenviado devuelve el mismo expediente.';
