-- historial_estado_webhook · módulo 03 — Aportes, Pagos QR y Conciliación
-- clase de dominio: HistorialEstadoWebhook
-- APPEND-ONLY: sin UPDATE ni DELETE (ver sql/40_reglas)
-- PARTICIONADA por rango de webhook_recibido_en (mensual)
-- Generado por scripts/generar_ddl.py — no editar a mano.

DO $$
BEGIN
  IF to_regclass('aportes.historial_estado_webhook') IS NOT NULL AND (SELECT c.relkind FROM pg_class c
      WHERE c.oid = to_regclass('aportes.historial_estado_webhook')) = 'r' THEN
    IF EXISTS (SELECT 1 FROM aportes.historial_estado_webhook) THEN
      RAISE EXCEPTION 'aportes.historial_estado_webhook tiene filas y no esta particionada: migrarla con respaldo antes de aplicar';
    END IF;
    DROP TABLE aportes.historial_estado_webhook CASCADE;
  END IF;
END $$;

CREATE TABLE IF NOT EXISTS aportes.historial_estado_webhook (
  id                                 UUID DEFAULT gen_random_uuid() NOT NULL,
  webhook_id                         UUID NOT NULL,
  webhook_recibido_en                TIMESTAMPTZ NOT NULL,
  estado_anterior                    VARCHAR(15) NOT NULL,
  estado_nuevo                       VARCHAR(15) NOT NULL,
  pago_id                            UUID,
  error_procesamiento                TEXT,
  fecha_hora                         TIMESTAMPTZ NOT NULL,
  CONSTRAINT pk_historial_estado_webhook PRIMARY KEY (id, webhook_recibido_en)
) PARTITION BY RANGE (webhook_recibido_en);

CREATE TABLE IF NOT EXISTS aportes.historial_estado_webhook_desborde
  PARTITION OF aportes.historial_estado_webhook DEFAULT;

DO $$
DECLARE d DATE := date_trunc('year', current_date)::date;
BEGIN
  FOR i IN 0..23 LOOP
    EXECUTE format(
      'CREATE TABLE IF NOT EXISTS aportes.historial_estado_webhook_%s PARTITION OF aportes.historial_estado_webhook FOR VALUES FROM (%L) TO (%L)',
      to_char(d + (i || ' month')::interval, 'YYYYMM'),
      d + (i || ' month')::interval,
      d + ((i + 1) || ' month')::interval);
  END LOOP;
END $$;

COMMENT ON TABLE aportes.historial_estado_webhook IS 'Módulo 03 — Aportes, Pagos QR y Conciliación. [append-only] Cada cambio de estado de un aviso de la pasarela, como una fila nueva: de RECIBIDO a PROCESADO, DESCARTADO o FALLIDO, con el pago que generó o el error, y cuándo.';
COMMENT ON COLUMN aportes.historial_estado_webhook.id IS 'Identificador estable de la fila (UUID). Es lo único que se loguea o viaja entre servicios.';
COMMENT ON COLUMN aportes.historial_estado_webhook.webhook_id IS 'IDX';
COMMENT ON COLUMN aportes.historial_estado_webhook.pago_id IS 'FK, NULL';
COMMENT ON COLUMN aportes.historial_estado_webhook.error_procesamiento IS 'NULL';
