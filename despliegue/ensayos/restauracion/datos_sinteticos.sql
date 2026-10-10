-- Datos SINTETICOS para el ensayo de restauracion (H13.S1.M4). Ninguno es dato real.
--
-- Uso:  psql -v ON_ERROR_STOP=1 -v lote=base -v cuantos=400 -f datos_sinteticos.sql
--
-- Los identificadores salen de md5(lote, i): el MISMO lote produce los MISMOS ids. Eso es
-- lo que permite, tras restaurar, reponer lo perdido desde la fuente externa y comprobar
-- que el mayor queda identico al del origen (huella de igualdad), sin inventar nada.
--
-- Lo que escribe, y solo por las vias que escribiria el servicio:
--   * cuatro cuentas del mayor (dos BOB, dos USD) y `cuantos` asientos de dos lineas;
--     cada decima asiento es en USD, para que el cuadre por moneda tenga algo que mirar;
--   * un evento del outbox por asiento. En el lote `base` los ultimos 10 quedan sin
--     publicar (8 PENDIENTE y 2 TOMADO por un relevo muerto): es el estado en que un
--     respaldo real encuentra una cola viva;
--   * en `erp.evento_consumido`, la marca de que el consumidor ya aplico los publicados.

BEGIN;

INSERT INTO nucleo_financiero.cuenta_contable
       (id, codigo, nombre, tipo, naturaleza, moneda, nivel, es_cuenta_de_movimiento, saldo)
VALUES ('5e000000-0000-4000-8000-000000000001', 'S2-1.1.01', 'Caja de custodia (sintetica)',  'ACTIVO', 'DEUDORA',   'BOB', 1, true, 0),
       ('5e000000-0000-4000-8000-000000000002', 'S2-2.1.01', 'Fondos de participantes (sint)', 'PASIVO', 'ACREEDORA', 'BOB', 1, true, 0),
       ('5e000000-0000-4000-8000-000000000003', 'S2-1.1.02', 'Caja USD (sintetica)',           'ACTIVO', 'DEUDORA',   'USD', 1, true, 0),
       ('5e000000-0000-4000-8000-000000000004', 'S2-2.1.02', 'Fondos USD (sintetica)',         'PASIVO', 'ACREEDORA', 'USD', 1, true, 0)
ON CONFLICT (id) DO NOTHING;

CREATE TEMP TABLE s2_lote ON COMMIT DROP AS
SELECT i,
       uuid_in(md5('s2:' || :'lote' || ':' || i)::cstring)        AS id,
       (i % 10 = 0)                                                AS en_usd,
       (((i * 37) % 900) + 100)::numeric(16,2)                     AS monto
  FROM generate_series(1, :cuantos) AS i;

INSERT INTO nucleo_financiero.asiento_contable (id, fecha, glosa, origen_tipo, origen_id, estado)
SELECT id, clock_timestamp(), 'S2 sintetico ' || :'lote' || ' ' || i, 'PAGO',
       uuid_in(md5('s2:origen:' || id)::cstring), 'CONFIRMADO'
  FROM s2_lote ORDER BY i;

INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, moneda, descripcion)
SELECT uuid_in(md5('s2:debe:' || id)::cstring), id,
       CASE WHEN en_usd THEN '5e000000-0000-4000-8000-000000000003'::uuid
            ELSE '5e000000-0000-4000-8000-000000000001'::uuid END,
       monto, 0, CASE WHEN en_usd THEN 'USD' ELSE 'BOB' END, 'S2 debe'
  FROM s2_lote
UNION ALL
SELECT uuid_in(md5('s2:haber:' || id)::cstring), id,
       CASE WHEN en_usd THEN '5e000000-0000-4000-8000-000000000004'::uuid
            ELSE '5e000000-0000-4000-8000-000000000002'::uuid END,
       0, monto, CASE WHEN en_usd THEN 'USD' ELSE 'BOB' END, 'S2 haber'
  FROM s2_lote;

-- Outbox. `base`: publicados salvo los ultimos 10. `posterior`: todo pendiente (nada salio).
INSERT INTO nucleo_financiero.evento_dominio
       (id, tipo, version, agregado, agregado_id, payload, correlation_id, ocurrido_en,
        publicado_en, estado, intentos, tomado_en, tomado_por)
SELECT uuid_in(md5('s2:evento:' || id)::cstring), 'nucleo_financiero.asiento_registrado', '1', 'asiento', id,
       jsonb_build_object('asientoId', id, 'monto', monto::text, 'moneda', CASE WHEN en_usd THEN 'USD' ELSE 'BOB' END),
       uuid_in(md5('s2:traza:' || id)::cstring), clock_timestamp(),
       CASE WHEN :'lote' = 'base' AND i <= :cuantos - 10 THEN clock_timestamp() END,
       CASE WHEN :'lote' = 'base' AND i <= :cuantos - 10 THEN 'PUBLICADO'
            WHEN :'lote' = 'base' AND i  > :cuantos - 2  THEN 'TOMADO'
            ELSE 'PENDIENTE' END,
       CASE WHEN :'lote' = 'base' AND i  > :cuantos - 2  THEN 1 ELSE 0 END,
       CASE WHEN :'lote' = 'base' AND i  > :cuantos - 2  THEN clock_timestamp() - interval '2 hours' END,
       CASE WHEN :'lote' = 'base' AND i  > :cuantos - 2  THEN 'relevo-que-murio' END
  FROM s2_lote ORDER BY i;

INSERT INTO erp.evento_consumido (id_evento, consumidor)
SELECT uuid_in(md5('s2:evento:' || id)::cstring), 'contabilidad-erp'
  FROM s2_lote
 WHERE :'lote' = 'base' AND i <= :cuantos - 10
ON CONFLICT DO NOTHING;

COMMIT;
