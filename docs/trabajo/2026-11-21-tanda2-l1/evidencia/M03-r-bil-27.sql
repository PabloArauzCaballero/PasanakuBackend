-- F1.L1.M03 · R-BIL-27 probada en la base con el rol de la aplicacion (svc_nucleo_financiero).
-- Usa una transaccion y dos usuarios sinteticos que dejaron las pruebas de integracion.
\set ON_ERROR_STOP 0
SELECT id AS tx FROM nucleo_financiero.transaccion_billetera WHERE tipo <> 'REVERSO' LIMIT 1 \gset
SELECT id AS a FROM identidad.usuario ORDER BY id LIMIT 1 \gset
SELECT id AS b FROM identidad.usuario ORDER BY id DESC LIMIT 1 \gset
BEGIN;
SET LOCAL ROLE svc_nucleo_financiero;
\echo '1) correcto: una solicitud SOLICITADA sin autorizador'
INSERT INTO nucleo_financiero.solicitud_reverso (transaccion_original_id, solicitada_por, clave_idempotencia, tipo, motivo, estado)
VALUES (:'tx', :'a', 'psql-r-bil-27-1', 'ANULACION', 'Prueba de la regla R-BIL-27 con el rol de la app', 'SOLICITADA') RETURNING estado;
SAVEPOINT s;
\echo '2) invalido: autoriza la misma persona que pidio'
UPDATE nucleo_financiero.solicitud_reverso SET autorizada_por = solicitada_por WHERE clave_idempotencia = 'psql-r-bil-27-1';
ROLLBACK TO SAVEPOINT s;
\echo '3) limite: una segunda solicitud viva para la misma transaccion'
INSERT INTO nucleo_financiero.solicitud_reverso (transaccion_original_id, solicitada_por, clave_idempotencia, tipo, motivo, estado)
VALUES (:'tx', :'b', 'psql-r-bil-27-2', 'ANULACION', 'Prueba de la regla R-BIL-27 con el rol de la app', 'SOLICITADA');
ROLLBACK TO SAVEPOINT s;
\echo '4) invalido: EJECUTADA sin autorizador ni reverso'
UPDATE nucleo_financiero.solicitud_reverso SET estado = 'EJECUTADA', resuelta_en = now() WHERE clave_idempotencia = 'psql-r-bil-27-1';
ROLLBACK TO SAVEPOINT s;
\echo '5) invalido: la misma clave de idempotencia dos veces'
INSERT INTO nucleo_financiero.solicitud_reverso (transaccion_original_id, solicitada_por, clave_idempotencia, tipo, motivo, estado)
VALUES (:'tx', :'b', 'psql-r-bil-27-1', 'ANULACION', 'Prueba de la regla R-BIL-27 con el rol de la app', 'SOLICITADA');
ROLLBACK;
