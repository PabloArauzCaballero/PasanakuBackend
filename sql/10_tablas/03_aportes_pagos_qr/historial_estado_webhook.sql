-- historial_estado_webhook · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: HistorialEstadoWebhook
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- PARTICIONADA por rango de webhook_recibido_en (mensual)
-- Generado por scripts/generar_ddl.py — no editar a mano.

CREATE TABLE IF NOT EXISTS aportes.historial_estado_webhook (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  webhook_id                         UUID NOT NULL,
  webhook_recibido_en                TIMESTAMPTZ NOT NULL,
  estado_anterior                    VARCHAR(15),
  estado_nuevo                       VARCHAR(15) NOT NULL,
  procesado_en                       TIMESTAMPTZ,
  intentos_procesamiento             SMALLINT DEFAULT 0 NOT NULL,
  error_procesamiento                TEXT,
  pago_id                            UUID,
  CONSTRAINT pk_historial_estado_webhook PRIMARY KEY (id, webhook_recibido_en),
  CONSTRAINT ck_historial_estado_webhook_estado_anterior CHECK (estado_anterior IN ('DESCARTADO', 'DUPLICADO', 'FALLIDO', 'PROCESADO', 'RECIBIDO')),
  CONSTRAINT ck_historial_estado_webhook_estado_nuevo CHECK (estado_nuevo IN ('DESCARTADO', 'DUPLICADO', 'FALLIDO', 'PROCESADO', 'RECIBIDO'))
) PARTITION BY RANGE (webhook_recibido_en);

DO $$
DECLARE d DATE := date_trunc('month', current_date)::date;
BEGIN
  IF EXISTS (SELECT 1 FROM pg_partitioned_table WHERE partrelid = to_regclass('aportes.historial_estado_webhook')) THEN
    FOR i IN 0..23 LOOP
      EXECUTE format(
      'CREATE TABLE IF NOT EXISTS aportes.historial_estado_webhook_%s PARTITION OF aportes.historial_estado_webhook FOR VALUES FROM (%L) TO (%L)',
        to_char(d + (i || ' month')::interval, 'YYYYMM'),
        d + (i || ' month')::interval,
        d + ((i + 1) || ' month')::interval);
    END LOOP;
  END IF;
END $$;

COMMENT ON TABLE aportes.historial_estado_webhook IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Que "pagué" signifique "el banco lo confirmó"';
COMMENT ON COLUMN aportes.historial_estado_webhook.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.historial_estado_webhook.webhook_id IS 'IDX';
COMMENT ON COLUMN aportes.historial_estado_webhook.webhook_recibido_en IS 'IDX';
COMMENT ON COLUMN aportes.historial_estado_webhook.estado_anterior IS 'NULL, CK, UQ+webhook_id+webhook_recibido_en';
COMMENT ON COLUMN aportes.historial_estado_webhook.estado_nuevo IS 'CK, IDX';
COMMENT ON COLUMN aportes.historial_estado_webhook.procesado_en IS 'NULL';
COMMENT ON COLUMN aportes.historial_estado_webhook.error_procesamiento IS 'NULL';
COMMENT ON COLUMN aportes.historial_estado_webhook.pago_id IS 'FK, NULL';
