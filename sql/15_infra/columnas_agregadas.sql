-- Columnas opcionales agregadas al modelo, sobre una base que ya existe.
-- Generado por scripts/generar_ddl.py — no editar a mano.
--
-- Solo agrega columnas NULL (COLUMNAS_AGREGADAS del generador): no reescribe
-- la tabla ni puede fallar por datos existentes. Sobre una base recien creada
-- no hace nada: la columna ya nace con la tabla.

ALTER TABLE aportes.reembolso ADD COLUMN IF NOT EXISTS clave_idempotencia VARCHAR(100);
COMMENT ON COLUMN aportes.reembolso.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
ALTER TABLE grupos.acuerdo ADD COLUMN IF NOT EXISTS entrante_participante_id UUID;
COMMENT ON COLUMN grupos.acuerdo.entrante_participante_id IS 'NULL';
