-- politica_billetera · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: PoliticaBilletera
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.politica_billetera (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(40) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  dias_inactividad_para_limitar      SMALLINT NOT NULL,
  permite_transferencia_p2p          BOOLEAN DEFAULT FALSE NOT NULL,
  requiere_mfa_desde                 NUMERIC(16,2) NOT NULL,
  ventana_enfriamiento_retiro_horas  SMALLINT NOT NULL,
  dias_vigencia_retencion            SMALLINT NOT NULL,
  permite_saldo_negativo             BOOLEAN DEFAULT FALSE NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  aprobada_por                       UUID,
  CONSTRAINT pk_politica_billetera PRIMARY KEY (id)
);

COMMENT ON TABLE nucleo_financiero.politica_billetera IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. Los parámetros de comportamiento de las billeteras, versionados.';
COMMENT ON COLUMN nucleo_financiero.politica_billetera.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.politica_billetera.codigo IS 'UQ';
COMMENT ON COLUMN nucleo_financiero.politica_billetera.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN nucleo_financiero.politica_billetera.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
COMMENT ON COLUMN nucleo_financiero.politica_billetera.aprobada_por IS 'FK, NULL';
