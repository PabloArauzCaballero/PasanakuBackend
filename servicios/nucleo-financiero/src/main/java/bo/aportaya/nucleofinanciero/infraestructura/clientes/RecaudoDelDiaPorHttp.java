package bo.aportaya.nucleofinanciero.infraestructura.clientes;

import bo.aportaya.nucleofinanciero.dominio.puertos.RecaudoDelDia;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code aportes} lo recaudado de un dia, con el token de quien cierra.
 *
 * <p>Tiempo maximo de {@link ClienteDeServicio}, sin reintento y sin respuesta parcial: red caida, 4xx, 5xx,
 * JSON roto, otra fecha, una moneda desconocida o un importe negativo son vacio, y el dia NO se cierra
 * (FALLBACK declarado: denegar; no se usa ningun total conocido de antes).
 */
@Component
public class RecaudoDelDiaPorHttp implements RecaudoDelDia {

    private final ClienteDeServicio aportes;

    public RecaudoDelDiaPorHttp(RestClient.Builder constructor, @Value("${aportaya.servicios.aportes}") String url) {
        this.aportes = new ClienteDeServicio(constructor, url, "aportes");
    }

    @Override
    public Optional<Resumen> consultar(LocalDate fecha) {
        try {
            return aportes.consultar("/aportes/recaudo-diario?fecha=" + fecha, Respuesta.class)
                    .filter(r -> fecha.equals(r.fecha()))
                    .map(Respuesta::aResumen);
        } catch (RuntimeException fueraDeContrato) {
            // 5xx, cuerpo ilegible, moneda desconocida o importe negativo: no se sabe. Nunca un cero inventado.
            return Optional.empty();
        }
    }

    private record TotalesDeUnaMoneda(
            String moneda,
            String totalRecaudado,
            String totalConciliado,
            String totalExcepciones,
            Integer cantidadPagos) {

        PorMoneda aPorMoneda() {
            return new PorMoneda(
                    Moneda.valueOf(moneda),
                    noNegativo(totalRecaudado),
                    noNegativo(totalConciliado),
                    noNegativo(totalExcepciones),
                    cantidadPagos);
        }
    }

    private record Respuesta(LocalDate fecha, List<TotalesDeUnaMoneda> porMoneda, Integer excepcionesAbiertas) {

        Resumen aResumen() {
            if (porMoneda == null || excepcionesAbiertas == null || excepcionesAbiertas < 0) {
                throw new IllegalArgumentException("resumen incompleto");
            }
            return new Resumen(
                    fecha,
                    porMoneda.stream().map(TotalesDeUnaMoneda::aPorMoneda).toList(),
                    excepcionesAbiertas);
        }
    }

    private static BigDecimal noNegativo(String monto) {
        BigDecimal valor = new BigDecimal(monto);
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("importe negativo");
        }
        return valor;
    }
}
