-- consumo_limite · módulo 10 — Billetera, Custodia y Dinero Electrónico
-- clase de dominio: ConsumoLimite
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS nucleo_financiero.consumo_limite (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  cuenta_billetera_id                UUID NOT NULL,
  limite_id                          UUID NOT NULL,
  ventana_inicio                     TIMESTAMPTZ NOT NULL,
  ventana_fin                        TIMESTAMPTZ NOT NULL,
  monto_acumulado                    NUMERIC(16,2) DEFAULT 0 NOT NULL,
  cantidad_acumulada                 INTEGER DEFAULT 0 NOT NULL,
  actualizado_en                     TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_consumo_limite PRIMARY KEY (id)
);

COMMENT ON TABLE nucleo_financiero.consumo_limite IS 'Módulo 10 — Billetera, Custodia y Dinero Electrónico. Cuánto lleva consumido cada cuenta de cada límite en la ventana actual.';
COMMENT ON COLUMN nucleo_financiero.consumo_limite.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN nucleo_financiero.consumo_limite.cuenta_billetera_id IS 'FK, IDX';
COMMENT ON COLUMN nucleo_financiero.consumo_limite.limite_id IS 'FK, IDX';
