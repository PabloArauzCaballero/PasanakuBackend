-- Totales de control del mayor y del outbox, una linea `clave=valor` por dato.
--
-- Se toma en el MOMENTO del respaldo y se guarda FUERA de la base: es el contrato contra
-- el que se concilia lo restaurado. Un manifiesto que viviera dentro del dump se
-- restauraria igual de roto que lo que debia vigilar.
--
-- Uso:  psql -tA -f manifiesto.sql   (solo lectura)

SELECT 'asientos='        || count(*)                                  FROM nucleo_financiero.asiento_contable;
SELECT 'movimientos='     || count(*)                                  FROM nucleo_financiero.movimiento_contable;
SELECT 'suma_debe='       || COALESCE(sum(debe), 0)::text              FROM nucleo_financiero.movimiento_contable;
SELECT 'suma_haber='      || COALESCE(sum(haber), 0)::text             FROM nucleo_financiero.movimiento_contable;
SELECT 'max_numero='      || COALESCE(max(numero), 0)                  FROM nucleo_financiero.asiento_contable;
-- Huella del libro en orden: si un solo asiento cambia de monto, de cuenta o de lugar, cambia.
SELECT 'huella_mayor='    || COALESCE(md5(string_agg(a.id::text || ':' || m.cuenta_id::text || ':' || m.debe::text || ':'
                                                    || m.haber::text, '|' ORDER BY a.numero, m.descripcion)), '-')
  FROM nucleo_financiero.asiento_contable a
  JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id = a.id;
SELECT 'eventos='         || count(*)                                  FROM nucleo_financiero.evento_dominio;
SELECT 'ev_pendientes='   || count(*)                                  FROM nucleo_financiero.evento_dominio WHERE estado = 'PENDIENTE';
SELECT 'ev_tomados='      || count(*)                                  FROM nucleo_financiero.evento_dominio WHERE estado = 'TOMADO';
SELECT 'ev_publicados='   || count(*)                                  FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO';
SELECT 'consumidos_erp='  || count(*)                                  FROM erp.evento_consumido WHERE consumidor = 'contabilidad-erp';
SELECT 'tablas_con_rls='  || count(*)                                  FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                                                                       WHERE c.relkind IN ('r','p') AND c.relrowsecurity AND n.nspname NOT LIKE 'pg\_%';
SELECT 'tablas='          || count(*)                                  FROM information_schema.tables
                                                                       WHERE table_schema NOT IN ('pg_catalog','information_schema');
