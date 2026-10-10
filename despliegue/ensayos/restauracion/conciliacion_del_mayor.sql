-- Conciliacion del mayor ANTES de reabrir. Solo lectura. Una fila por comprobacion:
--     id | ok (t/f) | detalle
-- Si una sola es `f`, el sistema NO se reabre (el guion de ensayo sale con codigo 1).
--
-- Uso:  psql -tA -F'|' -v m_asientos=... -v m_suma_debe=... -v m_huella=... \
--            -v m_max_numero=... -v m_eventos=... -v m_pendientes=... -v m_tomados=... \
--            -v m_publicados=... -v m_consumidos=... -v m_rls=... -v m_tablas=... \
--            -f conciliacion_del_mayor.sql
--
-- Los `m_*` salen de manifiesto.sql, tomado en el momento del respaldo y guardado fuera
-- de la base. Las comprobaciones C01-C04 y C08-C12 no necesitan manifiesto: son leyes
-- del libro (R-AUD-05, R-AUD-12, R-CTB, R-AUD-01) y valen sobre cualquier punto de
-- recuperacion. Las C05-C07 y C13-C17 comparan contra lo que se declaro al respaldar.

WITH por_asiento AS (
    SELECT asiento_id, moneda, sum(debe) AS d, sum(haber) AS h
      FROM nucleo_financiero.movimiento_contable GROUP BY asiento_id, moneda
), saldos AS (
    SELECT c.id, c.saldo,
           COALESCE(sum(CASE WHEN c.naturaleza = 'DEUDORA' THEN m.debe - m.haber ELSE m.haber - m.debe END), 0) AS recalculado
      FROM nucleo_financiero.cuenta_contable c
      LEFT JOIN nucleo_financiero.movimiento_contable m ON m.cuenta_id = c.id
     GROUP BY c.id, c.saldo
), trig AS (
    SELECT tgname FROM pg_trigger WHERE NOT tgisinternal AND tgenabled <> 'D'
)
SELECT 'C01 cada asiento cuadra, por moneda',
       NOT EXISTS (SELECT 1 FROM por_asiento WHERE d <> h),
       (SELECT count(*) FROM por_asiento WHERE d <> h) || ' asientos descuadrados'
UNION ALL SELECT 'C02 debe total = haber total',
       (SELECT COALESCE(sum(debe), 0) = COALESCE(sum(haber), 0) FROM nucleo_financiero.movimiento_contable),
       (SELECT COALESCE(sum(debe), 0) - COALESCE(sum(haber), 0) FROM nucleo_financiero.movimiento_contable) || ' de diferencia'
UNION ALL SELECT 'C03 ningun asiento en firme sin movimientos',
       NOT EXISTS (SELECT 1 FROM nucleo_financiero.asiento_contable a
                    WHERE a.estado IN ('CONFIRMADO','REVERSADO')
                      AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.movimiento_contable m WHERE m.asiento_id = a.id)),
       'asientos vacios: ' || (SELECT count(*) FROM nucleo_financiero.asiento_contable a
                                WHERE a.estado IN ('CONFIRMADO','REVERSADO')
                                  AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.movimiento_contable m WHERE m.asiento_id = a.id))
UNION ALL SELECT 'C04 ningun movimiento huerfano',
       NOT EXISTS (SELECT 1 FROM nucleo_financiero.movimiento_contable m
                    WHERE NOT EXISTS (SELECT 1 FROM nucleo_financiero.asiento_contable a WHERE a.id = m.asiento_id)),
       'huerfanos: ' || (SELECT count(*) FROM nucleo_financiero.movimiento_contable m
                          WHERE NOT EXISTS (SELECT 1 FROM nucleo_financiero.asiento_contable a WHERE a.id = m.asiento_id))
UNION ALL SELECT 'C05 asientos = manifiesto',
       (SELECT count(*) FROM nucleo_financiero.asiento_contable) = :m_asientos,
       (SELECT count(*) FROM nucleo_financiero.asiento_contable) || ' vs manifiesto ' || :m_asientos
UNION ALL SELECT 'C06 suma del debe = manifiesto',
       (SELECT COALESCE(sum(debe), 0) FROM nucleo_financiero.movimiento_contable) = :m_suma_debe::numeric,
       (SELECT COALESCE(sum(debe), 0) FROM nucleo_financiero.movimiento_contable) || ' vs manifiesto ' || :m_suma_debe
UNION ALL SELECT 'C07 huella del libro = manifiesto',
       (SELECT COALESCE(md5(string_agg(a.id::text || ':' || m.cuenta_id::text || ':' || m.debe::text || ':' || m.haber::text,
                                       '|' ORDER BY a.numero, m.descripcion)), '-')
          FROM nucleo_financiero.asiento_contable a JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id = a.id) = :'m_huella',
       'esperada ' || :'m_huella'
