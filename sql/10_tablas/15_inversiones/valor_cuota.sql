-- valor_cuota · módulo 15 — Inversiones voluntarias
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.valor_cuota (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  producto_inversion_id              UUID NOT NULL,
  fecha                              DATE NOT NULL,
  valor                              NUMERIC(18,6) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  fuente                             VARCHAR(200) NOT NULL,
  origen_datos                       VARCHAR(12) NOT NULL,
  publicado_en                       TIMESTAMPTZ NOT NULL,
  recibido_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_valor_cuota PRIMARY KEY (id),
  CONSTRAINT ck_valor_cuota_valor CHECK (valor > 0),
  CONSTRAINT ck_valor_cuota_origen_datos CHECK (origen_datos IN ('SINTETICO', 'VERIFICADO'))
);

COMMENT ON TABLE inversiones.valor_cuota IS 'Módulo 15 — Inversiones voluntarias. [append-only] Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.valor_cuota.id IS 'PK';
COMMENT ON COLUMN inversiones.valor_cuota.producto_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.valor_cuota.fecha IS 'UQ+producto_inversion_id';
COMMENT ON COLUMN inversiones.valor_cuota.valor IS 'CK: > 0';
COMMENT ON COLUMN inversiones.valor_cuota.origen_datos IS 'CK: SINTETICO | VERIFICADO';
