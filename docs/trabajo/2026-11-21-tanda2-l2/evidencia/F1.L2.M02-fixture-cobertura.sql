-- Las sentencias de FixturaDeGarantia.cubiertoPorElFondo, con valores sinteticos.
-- Sin FK (replica) porque la base local no tiene expedientes; los CHECK y los tipos SI se aplican. ROLLBACK al final.
BEGIN;
SET LOCAL session_replication_role = replica;
-- aplicada = true
INSERT INTO garantia.cobertura_incumplimiento
    (id, fondo_id, registro_id, obligacion_id, periodo_id, monto_solicitado, monto_cubierto,
     porcentaje_cobertura, estado, requirio_aprobacion_manual, solicitada_en, aplicada_en)
VALUES ('c0000000-0000-4000-8000-000000000001', gen_random_uuid(), 'c0000000-0000-4000-8000-0000000000e1',
        gen_random_uuid(), gen_random_uuid(), 500.00, 500.00, 100, 'APLICADA', false, now(),
        CASE WHEN true::boolean THEN now() END);
INSERT INTO garantia.deuda_participante
    (id, usuario_id, participante_id, grupo_id, registro_id, cobertura_id, acreedor, capital_original,
     recargos_acumulados, total_abonado, saldo_actual, moneda, estado, es_subrogada,
     fecha_exigibilidad, fecha_prescripcion, dias_vencida, version)
VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(),
        'c0000000-0000-4000-8000-0000000000e1', 'c0000000-0000-4000-8000-000000000001', 'FONDO_GARANTIA', 500.00,
        0, 0, 500.00, 'BOB', 'VIGENTE', false, current_date + 30, (current_date + interval '5 years')::date, 0, 0);
INSERT INTO garantia.historial_estado_incumplimiento
    (id, registro_id, estado_anterior, estado_nuevo, motivo, monto_asociado, ejecutado_por, es_automatico, fecha_hora)
VALUES (gen_random_uuid(), 'c0000000-0000-4000-8000-0000000000e1', 'NOTIFICADO', 'CUBIERTO_POR_GARANTIA',
        'Cubierto por el fondo, con deuda a recuperar', 500.00, gen_random_uuid(), false, now());
-- aplicada = false: solo el intento RECHAZADO
INSERT INTO garantia.cobertura_incumplimiento
    (id, fondo_id, registro_id, obligacion_id, periodo_id, monto_solicitado, monto_cubierto,
     porcentaje_cobertura, estado, requirio_aprobacion_manual, solicitada_en, aplicada_en)
VALUES (gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(), gen_random_uuid(),
        500.00, 0, 0, 'RECHAZADA', false, now(), CASE WHEN false::boolean THEN now() END);
SELECT estado, monto_cubierto, aplicada_en IS NOT NULL AS aplicada FROM garantia.cobertura_incumplimiento
 WHERE id = 'c0000000-0000-4000-8000-000000000001' OR estado = 'RECHAZADA' ORDER BY estado;
-- el tablero (IndicadoresDeGarantiaRepositorio.cubierto) cuenta la aplicada y no la rechazada
SELECT COALESCE(SUM(monto_cubierto), 0), count(*) FROM garantia.cobertura_incumplimiento
 WHERE aplicada_en IS NOT NULL AND estado NOT IN ('RECHAZADA', 'REVERSADA');
ROLLBACK;
