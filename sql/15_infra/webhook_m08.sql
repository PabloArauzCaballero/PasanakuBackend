-- F1.L1.M08 · migración segura de webhook_pasarela y preparación mensual.
-- Generado por scripts/generar_ddl.py — no editar a mano.
-- aplicar.sql mantiene una sola transacción: las aserciones son el límite de seguridad.
-- No se purgan filas en esta migración; la retención se aplica en trabajo separado.

DO $m08$
DECLARE
  padre regclass;
  padre_particionado boolean;
  mes_actual date := date_trunc('month', now() AT TIME ZONE 'UTC')::date;
  mes date;
  desde date;
  hasta date;
  columna record;
  total_origen bigint;
  total_copia bigint;
BEGIN
  -- Bloquea escritores durante la copia y el cambio de tabla; evita un ascenso de lock en DROP.
  LOCK TABLE ONLY aportes.webhook_pasarela IN ACCESS EXCLUSIVE MODE;
  padre := 'aportes.webhook_pasarela'::regclass;
  SELECT EXISTS (SELECT 1 FROM pg_partitioned_table WHERE partrelid = padre)
    INTO padre_particionado;
  IF padre_particionado THEN
    -- Una ejecución posterior solo prepara el horizonte; no reescribe datos.
    desde := mes_actual;
    hasta := (desde + interval '23 months')::date;
    mes := desde;
    WHILE mes <= hasta LOOP
      EXECUTE format(
        'CREATE TABLE IF NOT EXISTS aportes.%I PARTITION OF aportes.webhook_pasarela FOR VALUES FROM (%L) TO (%L)',
        'webhook_pasarela_' || to_char(mes, 'YYYYMM'),
        mes::timestamp AT TIME ZONE 'UTC', (mes + interval '1 month') AT TIME ZONE 'UTC');
      EXECUTE format(
        'CREATE TABLE IF NOT EXISTS aportes.%I PARTITION OF aportes.historial_estado_webhook FOR VALUES FROM (%L) TO (%L)',
        'historial_estado_webhook_' || to_char(mes, 'YYYYMM'),
        mes::timestamp AT TIME ZONE 'UTC', (mes + interval '1 month') AT TIME ZONE 'UTC');
      mes := (mes + interval '1 month')::date;
    END LOOP;
    RETURN;
  END IF;
  IF (SELECT relkind FROM pg_class WHERE oid = padre) <> 'r' THEN
    RAISE EXCEPTION 'M08: webhook_pasarela no es una tabla ordinaria ni un padre particionado';
  END IF;
  IF to_regclass('aportes.webhook_pasarela_m08') IS NOT NULL THEN
    RAISE EXCEPTION 'M08: existe webhook_pasarela_m08 sin completar; revisar antes de reintentar';
  END IF;
  SELECT LEAST(mes_actual, date_trunc('month', min(recibido_en) AT TIME ZONE 'UTC')::date),
         GREATEST((mes_actual + interval '23 months')::date,
                  date_trunc('month', max(recibido_en) AT TIME ZONE 'UTC')::date)
    INTO desde, hasta FROM aportes.webhook_pasarela;
  CREATE TABLE aportes.webhook_pasarela_m08 (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    proveedor_id uuid NOT NULL,
    evento varchar(60) NOT NULL,
    payload_crudo jsonb NOT NULL,
    firma varchar(255) NOT NULL,
    firma_valida boolean NOT NULL DEFAULT false,
    recibido_en timestamptz NOT NULL DEFAULT now(),
    clave_idempotencia varchar(120) NOT NULL,
    CONSTRAINT pk_webhook_pasarela_m08 PRIMARY KEY (id, recibido_en)
  ) PARTITION BY RANGE (recibido_en);
  -- El DDL de tablas ya actualizó los comentarios del modelo en la tabla original.
  EXECUTE format('COMMENT ON TABLE aportes.webhook_pasarela_m08 IS %L', obj_description(padre, 'pg_class'));
  FOR columna IN
    SELECT origen.attname, col_description(padre, origen.attnum) AS comentario
      FROM pg_attribute origen
      JOIN pg_attribute destino USING (attname)
     WHERE origen.attrelid = padre AND origen.attnum > 0 AND NOT origen.attisdropped
       AND destino.attrelid = 'aportes.webhook_pasarela_m08'::regclass
       AND col_description(padre, origen.attnum) IS NOT NULL
  LOOP
    EXECUTE format('COMMENT ON COLUMN aportes.webhook_pasarela_m08.%I IS %L', columna.attname, columna.comentario);
  END LOOP;
  mes := desde;
  WHILE mes <= hasta LOOP
    EXECUTE format(
      'CREATE TABLE aportes.%I PARTITION OF aportes.webhook_pasarela_m08 FOR VALUES FROM (%L) TO (%L)',
      'webhook_pasarela_' || to_char(mes, 'YYYYMM'),
      mes::timestamp AT TIME ZONE 'UTC', (mes + interval '1 month') AT TIME ZONE 'UTC');
    -- El padre de historial ya nació desde el modelo; se extiende para cubrir cada fecha copiada.
    IF to_regclass('aportes.historial_estado_webhook_' || to_char(mes, 'YYYYMM')) IS NULL THEN
      EXECUTE format(
        'CREATE TABLE aportes.%I PARTITION OF aportes.historial_estado_webhook FOR VALUES FROM (%L) TO (%L)',
        'historial_estado_webhook_' || to_char(mes, 'YYYYMM'),
        mes::timestamp AT TIME ZONE 'UTC', (mes + interval '1 month') AT TIME ZONE 'UTC');
    END IF;
    mes := (mes + interval '1 month')::date;
  END LOOP;
  SELECT count(*) INTO total_origen FROM aportes.webhook_pasarela;
  INSERT INTO aportes.webhook_pasarela_m08
    (id, proveedor_id, evento, payload_crudo, firma, firma_valida, recibido_en, clave_idempotencia)
  SELECT id, proveedor_id, evento, payload_crudo, firma, firma_valida, recibido_en, clave_idempotencia
    FROM aportes.webhook_pasarela;
  INSERT INTO aportes.webhook_clave
    (proveedor_id, clave_idempotencia, webhook_id, webhook_recibido_en)
  SELECT proveedor_id, clave_idempotencia, id, recibido_en
    FROM aportes.webhook_pasarela
  ON CONFLICT (proveedor_id, clave_idempotencia) DO NOTHING;
  INSERT INTO aportes.historial_estado_webhook
    (id, webhook_id, webhook_recibido_en, estado_anterior, estado_nuevo, procesado_en,
     intentos_procesamiento, error_procesamiento, pago_id)
  SELECT gen_random_uuid(), id, recibido_en, NULL, estado, procesado_en,
         intentos_procesamiento, error_procesamiento, pago_id
    FROM aportes.webhook_pasarela
  ON CONFLICT DO NOTHING;
  SELECT count(*) INTO total_copia FROM aportes.webhook_pasarela_m08;
  IF total_copia <> total_origen OR EXISTS (
    SELECT 1
      FROM aportes.webhook_pasarela original
      FULL JOIN aportes.webhook_pasarela_m08 copia
        ON copia.id = original.id AND copia.recibido_en = original.recibido_en
     WHERE original.id IS NULL OR copia.id IS NULL
        OR ROW(original.proveedor_id, original.evento, original.payload_crudo, original.firma,
               original.firma_valida, original.recibido_en, original.clave_idempotencia)
           IS DISTINCT FROM ROW(copia.proveedor_id, copia.evento, copia.payload_crudo, copia.firma,
                               copia.firma_valida, copia.recibido_en, copia.clave_idempotencia)
  ) THEN
    RAISE EXCEPTION 'M08: la copia del webhook no coincide; se conserva íntegra la tabla de origen';
  END IF;
  IF (SELECT count(*) FROM aportes.webhook_clave) <> total_origen OR EXISTS (
    SELECT 1 FROM aportes.webhook_pasarela original
      LEFT JOIN aportes.webhook_clave clave
        ON clave.proveedor_id = original.proveedor_id
       AND clave.clave_idempotencia = original.clave_idempotencia
     WHERE clave.webhook_id IS DISTINCT FROM original.id
        OR clave.webhook_recibido_en IS DISTINCT FROM original.recibido_en
  ) THEN
    RAISE EXCEPTION 'M08: la deduplicación global no es equivalente; se conserva íntegra la tabla de origen';
  END IF;
  IF (SELECT count(*) FROM aportes.historial_estado_webhook WHERE estado_anterior IS NULL) <> total_origen OR EXISTS (
    SELECT 1 FROM aportes.webhook_pasarela original
      LEFT JOIN aportes.historial_estado_webhook estado
        ON estado.webhook_id = original.id
       AND estado.webhook_recibido_en = original.recibido_en
       AND estado.estado_anterior IS NULL
     WHERE estado.webhook_id IS NULL
        OR ROW(estado.estado_nuevo, estado.procesado_en, estado.intentos_procesamiento,
               estado.error_procesamiento, estado.pago_id)
           IS DISTINCT FROM ROW(original.estado, original.procesado_en, original.intentos_procesamiento,
                               original.error_procesamiento, original.pago_id)
  ) THEN
    RAISE EXCEPTION 'M08: el historial inicial no coincide con el estado actual; se conserva íntegra la tabla de origen';
  END IF;
  -- La tabla vieja solo se suelta después de comprobar raw, deduplicación e historial exactos.
  DROP TABLE aportes.webhook_pasarela;
  ALTER TABLE aportes.webhook_pasarela_m08 RENAME TO webhook_pasarela;
  ALTER TABLE aportes.webhook_pasarela RENAME CONSTRAINT pk_webhook_pasarela_m08 TO pk_webhook_pasarela;
END $m08$;
