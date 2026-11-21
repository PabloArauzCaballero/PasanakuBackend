\set ON_ERROR_STOP 0
BEGIN;
SET LOCAL session_replication_role = replica; -- apaga solo las FK; los CHECK siguen
SAVEPOINT a;
INSERT INTO entregas.entrega_fondo (grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id, moneda, estado, metodo_desembolso, fecha_programada)
VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 'BOB', 'PROGRAMADA', 'TRANSFERENCIA_BANCARIA', current_date);
ROLLBACK TO a;
INSERT INTO entregas.entrega_fondo (grupo_id, periodo_id, turno_id, cupo_id, beneficiario_participante_id, moneda, estado, metodo_desembolso, fecha_programada)
VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), 'BOB', 'PROGRAMADA', 'EFECTIVO_ORGANIZADOR', current_date);
ROLLBACK;
