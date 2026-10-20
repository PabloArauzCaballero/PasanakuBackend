package bo.aportaya.grupos.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * El calendario de periodos de un grupo, que se arma cuando se conforma (2026-10-20). Un periodo por cupo: es
 * lo que el sorteo necesita para asignar un turno a cada uno (CU-60, AP-CU60-09).
 *
 * <p>Supuesto registrado (a confirmar con producto): el periodo n empieza en {@code fecha_inicio + (n-1)·paso}
 * —o hoy, si la fecha de inicio ya paso—, vence el ultimo dia de su paso, la gracia son los {@code dias_gracia}
 * del grupo y la entrega se preve para el dia siguiente a la gracia. Idempotente: un periodo que ya existe no se
 * toca.
 */
@Component
public class CalendarioDelGrupoRepositorio {

    public int generar(DSLContext dsl, java.util.UUID grupoId, LocalDate hoy) {
        Record g = dsl.fetchOne(
                """
                SELECT fecha_inicio, periodicidad, dias_gracia, num_periodos, cupos_totales, monto_aporte
                  FROM grupos.grupo WHERE id = ?
                """,
                grupoId);
        if (g == null) return 0;
        LocalDate inicio = g.get("fecha_inicio", LocalDate.class);
        if (inicio.isBefore(hoy)) inicio = hoy;
        String periodicidad = g.get("periodicidad", String.class);
        int gracia = g.get("dias_gracia", Integer.class);
        int periodos = g.get("num_periodos", Integer.class);
        BigDecimal objetivo = g.get("monto_aporte", BigDecimal.class)
                .multiply(BigDecimal.valueOf(g.get("cupos_totales", Integer.class)));
        int creados = 0;
        for (int n = 1; n <= periodos; n++) {
            LocalDate desde = sumar(inicio, periodicidad, n - 1);
            LocalDate limite = sumar(inicio, periodicidad, n).minusDays(1);
            LocalDate finGracia = limite.plusDays(gracia);
            creados += dsl.execute(
                    """
                    INSERT INTO grupos.periodo
                      (grupo_id, numero, fecha_inicio, fecha_limite_pago, fecha_fin_gracia, fecha_entrega_prevista,
                       estado, monto_objetivo, monto_recaudado, cupos_morosos)
                    VALUES (?, ?, ?, ?, ?, ?, 'PROGRAMADO', ?, 0, 0)
                    ON CONFLICT (grupo_id, numero) DO NOTHING
                    """,
                    grupoId,
                    (short) n,
                    desde,
                    limite,
                    finGracia,
                    finGracia.plusDays(1),
                    objetivo);
        }
        return creados;
    }

    private static LocalDate sumar(LocalDate desde, String periodicidad, int pasos) {
        return switch (periodicidad) {
            case "SEMANAL" -> desde.plusWeeks(pasos);
            case "QUINCENAL" -> desde.plusDays(15L * pasos);
            case "BIMENSUAL" -> desde.plusMonths(2L * pasos);
            default -> desde.plusMonths(pasos);
        };
    }
}
