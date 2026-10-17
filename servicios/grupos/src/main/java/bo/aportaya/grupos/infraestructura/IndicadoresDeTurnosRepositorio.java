package bo.aportaya.grupos.infraestructura;

import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · los turnos cobrados. La fecha del turno es la entrega PREVISTA de su periodo: la fecha real del desembolso
 * es de {@code entregas} y este servicio no la lee (invariante 11). El turno que cierra el ciclo es el de mayor
 * {@code orden_asignado} del grupo (decision D2 del trabajo tableros-por-rol).
 */
@Repository
public class IndicadoresDeTurnosRepositorio {

    public record Turnos(long normales, long deCierre) {

        public long total() {
            return normales + deCierre;
        }
    }

    public Turnos cobrados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        Record r = dsl.fetchOne(
                """
                SELECT count(*) FILTER (WHERE t.orden_asignado < u.ultimo),
                       count(*) FILTER (WHERE t.orden_asignado = u.ultimo)
                  FROM grupos.turno t
                  JOIN grupos.periodo p ON p.id = t.periodo_id
                  JOIN (SELECT grupo_id, max(orden_asignado) AS ultimo FROM grupos.turno
                         WHERE estado <> 'ANULADO' GROUP BY grupo_id) u ON u.grupo_id = t.grupo_id
                 WHERE t.estado = 'COBRADO' AND p.fecha_entrega_prevista BETWEEN ? AND ?
                """,
                desde,
                hasta);
        return new Turnos(r.get(0, Long.class), r.get(1, Long.class));
    }
}
