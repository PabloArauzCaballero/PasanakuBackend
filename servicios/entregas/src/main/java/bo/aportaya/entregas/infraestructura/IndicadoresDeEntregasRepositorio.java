package bo.aportaya.entregas.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · las entregas que debian salir y no salieron, y cuanto tardan en salir. Ambas se reconstruyen a cualquier
 * fecha con {@code historial_estado_entrega} (cuando paso a ENTREGADA), no con el estado de hoy. Fechas de La Paz.
 */
@Repository
public class IndicadoresDeEntregasRepositorio {

    public record Conteo(long cuantas) {}

    public record Promedio(BigDecimal dias, long casos) {}

    /** Programadas hasta `al` (inclusive) que a esa fecha no habian sido ENTREGADAS ni estaban anuladas. */
    public Conteo pendientes(DSLContext dsl, LocalDate al) {
        Record r = dsl.fetchOne(
                """
                SELECT count(*) FROM entregas.entrega_fondo e
                 WHERE e.fecha_programada <= ? AND e.estado <> 'ANULADA'
                   AND NOT EXISTS (SELECT 1 FROM entregas.historial_estado_entrega h
                                    WHERE h.entrega_id = e.id AND h.estado_nuevo = 'ENTREGADA'
                                      AND (h.fecha_hora AT TIME ZONE 'America/La_Paz')::date <= ?)
                """,
                al,
                al);
        return new Conteo(r.get(0, Long.class));
    }

    /** Promedio de dias entre la fecha programada y la ENTREGA, de las entregadas en el rango. Dos decimales. */
    public Promedio tiempoHastaDesembolso(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        Record r = dsl.fetchOne(
                """
                SELECT COALESCE(ROUND(AVG((h.fecha_hora AT TIME ZONE 'America/La_Paz')::date - e.fecha_programada), 2), 0),
                       count(*)
                  FROM entregas.entrega_fondo e
                  JOIN LATERAL (SELECT min(fecha_hora) AS fecha_hora FROM entregas.historial_estado_entrega
                                 WHERE entrega_id = e.id AND estado_nuevo = 'ENTREGADA') h ON h.fecha_hora IS NOT NULL
                 WHERE (h.fecha_hora AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                """,
                desde,
                hasta);
        return new Promedio(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }
}
