-- evidencia_medida_cuenta · módulo 01 — Identidad, Usuarios y Seguridad
-- clase de dominio: EvidenciaMedidaCuenta
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS identidad.evidencia_medida_cuenta (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  expediente_medida_cuenta_id        UUID NOT NULL,
  tipo                               VARCHAR(25) NOT NULL,
  contenido                          TEXT NOT NULL,
  referencia_id                      UUID,
  aportada_por                       UUID NOT NULL,
  registrada_en                      TIMESTAMPTZ DEFAULT now() NOT NULL,
  CONSTRAINT pk_evidencia_medida_cuenta PRIMARY KEY (id),
  CONSTRAINT ck_evidencia_medida_cuenta_tipo CHECK (tipo IN ('DESCARGO', 'EVIDENCIA_AFECTADO', 'EVIDENCIA_OPERADOR', 'EVIDENCIA_SISTEMA'))
);

COMMENT ON TABLE identidad.evidencia_medida_cuenta IS 'Módulo 01 — Identidad, Usuarios y Seguridad. [append-only] Cada evidencia del expediente y cada descargo del afectado, una fila por aporte.';
COMMENT ON COLUMN identidad.evidencia_medida_cuenta.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN identidad.evidencia_medida_cuenta.expediente_medida_cuenta_id IS 'FK, IDX';
COMMENT ON COLUMN identidad.evidencia_medida_cuenta.tipo IS 'evidencia del sistema, del operador, del afectado, o el descargo.';
COMMENT ON COLUMN identidad.evidencia_medida_cuenta.referencia_id IS 'NULL';
COMMENT ON COLUMN identidad.evidencia_medida_cuenta.aportada_por IS 'quién la incorporó.';
