-- apelacion_medida_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: ApelacionMedidaCuenta
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.apelacion_medida_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  expediente_medida_cuenta_id        UUID NOT NULL,
  apelante_id                        UUID NOT NULL,
  argumento                          TEXT NOT NULL,
  estado                             VARCHAR(25) NOT NULL,
  presentada_en                      TIMESTAMPTZ NOT NULL,
  fecha_limite_resolucion            TIMESTAMPTZ,
  resuelta_por                       UUID,
  resuelta_en                        TIMESTAMPTZ,
  resolucion                         TEXT,
  CONSTRAINT pk_apelacion_medida_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_apelacion_medida_cuenta_estado CHECK (estado IN ('ACEPTADA', 'ACEPTADA_POR_VENCIMIENTO', 'PRESENTADA', 'RECHAZADA'))
);

COMMENT ON TABLE identidad.apelacion_medida_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. La apelación única de un expediente, presentada por el afectado y resuelta por una persona que no propuso ni decidió la medida.';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.expediente_medida_cuenta_id IS 'FK, UQ';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.apelante_id IS 'FK';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.estado IS 'presentada, aceptada, rechazada, o aceptada por vencimiento del plazo de la instancia.';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.fecha_limite_resolucion IS 'NULL';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.resuelta_por IS 'tercera persona; la base impide que sea quien propuso o decidió.';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.resuelta_en IS 'NULL';
COMMENT ON COLUMN identidad.apelacion_medida_cuenta.resolucion IS 'NULL';
