package bo.aportaya.nucleofinanciero.infraestructura.clientes;

import bo.aportaya.nucleofinanciero.dominio.puertos.OfertasDelMercado;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code entregas} quien vende, a que precio y con que cargos. Falla CERRADA: sin respuesta, con una
 * respuesta ilegible, sin los cargos o con un importe mal formado devuelve vacio y el pago no se hace. Pagarle a
 * quien dice el cliente porque {@code entregas} estaba caido es justo lo que este puerto impide.
 */
@Component
public class OfertasPorHttp implements OfertasDelMercado {

    private final ClienteDeServicio entregas;

    public OfertasPorHttp(RestClient.Builder constructor, @Value("${aportaya.servicios.entregas}") String urlEntregas) {
        this.entregas = new ClienteDeServicio(constructor, urlEntregas, "entregas");
    }

    @Override
    public Optional<Oferta> oferta(UUID ofertaId) {
        try {
            return entregas.consultar("/entregas/mercado/ofertas/" + ofertaId, Respuesta.class)
                    .filter(r -> ofertaId.equals(r.ofertaId())
                            && r.vendedorUsuarioId() != null
                            && r.precio() != null
                            && r.cargos() != null)
                    .map(r -> new Oferta(
                            ofertaId,
                            r.vendedorUsuarioId(),
                            Dinero.de(
                                    r.precio().monto(),
                                    Moneda.valueOf(r.precio().moneda())),
                            Dinero.de(
                                    r.cargos().monto(),
                                    Moneda.valueOf(r.cargos().moneda())),
                            r.estado()));
        } catch (RuntimeException ilegible) {
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Respuesta(UUID ofertaId, UUID vendedorUsuarioId, Importe precio, Importe cargos, String estado) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Importe(String monto, String moneda) {}
}
