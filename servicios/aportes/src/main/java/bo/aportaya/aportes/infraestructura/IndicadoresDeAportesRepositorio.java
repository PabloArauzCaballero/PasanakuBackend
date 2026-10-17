package bo.aportaya.aportes.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · la mora y lo cobrado tarde. Es de {@code aportes} y no del mayor: una obligacion vencida es una cuenta por
 * cobrar que todavia no movio dinero, y el pago tardio solo se distingue del puntual mirando la fecha de gracia de la
 * obligacion. Fechas en hora de La Paz; solo aportes periodicos en la moneda pedida.
 */
@Repository
public class IndicadoresDeAportesRepositorio {

    public record Suma(BigDecimal total, long casos) {}

    public record Tasa(long enMora, long exigibles) {}

    /** De las obligaciones cuya gracia vencio en el rango, cuantas no estaban pagadas al vencer. */
    public Tasa mora(DSLContext dsl, LocalDate desde, LocalDate hasta, String moneda) {
        Record r = dsl.fetchOne(
                """
                SELECT count(*) FILTER (WHERE fecha_pago_efectivo IS NULL
                                           OR (fecha_pago_efectivo AT TIME ZONE 'America/La_Paz')::date > fecha_fin_gracia),
                       count(*)
                  FROM aportes.obligacion_aporte
                 WHERE tipo = 'APORTE_PERIODICO' AND moneda = ?
                   AND estado NOT IN ('ANULADO', 'EXONERADO')
                   AND fecha_fin_gracia BETWEEN ? AND ?
                """,
                moneda,
                desde,
                hasta);
        return new Tasa(r.get(0, Long.class), r.get(1, Long.class));
    }

    /** El saldo vencido y sin cubrir HOY: es foto del estado actual, no se reconstruye hacia atras. */
    public Suma dineroEnMora(DSLContext dsl, LocalDate al, String moneda) {
        return suma(dsl.fetchOne(
                """
                SELECT COALESCE(SUM(saldo_pendiente), 0), count(*)
                  FROM aportes.obligacion_aporte
                 WHERE tipo = 'APORTE_PERIODICO' AND moneda = ? AND saldo_pendiente > 0
                   AND fecha_fin_gracia < ?
                   AND estado NOT IN ('ANULADO', 'EXONERADO', 'CONDONADO', 'PAGADO', 'CUBIERTO_POR_GARANTIA')
                """,
                moneda,
                al));
    }

    /** Lo acreditado en el rango sobre obligaciones cuya gracia ya habia vencido: pago tarde de quien debia. */
    public Suma cobradoTarde(DSLContext dsl, LocalDate desde, LocalDate hasta, String moneda) {
        return suma(dsl.fetchOne(
                """
                SELECT COALESCE(SUM(p.monto), 0), count(*)
                  FROM aportes.pago p JOIN aportes.obligacion_aporte o ON o.id = p.obligacion_id
                 WHERE p.estado = 'ACREDITADO' AND p.moneda = ? AND o.tipo = 'APORTE_PERIODICO'
                   AND (COALESCE(p.fecha_hora_acreditacion, p.fecha_hora_pago) AT TIME ZONE 'America/La_Paz')::date
                       BETWEEN ? AND ?
                   AND (COALESCE(p.fecha_hora_acreditacion, p.fecha_hora_pago) AT TIME ZONE 'America/La_Paz')::date
                       > o.fecha_fin_gracia
                """,
                moneda,
                desde,
                hasta));
    }

    private static Suma suma(Record r) {
        return new Suma(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }
}
