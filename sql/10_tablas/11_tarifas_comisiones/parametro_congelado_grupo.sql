-- parametro_congelado_grupo · módulo 11 — Tarifas, Comisiones, Impuestos y Facturación
-- clase de dominio: ParametroCongeladoGrupo
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS tarifas.parametro_congelado_grupo (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  parametro_id                       UUID NOT NULL,
  version_parametro_id               UUID NOT NULL,
  nivel_codigo                       VARCHAR(30),
  congelado_en                       TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_parametro_congelado_grupo PRIMARY KEY (id)
);

COMMENT ON TABLE tarifas.parametro_congelado_grupo IS 'Módulo 11 — Tarifas, Comisiones, Impuestos y Facturación. [append-only] La versión de cada parámetro (y de cada nivel) que un grupo congeló al iniciar.';
COMMENT ON COLUMN tarifas.parametro_congelado_grupo.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN tarifas.parametro_congelado_grupo.grupo_id IS 'IDX';
COMMENT ON COLUMN tarifas.parametro_congelado_grupo.parametro_id IS 'FK';
COMMENT ON COLUMN tarifas.parametro_congelado_grupo.version_parametro_id IS 'FK';
COMMENT ON COLUMN tarifas.parametro_congelado_grupo.nivel_codigo IS 'NULL';
