-- dia_no_habil · módulo 02 — Grupos, Cupos, Turnos y Gobernanza
-- clase de dominio: DiaNoHabil
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS catalogo.dia_no_habil (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  fecha                              DATE NOT NULL,
  descripcion                        VARCHAR(120) NOT NULL,
  alcance                            VARCHAR(15) NOT NULL,
  grupo_id                           UUID,
  CONSTRAINT pk_dia_no_habil PRIMARY KEY (id),
  CONSTRAINT ck_dia_no_habil_alcance CHECK (alcance IN ('DEPARTAMENTAL', 'GRUPO', 'NACIONAL'))
);

COMMENT ON TABLE catalogo.dia_no_habil IS 'Módulo 02 — Grupos, Cupos, Turnos y Gobernanza. El calendario de feriados nacionales, departamentales o propios del grupo.';
COMMENT ON COLUMN catalogo.dia_no_habil.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN catalogo.dia_no_habil.fecha IS 'UQ+alcance+grupo_id';
COMMENT ON COLUMN catalogo.dia_no_habil.alcance IS 'CK';
COMMENT ON COLUMN catalogo.dia_no_habil.grupo_id IS 'FK, NULL';
