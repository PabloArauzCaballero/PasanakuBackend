-- politica_cobertura · módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones
-- clase de dominio: PoliticaCobertura
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS garantia.politica_cobertura (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  grupo_id                           UUID,
  porcentaje_constitucion            NUMERIC(5,2) NOT NULL,
  dias_mora_para_activar             SMALLINT NOT NULL,
  porcentaje_maximo_cobertura_por_aporte NUMERIC(5,2) NOT NULL,
  tope_cobertura_por_participante    NUMERIC(14,2) NOT NULL,
  tope_cobertura_por_periodo         NUMERIC(14,2) NOT NULL,
  max_coberturas_por_participante    SMALLINT NOT NULL,
  exige_aval_previo                  BOOLEAN DEFAULT FALSE NOT NULL,
  requiere_aprobacion_manual_desde   NUMERIC(14,2) NOT NULL,
  plazo_recuperacion_dias            SMALLINT NOT NULL,
  tasa_recargo_recuperacion          NUMERIC(5,2) NOT NULL,
  vigente_desde                      TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_politica_cobertura PRIMARY KEY (id)
);

COMMENT ON TABLE garantia.politica_cobertura IS 'Módulo 08 — Garantía, Incumplimiento, Cobranza y Sanciones. Las reglas del fondo: cuánto se aporta, cuándo se activa la cobertura, cuánto cubre y con qué límites.';
COMMENT ON COLUMN garantia.politica_cobertura.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN garantia.politica_cobertura.grupo_id IS 'FK, NULL';
COMMENT ON COLUMN garantia.politica_cobertura.porcentaje_constitucion IS 'cuánto de cada aporte alimenta el fondo. Sube el costo del pasanaku, así que es un equilibrio entre seguridad y precio.';
COMMENT ON COLUMN garantia.politica_cobertura.dias_mora_para_activar IS 'no se cubre al primer día de atraso. Si se cubriera de inmediato, el fondo pagaría por gente que simplemente se olvidó y va a pagar mañana, y se agotaría en meses.';
COMMENT ON COLUMN garantia.politica_cobertura.tope_cobertura_por_participante IS 'evitan el parásito. Sin límite, alguien podría no pagar nunca y dejar que el fondo cubra siempre — convirtiendo el seguro colectivo en su financiamiento personal.';
COMMENT ON COLUMN garantia.politica_cobertura.tope_cobertura_por_periodo IS 'protege contra el colapso masivo. Si en un período fallan seis de doce, el fondo no puede vaciarse cubriéndolos a todos; ahí corresponde un plan de contingencia, no cobertura.';
COMMENT ON COLUMN garantia.politica_cobertura.max_coberturas_por_participante IS 'evitan el parásito. Sin límite, alguien podría no pagar nunca y dejar que el fondo cubra siempre — convirtiendo el seguro colectivo en su financiamiento personal.';
COMMENT ON COLUMN garantia.politica_cobertura.exige_aval_previo IS 'solo se cubre a quien tiene quién responda.';
COMMENT ON COLUMN garantia.politica_cobertura.requiere_aprobacion_manual_desde IS 'montos grandes pasan por revisión humana.';
COMMENT ON COLUMN garantia.politica_cobertura.plazo_recuperacion_dias IS 'el fondo no es un regalo. Es un préstamo forzoso con plazo y con costo.';
COMMENT ON COLUMN garantia.politica_cobertura.tasa_recargo_recuperacion IS 'el fondo no es un regalo. Es un préstamo forzoso con plazo y con costo.';
COMMENT ON COLUMN garantia.politica_cobertura.vigente_desde IS 'Desde cuándo rige esta versión de la fila: las condiciones se versionan, no se pisan.';
