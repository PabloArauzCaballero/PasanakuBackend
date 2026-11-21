\set VERBOSITY terse
-- Variante: dentro de la MISMA transaccion, se adelantan los triggers diferidos y despues se agrega una linea.
SET ROLE svc_nucleo_financiero;
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000b1', now(),'prueba sintetica SET CONSTRAINTS','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000b1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),100.00,0,'BOB','d'),
 ('00000000-0000-0000-0000-0000000000b1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='2.1.01'),0,100.00,'BOB','h');
SET CONSTRAINTS ALL IMMEDIATE;
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000b1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),999.00,0,'BOB','descuadre tras adelantar');
COMMIT;
RESET ROLE;
SELECT a.id, count(m.id) lineas, sum(m.debe) debe, sum(m.haber) haber FROM nucleo_financiero.asiento_contable a
  LEFT JOIN nucleo_financiero.movimiento_contable m ON m.asiento_id=a.id WHERE a.id='00000000-0000-0000-0000-0000000000b1' GROUP BY a.id;
