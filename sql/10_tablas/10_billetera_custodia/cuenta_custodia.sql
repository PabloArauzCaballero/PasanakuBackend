-- cuenta_custodia · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: CuentaCustodia
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.cuenta_custodia (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  tipo                               VARCHAR(25) NOT NULL,
  entidad_financiera                 VARCHAR(60) NOT NULL,
  numero_cuenta_cifrado              VARCHAR(255) NOT NULL,
  version_llave                      SMALLINT NOT NULL,
  numero_enmascarado                 VARCHAR(30) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  saldo_segun_banco                  NUMERIC(18,2) DEFAULT 0 NOT NULL,
  saldo_segun_libro                  NUMERIC(18,2) DEFAULT 0 NOT NULL,
  fecha_saldo                        TIMESTAMPTZ NOT NULL,
  contrato_referencia                VARCHAR(80) NOT NULL,
  es_principal                       BOOLEAN DEFAULT FALSE NOT NULL,
  estado                             VARCHAR(15) NOT NULL,
  abierta_en                         DATE NOT NULL,
  CONSTRAINT pk_cuenta_custodia PRIMARY KEY (id),
  CONSTRAINT ck_cuenta_custodia_tipo CHECK (tipo IN ('CUENTA_ENCAJE', 'CUENTA_RECAUDADORA', 'FIDEICOMISO')),
  CONSTRAINT ck_cuenta_custodia_estado CHECK (estado IN ('ACTIVA', 'BLOQUEADA', 'CERRADA', 'INACTIVA'))
);

COMMENT ON TABLE nucleo_financiero.cuenta_custodia IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. La cuenta bancaria real donde está depositado el dinero que respalda todo el saldo de todas las billeteras.';
COMMENT ON COLUMN nucleo_financiero.cuenta_custodia.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.cuenta_custodia.tipo IS 'CK';
COMMENT ON COLUMN nucleo_financiero.cuenta_custodia.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN nucleo_financiero.cuenta_custodia.estado IS 'CK';
