package bo.aportaya.garantia.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · los numeros de RIESGO y FINANZAS cuyo dueño es {@code garantia}. El saldo del fondo sale de la cadena de
 * {@code movimiento_fondo} (el ultimo {@code saldo_resultante} de cada fondo a la fecha), no de la cabecera de
 * {@code fondo_garantia}, que es cache. Fechas en hora de La Paz.
 */
@Repository
public class IndicadoresDeGarantiaRepositorio {

    public record Suma(BigDecimal total, long casos) {}

    /** Lo cubierto por el fondo en el rango: coberturas aplicadas que no se rechazaron ni se reversaron. */
    public Suma cubierto(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return suma(dsl.fetchOne(
                """
                SELECT COALESCE(SUM(monto_cubierto), 0), count(*) FROM garantia.cobertura_incumplimiento
                 WHERE aplicada_en IS NOT NULL AND estado NOT IN ('RECHAZADA', 'REVERSADA', 'SOLICITADA')
                   AND (aplicada_en AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                """,
                desde,
                hasta));
    }

    /** Lo recuperado de deudas cubiertas en el rango: abonos no revertidos. */
    public Suma recuperado(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return suma(dsl.fetchOne(
                """
                SELECT COALESCE(SUM(monto), 0), count(*) FROM garantia.abono_recuperacion
                 WHERE revertido_en IS NULL AND (fecha AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                """,
                desde,
                hasta));
    }

    /** El saldo de todos los fondos al cierre del dia: el ultimo resultante de cada uno. */
    public Suma saldoDeLosFondos(DSLContext dsl, LocalDate al) {
        return suma(dsl.fetchOne(
                """
                SELECT COALESCE(SUM(u.saldo_resultante), 0), count(*) FROM (
                    SELECT DISTINCT ON (fondo_id) saldo_resultante FROM garantia.movimiento_fondo
                     WHERE (fecha AT TIME ZONE 'America/La_Paz')::date <= ?
                     ORDER BY fondo_id, fecha DESC, id DESC) u
                """,
                al));
    }

    private static Suma suma(Record r) {
        return new Suma(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }
}