UNION ALL SELECT 'C08 saldo almacenado = saldo recalculado desde el mayor',
       NOT EXISTS (SELECT 1 FROM saldos WHERE saldo <> recalculado),
       'cuentas con saldo distinto: ' || (SELECT count(*) FROM saldos WHERE saldo <> recalculado)
UNION ALL SELECT 'C09 la secuencia de asientos no retrocede',
       COALESCE((SELECT last_value FROM pg_sequences WHERE schemaname = 'nucleo_financiero' AND sequencename = 'asiento_contable_numero_seq'), 0)
           >= COALESCE((SELECT max(numero) FROM nucleo_financiero.asiento_contable), 0),
       'last_value ' || COALESCE((SELECT last_value::text FROM pg_sequences WHERE schemaname = 'nucleo_financiero' AND sequencename = 'asiento_contable_numero_seq'), 'NULL')
           || ' / max(numero) ' || COALESCE((SELECT max(numero)::text FROM nucleo_financiero.asiento_contable), '0')
UNION ALL SELECT 'C10 numeros de asiento sin repetir y max = manifiesto',
       (SELECT count(DISTINCT numero) = count(*) AND COALESCE(max(numero), 0) = :m_max_numero FROM nucleo_financiero.asiento_contable),
       'max ' || COALESCE((SELECT max(numero)::text FROM nucleo_financiero.asiento_contable), '0') || ' vs manifiesto ' || :m_max_numero
UNION ALL SELECT 'C11 append-only vigente: trigger puesto y sin UPDATE/DELETE para el servicio',
       EXISTS (SELECT 1 FROM trig WHERE tgname = 'tg_asiento_contable_append_only')
         AND NOT has_table_privilege('svc_nucleo_financiero', 'nucleo_financiero.asiento_contable', 'UPDATE')
         AND NOT has_table_privilege('svc_nucleo_financiero', 'nucleo_financiero.asiento_contable', 'DELETE'),
       'trigger y privilegios restaurados'
UNION ALL SELECT 'C12 cuadre diferido y moneda unica siguen en la base',
       EXISTS (SELECT 1 FROM trig WHERE tgname = 'tg_asiento_cuadrado')
         AND EXISTS (SELECT 1 FROM trig WHERE tgname = 'tg_asiento_una_moneda'),
       'constraint triggers R-AUD-05 y R-AUD-12'
UNION ALL SELECT 'C13 politicas de fila y tablas = manifiesto',
       (SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
         WHERE c.relkind IN ('r','p') AND c.relrowsecurity AND n.nspname NOT LIKE 'pg\_%') = :m_rls
         AND (SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema')) = :m_tablas,
       'rls esperadas ' || :m_rls || ', tablas esperadas ' || :m_tablas
UNION ALL SELECT 'C14 la base conserva su search_path (ALTER DATABASE no viaja en el dump)',
       EXISTS (SELECT 1 FROM pg_db_role_setting s JOIN pg_database d ON d.oid = s.setdatabase
                WHERE d.datname = current_database() AND s.setrole = 0
                  AND array_to_string(s.setconfig, ',') LIKE '%search_path=%nucleo_financiero%'),
       'search_path de la base'
UNION ALL SELECT 'C15 outbox coherente con el mayor (un evento por asiento, ninguno huerfano)',
       NOT EXISTS (SELECT 1 FROM nucleo_financiero.evento_dominio e
                    WHERE e.tipo = 'nucleo_financiero.asiento_registrado'
                      AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.asiento_contable a WHERE a.id = e.agregado_id))
         AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.asiento_contable a
                          WHERE NOT EXISTS (SELECT 1 FROM nucleo_financiero.evento_dominio e WHERE e.agregado_id = a.id)),
       'eventos sin asiento o asientos sin evento'
UNION ALL SELECT 'C16 todo lo consumido salio publicado',
       NOT EXISTS (SELECT 1 FROM erp.evento_consumido c
                    WHERE c.consumidor = 'contabilidad-erp'
                      AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.evento_dominio e WHERE e.id = c.id_evento AND e.estado = 'PUBLICADO')),
       'consumidos sin publicar: ' || (SELECT count(*) FROM erp.evento_consumido c
                                         WHERE c.consumidor = 'contabilidad-erp'
                                           AND NOT EXISTS (SELECT 1 FROM nucleo_financiero.evento_dominio e WHERE e.id = c.id_evento AND e.estado = 'PUBLICADO'))
UNION ALL SELECT 'C17 cola del outbox = manifiesto (pendientes, tomados huerfanos, publicados, consumidos)',
       (SELECT count(*) FROM nucleo_financiero.evento_dominio) = :m_eventos
         AND (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'PENDIENTE') = :m_pendientes
         AND (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'TOMADO') = :m_tomados
         AND (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO') = :m_publicados
         AND (SELECT count(*) FROM erp.evento_consumido WHERE consumidor = 'contabilidad-erp') = :m_consumidos,
       'cola restaurada tal como se respaldo';
