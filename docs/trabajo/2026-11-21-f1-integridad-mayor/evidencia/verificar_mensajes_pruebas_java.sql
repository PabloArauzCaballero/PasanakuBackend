\set VERBOSITY terse
-- Reproduce en psql los escenarios de CU24MonedaTest.mezclaDeMonedas (10 BOB al debe y 10 USD al haber)
-- y de CU10/CU12/CU24RechazosTest (una pata sola), para confirmar que el primer error al COMMIT
-- sigue siendo el que esas pruebas esperan (R-AUD-12 y R-AUD-05) con los triggers nuevos.
INSERT INTO nucleo_financiero.cuenta_contable(id, codigo, nombre, tipo, naturaleza, moneda, nivel, es_cuenta_de_movimiento, saldo)
VALUES ('00000000-0000-0000-0000-0000000000d1', 'T.USD.F1', 'cuenta sintetica USD', 'PASIVO', 'ACREEDORA', 'USD', 3, true, 0)
ON CONFLICT DO NOTHING;
SET ROLE svc_nucleo_financiero;
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000e1', now(),'mezcla','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000e1',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),10.00,0,'BOB','bob'),
 ('00000000-0000-0000-0000-0000000000e1','00000000-0000-0000-0000-0000000000d1',0,10.00,'USD','usd');
COMMIT;
BEGIN;
INSERT INTO nucleo_financiero.asiento_contable(id,fecha,glosa,origen_tipo,origen_id,estado)
VALUES ('00000000-0000-0000-0000-0000000000e2', now(),'descuadre a proposito','AJUSTE',gen_random_uuid(),'CONFIRMADO');
INSERT INTO nucleo_financiero.movimiento_contable(asiento_id,cuenta_id,debe,haber,moneda,descripcion) VALUES
 ('00000000-0000-0000-0000-0000000000e2',(SELECT id FROM nucleo_financiero.cuenta_contable WHERE codigo='1.1.01'),100.00,0,'BOB','pata sola');
COMMIT;
RESET ROLE;
