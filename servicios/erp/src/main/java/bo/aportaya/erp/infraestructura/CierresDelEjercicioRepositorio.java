package bo.aportaya.erp.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Los periodos de un ejercicio con su cierre (backoffice carril A · H7): solo lectura y solo del
 * esquema {@code erp}. Los totales son los sellados en {@code cierre_periodo_contable}; no se suma el
 * libro de {@code nucleo_financiero} para listar (esa lectura cruzada queda acotada al cierre).
 */
@Component
public class CierresDelEjercicioRepositorio {

    public Optional<Ejercicio> ejercicio(DSLContext dsl, UUID ejercicioId) {
        return Optional.ofNullable(dsl.fetchOne(
                        """
                SELECT id, anio, estado FROM erp.ejercicio_fiscal WHERE id = ?
                """,
                        ejercicioId))
                .map(f -> new Ejercicio(
                        f.get("id", UUID.class), f.get("anio", Short.class).intValue(), f.get("estado", String.class)));
    }

    public List<PeriodoConCierre> periodos(DSLContext dsl, UUID ejercicioId) {
        return dsl.fetch(
                        """
                SELECT p.id, p.mes, p.estado, p.fecha_inicio, p.fecha_fin, c.cerrado_en, c.total_debe, c.total_haber
                  FROM erp.periodo_contable p
                  LEFT JOIN erp.cierre_periodo_contable c ON c.periodo_contable_id = p.id
                 WHERE p.ejercicio_fiscal_id = ?
                 ORDER BY p.mes
                """,
                        ejercicioId)
                .map(f -> new PeriodoConCierre(
                        f.get("id", UUID.class),
                        f.get("mes", Short.class).intValue(),
                        f.get("estado", String.class),
                        f.get("fecha_inicio", LocalDate.class),
                        f.get("fecha_fin", LocalDate.class),
                        f.get("cerrado_en", OffsetDateTime.class),
                        f.get("total_debe", BigDecimal.class),
                        f.get("total_haber", BigDecimal.class)));
    }

    public record Ejercicio(UUID id, int anio, String estado) {}

    public record PeriodoConCierre(
            UUID id,
            int mes,
            String estado,
            LocalDate desde,
            LocalDate hasta,
            OffsetDateTime cerradoEn,
            BigDecimal totalDebe,
            BigDecimal totalHaber) {}
}
