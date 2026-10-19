package bo.aportaya.nucleofinanciero.dominio.puertos;

import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lo recaudado de un dia, segun {@code aportes} (CU-51, backoffice carril A · H8).
 *
 * <p>Los pagos, sus conciliaciones y sus excepciones viven en {@code aportes} y este servicio no los lee
 * (invariante 11): los pregunta por el contrato {@code GET /aportes/recaudo-diario}. Vacio significa «no se
 * sabe» —el servicio no respondio, respondio mal o fuera de contrato— y quien pregunta NO cierra el dia con
 * un numero inventado (invariante 9).
 */
public interface RecaudoDelDia {

    Optional<Resumen> consultar(LocalDate fecha);

    /** Un total por moneda: sumar monedas distintas esta prohibido (regla 91.1.2). */
    record Resumen(LocalDate fecha, List<PorMoneda> porMoneda, int excepcionesAbiertas) {}

    record PorMoneda(
            Moneda moneda, BigDecimal recaudado, BigDecimal conciliado, BigDecimal excepciones, int cantidadPagos) {}
}
