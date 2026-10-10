-- Cuentas de sistema que los servicios necesitan para operar: custodia de inversion, ingresos de la plataforma, puente de custodia, y el mapeo SINTETICO de las cuentas logicas de garantia (CAJA_EMPRESA, RESERVA_RESPALDO, POZO_GRUPO) a cuentas del plan, por moneda.
-- GENERADO desde seeders/minimos/22-cuentas-de-sistema.json — no editar a mano.

-- SINTETICO · apto_produccion=false. Arbol 9 (solo BOB y USD): cada cuenta es de UNA moneda (R-CTB-10). El mapeo logica->codigo vive en la configuracion de nucleo-financiero (aportaya.contabilidad.cuentas-logicas.<LOGICA>.<MONEDA>), no aca.
-- Jerarquía en la propia tabla: una sentencia por fila para que cada
-- hija vea a su madre ya insertada.
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9', 'Cuentas sinteticas de simulacion (NO aptas para produccion)', 'ACTIVO', 'DEUDORA', 'BOB', NULL, 1, FALSE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.1', 'Sinteticas BOB', 'ACTIVO', 'DEUDORA', 'BOB', (SELECT id FROM cuenta_contable WHERE codigo = '9'), 2, FALSE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.2', 'Sinteticas USD', 'ACTIVO', 'DEUDORA', 'USD', (SELECT id FROM cuenta_contable WHERE codigo = '9'), 2, FALSE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.1.01', 'Caja de la empresa BOB (sintetica)', 'ACTIVO', 'DEUDORA', 'BOB', (SELECT id FROM cuenta_contable WHERE codigo = '9.1'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.1.02', 'Reserva de respaldo BOB (sintetica)', 'ACTIVO', 'DEUDORA', 'BOB', (SELECT id FROM cuenta_contable WHERE codigo = '9.1'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.1.03', 'Pozo del grupo BOB (sintetica)', 'PASIVO', 'ACREEDORA', 'BOB', (SELECT id FROM cuenta_contable WHERE codigo = '9.1'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.2.01', 'Caja de la empresa USD (sintetica)', 'ACTIVO', 'DEUDORA', 'USD', (SELECT id FROM cuenta_contable WHERE codigo = '9.2'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.2.02', 'Reserva de respaldo USD (sintetica)', 'ACTIVO', 'DEUDORA', 'USD', (SELECT id FROM cuenta_contable WHERE codigo = '9.2'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;
INSERT INTO cuenta_contable (codigo, nombre, tipo, naturaleza, moneda, cuenta_padre_id, nivel, es_cuenta_de_movimiento, saldo) VALUES
  ('9.2.03', 'Pozo del grupo USD (sintetica)', 'PASIVO', 'ACREEDORA', 'USD', (SELECT id FROM cuenta_contable WHERE codigo = '9.2'), 3, TRUE, 0)
ON CONFLICT (codigo) DO NOTHING;

-- Cuentas de billetera de sistema, en cero. Una custodia de inversion activa por moneda (R-BIL-25); una de ingresos por moneda, porque con dos nucleo-financiero no elige. Sin cuenta contable espejo donde contabilidad no la definio. Ids estables (…c0 puente, c1/c2 ingresos, c3/c4 custodia de inversion): la segunda corrida no inserta nada. Sin objetivo de conflicto a proposito: la segunda corrida, y una base donde ya hay una custodia de inversion con otro numero (R-BIL-25), no rompen el despliegue del esquema.
INSERT INTO cuenta_billetera (id, numero_cuenta, tipo, moneda, estado, nivel_debida_diligencia, fecha_apertura, cuenta_contable_id, permite_saldo_negativo) VALUES
  ('00000000-0000-0000-0000-0000000000c0', 'SYS-CUSTODIA', 'PUENTE_CUSTODIA', 'BOB', 'ACTIVA', 'REFORZADA', now(), (SELECT id FROM cuenta_contable WHERE codigo = '1.1.01'), TRUE),
  ('00000000-0000-0000-0000-0000000000c1', 'SYS-INGRESOS', 'PLATAFORMA_INGRESOS', 'BOB', 'ACTIVA', 'REFORZADA', now(), (SELECT id FROM cuenta_contable WHERE codigo = '4.1.01'), TRUE),
  ('00000000-0000-0000-0000-0000000000c2', 'SYS-INGRESOS-USD', 'PLATAFORMA_INGRESOS', 'USD', 'ACTIVA', 'REFORZADA', now(), NULL, TRUE),
  ('00000000-0000-0000-0000-0000000000c3', 'SYS-CUSTINV-BOB', 'CUSTODIA_INVERSION', 'BOB', 'ACTIVA', 'REFORZADA', now(), NULL, FALSE),
  ('00000000-0000-0000-0000-0000000000c4', 'SYS-CUSTINV-USD', 'CUSTODIA_INVERSION', 'USD', 'ACTIVA', 'REFORZADA', now(), NULL, FALSE)
ON CONFLICT DO NOTHING;
