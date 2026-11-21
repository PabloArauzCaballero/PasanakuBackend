\set VERBOSITY terse
SET ROLE svc_nucleo_financiero;
-- 1) asiento confirmado y cuadrado, en su propia transaccion
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000a1', now(),'prueba sintetica F1.S1.M1','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000a1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),100.00,0,'BOB','d'),
 ('00000000-0000-0000-0000-0000000000a1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,100.00,'BOB','h');
COMMIT;
-- 2) linea tardia descuadrante en otra transaccion
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000a1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),50.00,0,'BOB','tardia');
-- 3) par tardio cuadrado en otra transaccion
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000a1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),30.00,0,'BOB','tardia par d'),
 ('00000000-0000-0000-0000-0000000000a1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,30.00,'BOB','tardia par h');
-- 4) asiento nuevo con lineas negativas que "cuadran"
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000a2', now(),'prueba sintetica negativa','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000a2',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),-10.00,0,'BOB','neg'),
 ('00000000-0000-0000-0000-0000000000a2',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,-10.00,'BOB','neg');
COMMIT;
-- 5) linea con debe y haber a la vez
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000a3', now(),'prueba sintetica ambigua','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000a3',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),20.00,20.00,'BOB','ambigua');
COMMIT;
-- 6) asiento confirmado sin lineas
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000a4', now(),'prueba sintetica vacia','AJUSTE',gen_random_uuid(),'CONFIRMADO');
COMMIT;
RESET ROLE;
SELECT a.id, a.estado, count(m.id) lineas, COALESCE(sum(m.debe),0) debe, COALESCE(sum(m.haber),0) haber
  FROM nucleo_financiero.asiento_contable a LEFT JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id=a.id
 WHERE a.id::text LIKE '00000000-0000-0000-0000-0000000000a%' GROUP BY a.id,a.estado ORDER BY a.id;
