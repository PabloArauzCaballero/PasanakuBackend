package bo.aportaya.aportes.infraestructura;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Lo recaudado de un dia, por moneda (CU-51, backoffice carril A · H8): {@code pago},
 * {@code conciliacion} y {@code excepcion_conciliacion} de este esquema, que nucleo-financiero no lee.
 *
 * <p>El dia llega como intervalo {@code [desde, hasta)} ya cortado en la zona del negocio: esta clase no
 * decide que es «un dia».
 */
@Component
public class RecaudoDiarioRepositorio {

    public List<PorMoneda> porMoneda(DSLContext dsl, OffsetDateTime desde, OffsetDateTime hasta) {
        return dsl.fetch(
                        """
                SELECT p.moneda,
                       sum(p.monto) AS recaudado,
                       COALESCE(sum(p.monto) FILTER (
                           WHERE c.estado IN ('CONCILIADO_AUTOMATICO', 'CONCILIADO_MANUAL')), 0) AS conciliado,
                       COALESCE(sum(e.diferencia), 0) AS excepciones,
                       count(*) AS pagos
                  FROM aportes.pago p
                  LEFT JOIN aportes.conciliacion c ON c.pago_id = p.id
                  LEFT JOIN LATERAL (
                        SELECT sum(abs(x.monto_diferencia)) AS diferencia
                          FROM aportes.excepcion_conciliacion x
                         WHERE x.conciliacion_id = c.id AND x.estado <> 'RESUELTA') e ON true
                 WHERE p.estado = 'ACREDITADO'
                   AND p.fecha_hora_acreditacion >= ?::timestamptz AND p.fecha_hora_acreditacion < ?::timestamptz
                 GROUP BY p.moneda
                 ORDER BY p.moneda
                """,
                        desde,
                        hasta)
                .map(f -> new PorMoneda(
                        f.get("moneda", String.class).trim(),
                        f.get("recaudado", BigDecimal.class),
                        f.get("conciliado", BigDecimal.class),
                        f.get("excepciones", BigDecimal.class),
                        f.get("pagos", Integer.class)));
    }

    /** Excepciones no resueltas de los pagos acreditados del dia, en cualquier moneda. */
    public int excepcionesAbiertas(DSLContext dsl, OffsetDateTime desde, OffsetDateTime hasta) {
        return dsl.fetchOne(
                        """
                SELECT count(*)::int
                  FROM aportes.excepcion_conciliacion x
                  JOIN aportes.conciliacion c ON c.id = x.conciliacion_id
                  JOIN aportes.pago p ON p.id = c.pago_id
                 WHERE x.estado <> 'RESUELTA'
                   AND p.estado = 'ACREDITADO'
                   AND p.fecha_hora_acreditacion >= ?::timestamptz AND p.fecha_hora_acreditacion < ?::timestamptz
                """,
                        desde,
                        hasta)
                .get(0, Integer.class);
    }

    public record PorMoneda(
            String moneda, BigDecimal recaudado, BigDecimal conciliado, BigDecimal excepciones, int pagos) {}
}
