-- turno_del_grupo · módulo 04 — Entregas de Fondo
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS entregas.turno_del_grupo (
  turno_id                           UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID NOT NULL,
  periodo_id                         UUID NOT NULL,
  cupo_id                            UUID NOT NULL,
  titular_participante_id            UUID NOT NULL,
  version                            INTEGER DEFAULT 0 NOT NULL,
  recibido_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_turno_del_grupo PRIMARY KEY (turno_id),
  CONSTRAINT ck_turno_del_grupo_version CHECK (version >= 0)
);

COMMENT ON TABLE entregas.turno_del_grupo IS 'Módulo 04 — Entregas de Fondo. La copia que guarda entregas de los turnos que publica grupos: qué turno cobra el pozo de cada periodo, de qué cupo y quién era el titular cuando grupos lo publicó.';
COMMENT ON COLUMN entregas.turno_del_grupo.turno_id IS 'PK';
COMMENT ON COLUMN entregas.turno_del_grupo.grupo_id IS 'IDX';
COMMENT ON COLUMN entregas.turno_del_grupo.periodo_id IS 'UQ';
COMMENT ON COLUMN entregas.turno_del_grupo.version IS 'CK: >= 0';
