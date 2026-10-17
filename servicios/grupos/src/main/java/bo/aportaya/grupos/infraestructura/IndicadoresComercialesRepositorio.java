package bo.aportaya.grupos.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · NEGOCIO, lo que es de {@code grupos}. La creacion y la conformacion de un grupo salen de
 * {@code historial_estado_grupo} (el grupo no guarda cuando se creo); sin historial, la creacion es {@code fecha_inicio}.
 * Solo conteos y sumas: ninguna persona sale de aca (regla 90.2). Fechas en hora de La Paz.
 */
@Repository
public class IndicadoresComercialesRepositorio {

    public record Suma(BigDecimal total, long casos) {}

    public record Tasa(long numerador, long denominador) {}

    /** Personas distintas con un cupo vigente en un grupo en curso, HOY. */
    public long activos(DSLContext dsl) {
        return dsl.fetchOne(
                        """
                        SELECT count(DISTINCT p.usuario_id) FROM grupos.participante p
                          JOIN grupos.grupo g ON g.id = p.grupo_id
                         WHERE g.estado IN ('EN_CURSO', 'ACTIVO') AND p.estado IN ('ACTIVO', 'EN_MORA')
                        """)
                .get(0, Long.class);
    }

    /** Personas cuya PRIMERA entrada a un grupo cae en el rango. */
    public long primerIngreso(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.fetchOne(
                        """
                        SELECT count(*) FROM (SELECT usuario_id, min(fecha_ingreso) AS primera
                                                FROM grupos.participante GROUP BY usuario_id) x
                         WHERE (x.primera AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                        """,
                        desde,
                        hasta)
                .get(0, Long.class);
    }

    public long creados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.fetchOne(
                        """
                        SELECT count(*) FROM grupos.grupo g
                         WHERE COALESCE((SELECT (min(h.fecha_hora) AT TIME ZONE 'America/La_Paz')::date
                                           FROM grupos.historial_estado_grupo h WHERE h.grupo_id = g.id),
                                        g.fecha_inicio) BETWEEN ? AND ?
                        """,
                        desde,
                        hasta)
                .get(0, Long.class);
    }

    public long conformados(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        return dsl.fetchOne(
                        """
                        SELECT count(DISTINCT grupo_id) FROM grupos.historial_estado_grupo
                         WHERE estado_nuevo = 'CONFORMADO'
                           AND (fecha_hora AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                        """,
                        desde,
                        hasta)
                .get(0, Long.class);
    }

    /** Dias de creacion a conformacion, de los conformados en el rango que tienen historial de ambas. */
    public Suma diasDeLlenado(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        Record r = dsl.fetchOne(
                """
                SELECT COALESCE(ROUND(AVG(EXTRACT(EPOCH FROM (c.conformado - c.creado)) / 86400.0), 2), 0), count(*)
                  FROM (SELECT grupo_id, min(fecha_hora) AS creado,
                               min(fecha_hora) FILTER (WHERE estado_nuevo = 'CONFORMADO') AS conformado
                          FROM grupos.historial_estado_grupo GROUP BY grupo_id) c
                 WHERE c.conformado IS NOT NULL AND c.creado < c.conformado
                   AND (c.conformado AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                """,
                desde,
                hasta);
        return new Suma(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }

    /** De quienes estaban en un grupo FINALIZADO en el rango, cuantos entraron a otro dentro de los 60 dias. */
    public Tasa retencion(DSLContext dsl, LocalDate desde, LocalDate hasta) {
        Record r = dsl.fetchOne(
                """
                SELECT count(*) FILTER (WHERE EXISTS (
                           SELECT 1 FROM grupos.participante p2
                            WHERE p2.usuario_id = p.usuario_id AND p2.grupo_id <> p.grupo_id
                              AND p2.fecha_ingreso >= f.fin AND p2.fecha_ingreso < f.fin + interval '60 days')),
                       count(*)
                  FROM (SELECT grupo_id, min(fecha_hora) AS fin FROM grupos.historial_estado_grupo
                         WHERE estado_nuevo = 'FINALIZADO' GROUP BY grupo_id) f
                  JOIN grupos.participante p ON p.grupo_id = f.grupo_id
                 WHERE (f.fin AT TIME ZONE 'America/La_Paz')::date BETWEEN ? AND ?
                """,
                desde,
                hasta);
        return new Tasa(r.get(0, Long.class), r.get(1, Long.class));
    }

    /** Cupos ocupados x cuota x periodos que faltan, de los grupos en curso HOY, en la moneda pedida. */
    public Suma volumenComprometido(DSLContext dsl, String moneda) {
        Record r = dsl.fetchOne(
                """
                SELECT COALESCE(SUM(g.cupos_ocupados * g.monto_aporte * (
                           SELECT count(*) FROM grupos.periodo pe
                            WHERE pe.grupo_id = g.id AND pe.estado NOT IN ('LIQUIDADO', 'CERRADO', 'CANCELADO'))), 0),
                       count(*)
                  FROM grupos.grupo g
                 WHERE g.estado IN ('EN_CURSO', 'ACTIVO') AND g.moneda = ?
                """,
                moneda);
        return new Suma(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }
}
