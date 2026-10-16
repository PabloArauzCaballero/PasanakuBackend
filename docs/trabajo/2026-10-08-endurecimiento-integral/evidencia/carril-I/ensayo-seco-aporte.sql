-- ENSAYO EN SECO del SQL que escribe ConsumoDeAportes, contra el esquema REAL del stack y con el ROL REAL del servicio.
-- Todo dentro de UNA transaccion que termina en ROLLBACK: no deja filas (solo consume numeros de secuencia).
BEGIN;
SET LOCAL ROLE svc_nucleo_financiero;
SELECT set_config('app.rol', 'sistema', true), set_config('app.usuario_id', '00000000-0000-0000-0000-00000000a5e2', true) \gset
SELECT gen_random_uuid() AS tx \gset
SELECT gen_random_uuid() AS asi \gset
SELECT gen_random_uuid() AS gw \gset
\set usuario '8d347bfe-b89e-4d36-99c2-33c62d75e21c'
\set cuenta '0c6e3e81-81bb-41ee-8fb0-a0b8990f52d5'
\set grupo '0d7b1840-5e7b-48b8-9538-7f22a442469b'
SELECT 'antes: libro usuario', COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0) FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = :'cuenta';
SELECT 'antes: mayor 2.1.01 / 2.1.02 (haber-debe)', (SELECT saldo FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'), (SELECT saldo FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.02');
-- la billetera GRUPO (hoy nadie la abre)
INSERT INTO nucleo_financiero.cuenta_billetera (id, numero_cuenta, tipo, grupo_id, moneda, estado, nivel_debida_diligencia, saldo_disponible, saldo_retenido, permite_saldo_negativo, fecha_apertura, version)
VALUES (:'gw', 'SECO-' || substr(:'gw', 1, 8), 'GRUPO', :'grupo', 'BOB', 'ACTIVA', 'ESTANDAR', 0, 0, false, now(), 0);
-- 1. el asiento PRIMERO (origen PAGO, origen_id = la transaccion que va a existir)
INSERT INTO nucleo_financiero.asiento_contable (id, fecha, glosa, origen_tipo, origen_id, grupo_id, estado)
VALUES (:'asi', now(), 'Aporte cobrado (ensayo en seco)', 'PAGO', :'tx', :'grupo', 'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, descripcion, moneda)
SELECT gen_random_uuid(), :'asi', id, 1500.00, 0.00, 'seco debe', 'BOB' FROM nucleo_financiero.cuenta_contable WHERE codigo = '2.1.01';
INSERT INTO nucleo_financiero.movimiento_contable (id, asiento_id, cuenta_id, debe, haber, descripcion, moneda)
SELECT gen_random_uuid(), :'asi', id, 0.00, 1500.00, 'seco haber', 'BOB' FROM nucleo_financiero.cuenta_contable WHERE codigo = '2.1.02';
-- 2. la transaccion, YA apuntando al asiento y al grupo
INSERT INTO nucleo_financiero.transaccion_billetera (id, tipo, estado, moneda, monto_total, grupo_id, asiento_contable_id, origen_tipo, origen_id, canal, iniciada_por, clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
VALUES (:'tx', 'APORTE_A_GRUPO', 'APLICADA', 'BOB', 1500.00, :'grupo', :'asi', 'OBLIGACION_APORTE', gen_random_uuid(), 'API', :'usuario', 'aporte-cobrado:seco-' || :'tx', repeat('a', 64), now(), now());
INSERT INTO nucleo_financiero.movimiento_billetera (id, transaccion_id, cuenta_billetera_id, orden, sentido, monto, saldo_disponible_posterior, saldo_retenido_posterior, glosa, registrado_en)
VALUES (gen_random_uuid(), :'tx', :'cuenta', 1, 'DEBITO', 1500.00, 0, 0, 'seco', now()),
       (gen_random_uuid(), :'tx', :'gw', 2, 'CREDITO', 1500.00, 0, 0, 'seco', now());
SET CONSTRAINTS ALL IMMEDIATE;
SELECT 'despues: libro usuario', COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0) FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = :'cuenta';
SELECT 'despues: libro grupo', COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0) FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id = :'gw';
SELECT 'despues: mayor 2.1.01 / 2.1.02 (cache R-CTB-09)', (SELECT saldo FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'), (SELECT saldo FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.02');
SELECT 'asiento cuadra', SUM(debe), SUM(haber), count(*) FROM nucleo_financiero.movimiento_contable WHERE asiento_id = :'asi';
SELECT 'enlaces', (SELECT asiento_contable_id = :'asi' FROM nucleo_financiero.transaccion_billetera WHERE id = :'tx'), (SELECT origen_id = :'tx' FROM nucleo_financiero.asiento_contable WHERE id = :'asi');
-- 3. la unicidad por pago (uq_tx_idem): el MISMO pago otra vez debe chocar
SAVEPOINT duplicado;
INSERT INTO nucleo_financiero.transaccion_billetera (id, tipo, estado, moneda, monto_total, grupo_id, origen_tipo, origen_id, canal, iniciada_por, clave_idempotencia, hash_registro, ocurrida_en, registrada_en)
VALUES (gen_random_uuid(), 'APORTE_A_GRUPO', 'APLICADA', 'BOB', 1500.00, :'grupo', 'OBLIGACION_APORTE', gen_random_uuid(), 'API', :'usuario', 'aporte-cobrado:seco-' || :'tx', repeat('b', 64), now(), now());
ROLLBACK TO SAVEPOINT duplicado;
ROLLBACK;
