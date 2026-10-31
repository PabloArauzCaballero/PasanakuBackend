-- subasta_cupo · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.subasta_cupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  cupo_id                            UUID NOT NULL,
  participante_origen_id             UUID NOT NULL,
  vendedor_usuario_id                UUID NOT NULL,
  nombre_grupo                       VARCHAR(120) NOT NULL,
  periodicidad                       VARCHAR(15) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  monto_aporte                       NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_base                         NUMERIC(14,2) DEFAULT 0 NOT NULL,
  monto_ganador                      NUMERIC(14,2),
  estado                             VARCHAR(12) NOT NULL,
  abierta_en                         TIMESTAMPTZ DEFAULT now() NOT NULL,
  cierra_en                          TIMESTAMPTZ NOT NULL,
  puja_ganadora_id                   UUID,
  traspaso_ref                       UUID,
  liquidacion_ref                    UUID,
  motivo_fallo                       VARCHAR(200),
  clave_idempotencia                 VARCHAR(80) NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_subasta_cupo PRIMARY KEY (id),
  CONSTRAINT ck_subasta_cupo_monto_aporte CHECK (monto_aporte > 0),
  CONSTRAINT ck_subasta_cupo_monto_base CHECK (monto_base > 0),
  CONSTRAINT ck_subasta_cupo_estado CHECK (estado IN ('ABIERTA', 'ADJUDICADA', 'LIQUIDANDO', 'VENDIDA', 'DESIERTA', 'CANCELADA', 'FALLIDA'))
);

COMMENT ON TABLE entregas.subasta_cupo IS 'Módulo 04 — Entregas de Fondo. El cupo que su titular pone en subasta: precio base, plazo de cierre y el estado de la venta, de ABIERTA hasta VENDIDA, DESIERTA, CANCELADA o FALLIDA.';
COMMENT ON COLUMN entregas.subasta_cupo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN entregas.subasta_cupo.grupo_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.subasta_cupo.cupo_id IS 'FK, IDX';
COMMENT ON COLUMN entregas.subasta_cupo.participante_origen_id IS 'FK';
COMMENT ON COLUMN entregas.subasta_cupo.vendedor_usuario_id IS 'IDX';
COMMENT ON COLUMN entregas.subasta_cupo.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN entregas.subasta_cupo.monto_aporte IS 'CK: > 0';
COMMENT ON COLUMN entregas.subasta_cupo.monto_base IS 'CK: > 0';
COMMENT ON COLUMN entregas.subasta_cupo.monto_ganador IS 'NULL';
COMMENT ON COLUMN entregas.subasta_cupo.estado IS 'CK: ABIERTA|ADJUDICADA|LIQUIDANDO|VENDIDA|DESIERTA|CANCELADA|FALLIDA, IDX';
COMMENT ON COLUMN entregas.subasta_cupo.cierra_en IS 'IDX';
COMMENT ON COLUMN entregas.subasta_cupo.puja_ganadora_id IS 'NULL';
COMMENT ON COLUMN entregas.subasta_cupo.traspaso_ref IS 'NULL';
COMMENT ON COLUMN entregas.subasta_cupo.liquidacion_ref IS 'NULL';
COMMENT ON COLUMN entregas.subasta_cupo.motivo_fallo IS 'NULL';
COMMENT ON COLUMN entregas.subasta_cupo.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
