-- posicion_inversion · módulo 15 — Inversiones voluntarias
-- clase de dominio: PosicionInversion
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.posicion_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  usuario_id                         UUID NOT NULL,
  producto_inversion_id              UUID NOT NULL,
  version_condiciones_id             UUID NOT NULL,
  orden_inversion_id                 UUID NOT NULL,
  tipo                               VARCHAR(5) NOT NULL,
  principal                          NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  cuotas                             NUMERIC(18,6),
  valor_cuota_entrada                NUMERIC(18,6),
  marca_maxima                       NUMERIC(18,6),
  fecha_constitucion                 DATE NOT NULL,
  fecha_vencimiento                  DATE,
  posicion_externa                   VARCHAR(80) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  creada_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  cerrada_en                         TIMESTAMPTZ,
  CONSTRAINT pk_posicion_inversion PRIMARY KEY (id),
  CONSTRAINT ck_posicion_inversion_tipo CHECK (tipo IN ('DPF', 'FONDO')),
  CONSTRAINT ck_posicion_inversion_principal CHECK (principal > 0),
  CONSTRAINT ck_posicion_inversion_cuotas CHECK (cuotas >= 0),
  CONSTRAINT ck_posicion_inversion_estado CHECK (estado IN ('ABIERTA', 'CERRADA'))
);

COMMENT ON TABLE inversiones.posicion_inversion IS 'Módulo 15 — Inversiones voluntarias. Que una inversion voluntaria nunca prometa rentabilidad ni duplique el saldo';
COMMENT ON COLUMN inversiones.posicion_inversion.id IS 'PK';
COMMENT ON COLUMN inversiones.posicion_inversion.usuario_id IS 'IDX';
COMMENT ON COLUMN inversiones.posicion_inversion.producto_inversion_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.posicion_inversion.version_condiciones_id IS 'FK, IDX';
COMMENT ON COLUMN inversiones.posicion_inversion.orden_inversion_id IS 'FK, UQ';
COMMENT ON COLUMN inversiones.posicion_inversion.tipo IS 'CK: DPF | FONDO';
COMMENT ON COLUMN inversiones.posicion_inversion.principal IS 'CK: > 0';
COMMENT ON COLUMN inversiones.posicion_inversion.cuotas IS 'NULL, CK: >= 0';
COMMENT ON COLUMN inversiones.posicion_inversion.valor_cuota_entrada IS 'NULL';
COMMENT ON COLUMN inversiones.posicion_inversion.marca_maxima IS 'NULL';
COMMENT ON COLUMN inversiones.posicion_inversion.fecha_vencimiento IS 'NULL';
COMMENT ON COLUMN inversiones.posicion_inversion.posicion_externa IS 'UQ';
COMMENT ON COLUMN inversiones.posicion_inversion.estado IS 'CK: ABIERTA | CERRADA, IDX';
COMMENT ON COLUMN inversiones.posicion_inversion.cerrada_en IS 'NULL';
