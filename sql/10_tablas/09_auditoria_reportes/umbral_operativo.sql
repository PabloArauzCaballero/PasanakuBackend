-- umbral_operativo · módulo 09 — Auditoría, Reportes y Cumplimiento
-- clase de dominio: UmbralOperativo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS catalogo.umbral_operativo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  concepto                           VARCHAR(30) NOT NULL,
  nivel_kyc_requerido                VARCHAR(15) NOT NULL,
  monto_maximo                       NUMERIC(16,2) DEFAULT 0 NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  vigente_desde                      DATE NOT NULL,
  CONSTRAINT pk_umbral_operativo PRIMARY KEY (id),
  CONSTRAINT ck_umbral_operativo_nivel_kyc_requerido CHECK (nivel_kyc_requerido IN ('BASICO', 'COMPLETO', 'INTERMEDIO', 'NINGUNO'))
);

COMMENT ON TABLE catalogo.umbral_operativo IS 'Módulo 09 — Auditoría, Reportes y Cumplimiento. Cuánto puede mover un usuario según su nivel de KYC.';
COMMENT ON COLUMN catalogo.umbral_operativo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN catalogo.umbral_operativo.concepto IS 'UQ+nivel_kyc_requerido';
COMMENT ON COLUMN catalogo.umbral_operativo.nivel_kyc_requerido IS 'CK';
COMMENT ON COLUMN catalogo.umbral_operativo.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN catalogo.umbral_operativo.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
