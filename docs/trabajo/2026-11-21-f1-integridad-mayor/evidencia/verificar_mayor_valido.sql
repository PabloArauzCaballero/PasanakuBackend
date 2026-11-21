\set VERBOSITY terse
-- Camino legitimo: asiento confirmado con sus lineas en la misma transaccion, y su contra-asiento.
SET ROLE svc_nucleo_financiero;
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000c1', now(),'prueba sintetica valida','AJUSTE','00000000-0000-0000-0000-0000000000f1','CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000c1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),100.00,0,'BOB','d'),
 ('00000000-0000-0000-0000-0000000000c1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,100.00,'BOB','h');
COMMIT;
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado,asiento_reversa_id)
VALUES ('00000000-0000-0000-0000-0000000000c2', now(),'contra-asiento sintetico','AJUSTE','00000000-0000-0000-0000-0000000000f1','REVERSADO','00000000-0000-0000-0000-0000000000c1');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000c2',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),0,100.00,'BOB','reversa h'),
 ('00000000-0000-0000-0000-0000000000c2',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),100.00,0,'BOB','reversa d');
COMMIT;
-- subtransaccion: lineas insertadas tras un SAVEPOINT sobre el asiento de la misma transaccion
BEGIN;
SAVEPOINT s1;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000c3', now(),'prueba sintetica savepoint','AJUSTE',gen_random_uuid(),'CONFIRMADO');
RELEASE SAVEPOINT s1;
SAVEPOINT s2;
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000c3',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),5.00,0,'BOB','d'),
 ('00000000-0000-0000-0000-0000000000c3',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,5.00,'BOB','h');
RELEASE SAVEPOINT s2;
COMMIT;
RESET ROLE;
SELECT a.id, a.estado, count(m.id) lineas, sum(m.debe) debe, sum(m.haber) haber
  FROM nucleo_financiero.asiento_contable a JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id=a.id
 WHERE a.id::text LIKE '00000000-0000-0000-0000-0000000000c%' GROUP BY a.id,a.estado ORDER BY a.id;
