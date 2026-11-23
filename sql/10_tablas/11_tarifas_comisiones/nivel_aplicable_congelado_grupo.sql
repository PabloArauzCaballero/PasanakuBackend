-- nivel_aplicable_congelado_grupo · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.nivel_aplicable_congelado_grupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  nivel_codigo                       VARCHAR(30) NOT NULL,
  orden                              SMALLINT NOT NULL,
  congelado_en                       TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_nivel_aplicable_congelado_grupo PRIMARY KEY (id)
);

COMMENT ON TABLE tarifas.nivel_aplicable_congelado_grupo IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. [append-only] La política de cobro es dato, no código: se cambia con un seeder';
COMMENT ON COLUMN tarifas.nivel_aplicable_congelado_grupo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.nivel_aplicable_congelado_grupo.grupo_id IS 'IDX';
COMMENT ON COLUMN tarifas.nivel_aplicable_congelado_grupo.nivel_codigo IS 'UQ+grupo_id';
