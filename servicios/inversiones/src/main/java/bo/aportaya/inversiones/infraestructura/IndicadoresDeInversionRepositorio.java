package bo.aportaya.inversiones.infraestructura;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

/**
 * Tableros · el saldo invertido al cierre de un dia: el principal de las posiciones ya constituidas y todavia no
 * cerradas a esa fecha. Se reconstruye a cualquier fecha. Hoy el aliado es un SIMULADO (ver README): el numero es
 * sintetico hasta que exista un aliado real.
 */
@Repository
public class IndicadoresDeInversionRepositorio {

    public record Suma(BigDecimal total, long casos) {}

    public Suma invertido(DSLContext dsl, LocalDate al, String moneda) {
        Record r = dsl.fetchOne(
                """
                SELECT COALESCE(SUM(principal), 0), count(*) FROM inversiones.posicion_inversion
                 WHERE moneda = ? AND fecha_constitucion <= ?
                   AND (cerrada_en IS NULL OR (cerrada_en AT TIME ZONE 'America/La_Paz')::date > ?)
                """,
                moneda,
                al,
                al);
        return new Suma(r.get(0, BigDecimal.class), r.get(1, Long.class));
    }
}
