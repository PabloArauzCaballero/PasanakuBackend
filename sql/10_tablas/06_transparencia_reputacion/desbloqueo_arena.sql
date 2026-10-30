-- desbloqueo_arena · módulo 06 — Transparencia y Reputación
-- clase de dominio: DesbloqueoArena
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS transparencia.desbloqueo_arena (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  arena_id                           UUID NOT NULL,
  cupos_max                          SMALLINT NOT NULL,
  aporte_max                         NUMERIC(14,2) NOT NULL,
  moneda                             CHAR(3) NOT NULL,
  estado                             VARCHAR(10) NOT NULL,
  CONSTRAINT pk_desbloqueo_arena PRIMARY KEY (id),
  CONSTRAINT ck_desbloqueo_arena_cupos_max CHECK (cupos_max BETWEEN 3 AND 40),
  CONSTRAINT ck_desbloqueo_arena_aporte_max CHECK (aporte_max > 0),
  CONSTRAINT ck_desbloqueo_arena_moneda CHECK (moneda IN ('BOB', 'USD')),
  CONSTRAINT ck_desbloqueo_arena_estado CHECK (estado IN ('PROPUESTO', 'VIGENTE'))
);

COMMENT ON TABLE transparencia.desbloqueo_arena IS 'Módulo 06 — Transparencia y Reputación. Lo que desbloquea una arena (a lo sumo uno por arena): cuántos cupos totales puede tener como máximo un grupo y cuánto aporte por periodo como máximo, con su moneda, puede crear o integrar quien está en esa arena. Lleva un estado: solo el VIGENTE limita.';
COMMENT ON COLUMN transparencia.desbloqueo_arena.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN transparencia.desbloqueo_arena.arena_id IS 'FK, UQ';
COMMENT ON COLUMN transparencia.desbloqueo_arena.cupos_max IS 'Cupos totales máximos del grupo que puede crear o integrar quien está en la arena.';
COMMENT ON COLUMN transparencia.desbloqueo_arena.aporte_max IS 'Aporte máximo por periodo, en moneda, del grupo que puede crear o integrar quien está en la arena.';
COMMENT ON COLUMN transparencia.desbloqueo_arena.moneda IS 'Moneda ISO-4217 del importe de la fila: ningún importe existe sin su moneda (regla 91.1.2).';
COMMENT ON COLUMN transparencia.desbloqueo_arena.estado IS 'PROPUESTO se muestra pero no limita; solo VIGENTE se aplica.';
