package bo.aportaya.garantia;

import java.math.BigDecimal;
import java.util.UUID;
import org.jooq.DSLContext;

/** Las filas de un deudor del fondo: separadas de {@link FixturaDeGarantia} por la regla de tamano de archivo. */
class FixturaDeDeudor {

    private final DSLContext dsl;

    FixturaDeDeudor(DSLContext dsl) {
        this.dsl = dsl;
    }

    /**
     * Un expediente cubierto por el fondo, con su deuda: los hechos que dejaba la cobertura manual retirada
     * (F1.L2.M02). Las pruebas que parten de un deudor (CU-27, CU-66, tableros) los arman aca hasta que la cobertura
     * del modelo A (F3.L2.M05) los vuelva a escribir como acto del sistema. Cubrir no perdona: queda la deuda. Con
     * {@code aplicada=false} queda solo el intento RECHAZADO, sin deuda ni transicion.
     */
    UUID cubiertoPorElFondo(UUID expedienteId, String monto, UUID ejecutadoPor, boolean aplicada) {
        var r = dsl.fetchOne(
                """
                SELECT r.usuario_id, r.participante_id, r.grupo_id, r.periodo_id, r.obligacion_id, f.id, f.moneda,
                       COALESCE((SELECT h.estado_nuevo FROM garantia.historial_estado_incumplimiento h
                                  WHERE h.registro_id = r.id ORDER BY h.fecha_hora DESC, h.id DESC LIMIT 1),
                                r.estado)
                  FROM garantia.registro_incumplimiento r
                  JOIN garantia.fondo_garantia f ON f.grupo_id = r.grupo_id
                 WHERE r.id = ?
                """,
                expedienteId);
        UUID cobertura = UUID.randomUUID();
        BigDecimal importe = new BigDecimal(monto);
        dsl.execute(
                """
                INSERT INTO garantia.cobertura_incumplimiento
                    (id, fondo_id, registro_id, obligacion_id, periodo_id, monto_solicitado, monto_cubierto,
                     porcentaje_cobertura, estado, requirio_aprobacion_manual, solicitada_en, aplicada_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, now(), CASE WHEN ?::boolean THEN now() END)
                """,
                cobertura,
                r.get(5, UUID.class),
                expedienteId,
                r.get(4, UUID.class),
                r.get(3, UUID.class),
                importe,
                aplicada ? importe : BigDecimal.ZERO,
                aplicada ? 100 : 0,
                aplicada ? "APLICADA" : "RECHAZADA",
                aplicada);
        if (!aplicada) {
            return cobertura;
        }
        dsl.execute(
                """
                INSERT INTO garantia.deuda_participante
                    (id, usuario_id, participante_id, grupo_id, registro_id, cobertura_id, acreedor, capital_original,
                     recargos_acumulados, total_abonado, saldo_actual, moneda, estado, es_subrogada,
                     fecha_exigibilidad, fecha_prescripcion, dias_vencida, version)
                VALUES (gen_random_uuid(), ?, ?, ?, ?, ?, 'FONDO_GARANTIA', ?, 0, 0, ?, ?, 'VIGENTE', false,
                        current_date + 30, (current_date + interval '5 years')::date, 0, 0)
                """,
                r.get(0, UUID.class),
                r.get(1, UUID.class),
                r.get(2, UUID.class),
                expedienteId,
                cobertura,
                importe,
                importe,
                r.get(6, String.class));
        dsl.execute(
                """
                INSERT INTO garantia.historial_estado_incumplimiento
                    (id, registro_id, estado_anterior, estado_nuevo, motivo, monto_asociado, ejecutado_por,
                     es_automatico, fecha_hora)
                VALUES (gen_random_uuid(), ?, ?, 'CUBIERTO_POR_GARANTIA',
                        'Cubierto por el fondo, con deuda a recuperar', ?, ?, false, now())
                """,
                expedienteId,
                r.get(7, String.class),
                importe,
                ejecutadoPor);
        return cobertura;
    }
}
