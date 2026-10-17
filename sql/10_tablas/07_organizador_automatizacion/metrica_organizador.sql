-- metrica_organizador · módulo 07 — Organizador y Automatización
-- clase de dominio: MetricaOrganizador
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS organizador.metrica_organizador (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  evaluacion_id                      UUID NOT NULL,
  codigo                             VARCHAR(40) NOT NULL,
  valor                              NUMERIC(12,4) NOT NULL,
  meta                               NUMERIC(12,4) NOT NULL,
  cumple                             BOOLEAN DEFAULT FALSE NOT NULL,
  peso                               NUMERIC(4,3) NOT NULL,
  CONSTRAINT pk_metrica_organizador PRIMARY KEY (id)
);

COMMENT ON TABLE organizador.metrica_organizador IS 'Módulo 07 — Organizador y Automatización. El detalle de cada indicador dentro de la evaluación: valor, meta, si cumple y cuánto pesa.';
COMMENT ON COLUMN organizador.metrica_organizador.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN organizador.metrica_organizador.evaluacion_id IS 'FK, IDX';
