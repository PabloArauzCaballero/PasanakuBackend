-- instruccion_libro · módulo 15 — Inversiones voluntarias
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.instruccion_libro (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  origen_tipo                        VARCHAR(10) NOT NULL,
  origen_id                          UUID NOT NULL,
  tipo                               VARCHAR(10) NOT NULL,
  usuario_id                         UUID NOT NULL,
  cuenta_billetera_id                UUID NOT NULL,
  monto                              NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  clave_idempotencia                 VARCHAR(100) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  referencia_libro                   UUID,
  intentos                           INTEGER DEFAULT 0 NOT NULL,
  ultimo_error                       VARCHAR(60),
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  aplicada_en                        TIMESTAMPTZ,
  CONSTRAINT pk_instruccion_libro PRIMARY KEY (id),
  CONSTRAINT ck_instruccion_libro_origen_tipo CHECK (origen_tipo IN ('ORDEN', 'RESCATE')),
  CONSTRAINT ck_instruccion_libro_tipo CHECK (tipo IN ('RETENER', 'LIBERAR', 'DEBITAR', 'ACREDITAR')),
  CONSTRAINT ck_instruccion_libro_monto CHECK (monto > 0),
  CONSTRAINT ck_instruccion_libro_estado CHECK (estado IN ('PENDIENTE', 'APLICADA', 'FALLIDA'))
);

COMMENT ON TABLE inversiones.instruccion_libro IS 'Módulo 15 — Inversiones voluntarias. Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.instruccion_libro.id IS 'PK';
COMMENT ON COLUMN inversiones.instruccion_libro.origen_tipo IS 'CK: ORDEN | RESCATE, IDX';
COMMENT ON COLUMN inversiones.instruccion_libro.origen_id IS 'IDX, polimorfica';
COMMENT ON COLUMN inversiones.instruccion_libro.tipo IS 'CK: RETENER | LIBERAR | DEBITAR | ACREDITAR';
COMMENT ON COLUMN inversiones.instruccion_libro.monto IS 'CK: > 0';
COMMENT ON COLUMN inversiones.instruccion_libro.clave_idempotencia IS 'UQ';
COMMENT ON COLUMN inversiones.instruccion_libro.estado IS 'CK: PENDIENTE | APLICADA | FALLIDA, IDX';
COMMENT ON COLUMN inversiones.instruccion_libro.referencia_libro IS 'NULL';
COMMENT ON COLUMN inversiones.instruccion_libro.ultimo_error IS 'NULL';
COMMENT ON COLUMN inversiones.instruccion_libro.aplicada_en IS 'NULL';
