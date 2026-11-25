-- politica_medida_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: PoliticaMedidaCuenta
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.politica_medida_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  version                            VARCHAR(20) NOT NULL,
  plazo_descargo_dias_habiles        SMALLINT NOT NULL,
  plazo_apelacion_dias_habiles       SMALLINT NOT NULL,
  plazo_resolucion_apelacion_dias_habiles SMALLINT,
  prescribe_en_meses                 SMALLINT NOT NULL,
  duracion_maxima_suspension_dias    SMALLINT NOT NULL,
  fuente                             VARCHAR(200) NOT NULL,
  aprobada_por                       UUID,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_politica_medida_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_politica_medida_cuenta_plazo_descargo_dias_habiles CHECK (plazo_descargo_dias_habiles > 0),
  CONSTRAINT ck_politica_medida_cuenta_plazo_apelacion_dias_habiles CHECK (plazo_apelacion_dias_habiles > 0),
  CONSTRAINT ck_politica_medida_cuenta_prescribe_en_meses CHECK (prescribe_en_meses > 0),
  CONSTRAINT ck_politica_medida_cuenta_duracion_maxima_suspension_dias CHECK (duracion_maxima_suspension_dias > 0)
);

COMMENT ON TABLE identidad.politica_medida_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. La política versionada que fija los plazos de una medida sobre la cuenta de un participante: descargo, apelación, resolución de la apelación, prescripción y duración máxima de la suspensión.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.version IS 'UQ';
COMMENT ON COLUMN identidad.politica_medida_cuenta.plazo_descargo_dias_habiles IS 'días hábiles que tiene la persona para presentar su descargo, contados desde la notificación entregada.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.plazo_apelacion_dias_habiles IS 'días hábiles para apelar desde que se le comunica la decisión.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.plazo_resolucion_apelacion_dias_habiles IS 'días hábiles que tiene la segunda instancia para resolver; vacío mientras no se decida, y entonces no hay resolución automática por vencimiento.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.prescribe_en_meses IS 'meses desde la apertura tras los cuales un expediente sin decisión prescribe y no se reabre por el mismo hecho.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.duracion_maxima_suspension_dias IS 'tope en días corridos de una suspensión temporal.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.fuente IS 'de dónde salen los valores (decisión de negocio, norma o contrato), para que nadie los confunda con una cifra regulatoria.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.aprobada_por IS 'quién aprobó la versión.';
COMMENT ON COLUMN identidad.politica_medida_cuenta.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
