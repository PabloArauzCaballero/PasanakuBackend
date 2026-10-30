-- Columnas opcionales agregadas al modelo, sobre una base que ya existe.
-- Generado por scripts/generar_ddl.py — no editar a mano.
--
-- Solo agrega columnas NULL (COLUMNAS_AGREGADAS del generador): no reescribe
-- la tabla ni puede fallar por datos existentes. Sobre una base recien creada
-- no hace nada: la columna ya nace con la tabla.

ALTER TABLE grupos.acuerdo ADD COLUMN IF NOT EXISTS entrante_participante_id UUID;
COMMENT ON COLUMN grupos.acuerdo.entrante_participante_id IS 'NULL';
