-- producto_inversion · módulo 15 — Inversiones voluntarias
-- clase de dominio: ProductoInversion
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS inversiones.producto_inversion (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(40) NOT NULL,
  tipo                               VARCHAR(5) NOT NULL,
  nombre                             VARCHAR(120) NOT NULL,
  emisor                             VARCHAR(120) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  nivel_riesgo                       VARCHAR(10) NOT NULL,
  estado                             VARCHAR(12) NOT NULL,
  creado_en                          TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_producto_inversion PRIMARY KEY (id),
  CONSTRAINT ck_producto_inversion_tipo CHECK (tipo IN ('DPF', 'FONDO')),
  CONSTRAINT ck_producto_inversion_nivel_riesgo CHECK (nivel_riesgo IN ('BAJO', 'MEDIO', 'ALTO')),
  CONSTRAINT ck_producto_inversion_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO'))
);

COMMENT ON TABLE inversiones.producto_inversion IS 'Módulo 15 — Inversiones voluntarias. Un producto que ofrece un aliado: un depósito a plazo fijo (DPF) o un fondo (FONDO), con su emisor, su moneda y su nivel de riesgo.';
COMMENT ON COLUMN inversiones.producto_inversion.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN inversiones.producto_inversion.codigo IS 'UQ';
COMMENT ON COLUMN inversiones.producto_inversion.tipo IS 'CK: DPF | FONDO, IDX';
COMMENT ON COLUMN inversiones.producto_inversion.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN inversiones.producto_inversion.nivel_riesgo IS 'CK: BAJO | MEDIO | ALTO';
COMMENT ON COLUMN inversiones.producto_inversion.estado IS 'CK: ACTIVO | SUSPENDIDO, IDX';
