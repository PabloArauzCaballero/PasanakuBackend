-- Reanudacion de los eventos tras restaurar. Se corre DESPUES de que la conciliacion del
-- mayor dio verde, nunca antes: publicar sobre un libro sin conciliar propaga el dano.
--
-- Emula, en SQL, lo que hace el relevo del servicio (ADR-018, plataforma/comun-mensajeria):
-- tomar lo PENDIENTE o lo TOMADO por un relevo muerto, publicar, marcar. Y lo que hace el
-- consumidor (ADR-027): aplicar UNA vez por (id_evento, consumidor), aunque la entrega sea
-- al-menos-una-vez.
--
-- Uso:  psql -tA -F'|' -f reanudacion_de_eventos.sql      Salida:  id | ok (t/f) | detalle

CREATE TEMP TABLE r_antes ON COMMIT PRESERVE ROWS AS
SELECT (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'PENDIENTE') AS pendientes,
       (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'TOMADO')    AS tomados,
       (SELECT count(*) FROM erp.evento_consumido WHERE consumidor = 'contabilidad-erp')  AS consumidos;

-- 1. Tomar (con el huerfano), publicar y marcar. SKIP LOCKED: dos relevos no pisan la fila.
CREATE TEMP TABLE r_publicados ON COMMIT PRESERVE ROWS AS
WITH tomados AS (
    SELECT id FROM nucleo_financiero.evento_dominio
     WHERE estado = 'PENDIENTE'
        OR (estado = 'TOMADO' AND tomado_en < now() - interval '15 minutes')
     ORDER BY ocurrido_en
       FOR UPDATE SKIP LOCKED
), marcado AS (
    UPDATE nucleo_financiero.evento_dominio e
       SET estado = 'PUBLICADO', publicado_en = now(), intentos = e.intentos + 1,
           tomado_en = NULL, tomado_por = NULL
      FROM tomados WHERE e.id = tomados.id
    RETURNING e.id
) SELECT id FROM marcado;

-- 2. El consumidor aplica lo que le llega. La clave primaria (id_evento, consumidor) es la dedupe.
CREATE TEMP TABLE r_aplicados ON COMMIT PRESERVE ROWS AS
WITH aplicado AS (
    INSERT INTO erp.evento_consumido (id_evento, consumidor)
    SELECT id, 'contabilidad-erp' FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO'
    ON CONFLICT DO NOTHING
    RETURNING id_evento
) SELECT id_evento FROM aplicado;

-- 3. Entrega duplicada: el broker vuelve a entregar TODO (at-least-once). No debe aplicar nada.
CREATE TEMP TABLE r_duplicados ON COMMIT PRESERVE ROWS AS
WITH aplicado AS (
    INSERT INTO erp.evento_consumido (id_evento, consumidor)
    SELECT id, 'contabilidad-erp' FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO'
    ON CONFLICT DO NOTHING
    RETURNING id_evento
) SELECT id_evento FROM aplicado;

SELECT 'R01 se publico exactamente lo que estaba pendiente o huerfano',
       (SELECT count(*) FROM r_publicados) = (SELECT pendientes + tomados FROM r_antes),
       (SELECT count(*) FROM r_publicados) || ' publicados de ' || (SELECT pendientes + tomados FROM r_antes) || ' en cola'
UNION ALL SELECT 'R02 la cola quedo vacia (nada PENDIENTE ni TOMADO)',
       NOT EXISTS (SELECT 1 FROM nucleo_financiero.evento_dominio WHERE estado IN ('PENDIENTE','TOMADO')),
       (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado IN ('PENDIENTE','TOMADO')) || ' sin publicar'
UNION ALL SELECT 'R03 el consumidor aplico solo lo que no tenia (no re-aplico lo ya consumido)',
       (SELECT count(*) FROM r_aplicados) = (SELECT count(*) FROM r_publicados),
       (SELECT count(*) FROM r_aplicados) || ' aplicados; ya tenia ' || (SELECT consumidos FROM r_antes)
UNION ALL SELECT 'R04 la entrega duplicada no aplico nada (idempotencia por clave primaria)',
       (SELECT count(*) FROM r_duplicados) = 0,
       (SELECT count(*) FROM r_duplicados) || ' efectos de la reentrega'
UNION ALL SELECT 'R05 todo publicado quedo consumido una sola vez',
       (SELECT count(*) FROM erp.evento_consumido WHERE consumidor = 'contabilidad-erp')
           = (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO'),
       (SELECT count(*) FROM erp.evento_consumido WHERE consumidor = 'contabilidad-erp') || ' consumidos / '
           || (SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE estado = 'PUBLICADO') || ' publicados';
