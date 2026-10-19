-- regla_entrega · módulo 04 — Entregas de Fondo
-- clase de dominio: ReglaEntrega
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.regla_entrega (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  codigo                             VARCHAR(40) NOT NULL,
  descripcion                        VARCHAR(200) NOT NULL,
  es_bloqueante                      BOOLEAN DEFAULT FALSE NOT NULL,
  permite_omision                    BOOLEAN DEFAULT FALSE NOT NULL,
  rol_que_puede_omitir               VARCHAR(30),
  orden                              SMALLINT NOT NULL,
  activa                             BOOLEAN DEFAULT FALSE NOT NULL,
  CONSTRAINT pk_regla_entrega PRIMARY KEY (id)
);

COMMENT ON TABLE entregas.regla_entrega IS 'Módulo 04 — Entregas de Fondo. El catálogo de controles previos, con su orden, si bloquean y quién puede omitirlos.';
COMMENT ON COLUMN entregas.regla_entrega.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN entregas.regla_entrega.codigo IS 'UQ';
COMMENT ON COLUMN entregas.regla_entrega.rol_que_puede_omitir IS 'NULL';
