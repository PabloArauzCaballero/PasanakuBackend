\set ON_ERROR_STOP 0
BEGIN;
SET LOCAL session_replication_role = replica; -- solo apaga las FK (triggers); CHECK y UNIQUE siguen
INSERT INTO entregas.oferta_turno (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda, monto_derecho, monto_precio, monto_cargos, estado, vigente_hasta, publicada_en, clave_idempotencia, version)
VALUES ('00000000-0000-4000-8000-0000000000a1', gen_random_uuid(), '00000000-0000-4000-8000-0000000000f1', gen_random_uuid(), '00000000-0000-4000-8000-0000000000b1', gen_random_uuid(), 'BOB', 6000.00, 5500.00, 50.00, 'PUBLICADA', now() + interval '1 day', now(), 'oferta-a1', 0);
SAVEPOINT s1;
INSERT INTO entregas.oferta_turno (id, grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda, monto_derecho, monto_precio, monto_cargos, estado, vigente_hasta, publicada_en, clave_idempotencia, version)
SELECT gen_random_uuid(), grupo_id, turno_id, cupo_id, participante_origen_id, vendedor_usuario_id, moneda, monto_derecho, monto_precio, monto_cargos, 'PUBLICADA', vigente_hasta, now(), 'directa-' || gen_random_uuid(), 0 FROM entregas.oferta_turno WHERE id = '00000000-0000-4000-8000-0000000000a1';
ROLLBACK TO s1;
UPDATE entregas.oferta_turno SET estado='VENDIDA' WHERE id='00000000-0000-4000-8000-0000000000a1';
INSERT INTO entregas.cesion_derecho (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, estado, retencion_ref, liquidacion_ref, clave_idempotencia, creada_en, titulo_asignado_en, liquidada_en, version)
VALUES ('00000000-0000-4000-8000-0000000000c1', '00000000-0000-4000-8000-0000000000a1', '00000000-0000-4000-8000-0000000000f1', '00000000-0000-4000-8000-0000000000b1', '00000000-0000-4000-8000-0000000000b2', gen_random_uuid(), 'BOB', 5500.00, 'LIQUIDADA', gen_random_uuid(), gen_random_uuid(), 'cesion-c1', now(), now(), now(), 0);
SELECT 'cesion liquidada insertada' AS paso, count(*) FROM entregas.cesion_derecho WHERE id='00000000-0000-4000-8000-0000000000c1';
SAVEPOINT s2;
UPDATE entregas.cesion_derecho SET liquidacion_ref = NULL WHERE id = '00000000-0000-4000-8000-0000000000c1';
ROLLBACK TO s2;
INSERT INTO entregas.cesion_derecho (id, oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, estado, clave_idempotencia, creada_en, version)
SELECT gen_random_uuid(), oferta_turno_id, turno_id, participante_origen_id, participante_destino_id, comprador_usuario_id, moneda, monto_precio, 'VALIDADA', 'otra-' || gen_random_uuid(), now(), 0 FROM entregas.cesion_derecho WHERE id = '00000000-0000-4000-8000-0000000000c1';
ROLLBACK TO s2;
UPDATE entregas.cesion_derecho SET participante_destino_id = participante_origen_id WHERE id = '00000000-0000-4000-8000-0000000000c1';
ROLLBACK;
