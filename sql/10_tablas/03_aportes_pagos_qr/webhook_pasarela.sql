-- webhook_pasarela · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: WebhookPasarela
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- PARTICIONADA por rango de recibido_en (mensual)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.webhook_pasarela (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  proveedor_id                       UUID NOT NULL,
  evento                             VARCHAR(60) NOT NULL,
  payload_crudo                      JSONB NOT NULL,
  firma                              VARCHAR(255) NOT NULL,
  firma_valida                       BOOLEAN DEFAULT FALSE NOT NULL,
  recibido_en                        TIMESTAMPTZ DEFAULT now() NOT NULL,
  clave_idempotencia                 VARCHAR(120) NOT NULL,
  CONSTRAINT pk_webhook_pasarela PRIMARY KEY (id, recibido_en)
) PARTITION BY RANGE (recibido_en);

DO $$
DECLARE d DATE := date_trunc('month', current_date)::date;
BEGIN
  IF EXISTS (SELECT 1 FROM pg_partitioned_table WHERE partrelid = to_regclass('aportes.webhook_pasarela')) THEN
    FOR i IN 0..23 LOOP
      EXECUTE format(
      'CREATE TABLE IF NOT EXISTS aportes.webhook_pasarela_%s PARTITION OF aportes.webhook_pasarela FOR VALUES FROM (%L) TO (%L)',
        to_char(d + (i || ' month')::interval, 'YYYYMM'),
        d + (i || ' month')::interval,
        d + ((i + 1) || ' month')::interval);
    END LOOP;
  END IF;
END $$;

COMMENT ON TABLE aportes.webhook_pasarela IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Cada notificación que la pasarela envía al sistema, guardada en crudo antes de procesarla.';
COMMENT ON COLUMN aportes.webhook_pasarela.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.webhook_pasarela.proveedor_id IS 'FK, IDX';
COMMENT ON COLUMN aportes.webhook_pasarela.recibido_en IS 'IDX';
COMMENT ON COLUMN aportes.webhook_pasarela.clave_idempotencia IS 'Clave que vuelve idempotente la operación: un reintento con la misma clave no repite el efecto (regla 91.1.4).';
