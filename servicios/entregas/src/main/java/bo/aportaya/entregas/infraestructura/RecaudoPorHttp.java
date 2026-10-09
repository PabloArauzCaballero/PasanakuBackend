package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code aportes} cuanto del pozo es caja confirmada.
 *
 * <p>Timeout corto de {@link ClienteDeServicio}, sin reintento y sin respuesta parcial: cualquier
 * falla (red, 4xx, 5xx, JSON roto) es vacio y quien pregunta no avanza (denegar por omision). No
 * hay circuit breaker todavia: riesgo residual declarado.
 */
@Component
public class RecaudoPorHttp implements RecaudoDelPozo {

    private final ClienteDeServicio cliente;

    @Autowired
    public RecaudoPorHttp(RestClient.Builder constructor, @Value("${aportaya.servicios.aportes}") String urlDeAportes) {
        this(new ClienteDeServicio(constructor, urlDeAportes, "aportes"));
    }

    RecaudoPorHttp(ClienteDeServicio cliente) {
        this.cliente = cliente;
    }

    @Override
    public Optional<Recaudo> consultar(UUID periodoId) {
        try {
            return cliente.consultar("/aportes/periodos/" + periodoId + "/recaudo", Respuesta.class)
                    .map(Respuesta::aRecaudo);
        } catch (RuntimeException noSePudo) {
            return Optional.empty();
        }
    }

    private record Importe(String monto, String moneda) {
        Dinero aDinero() {
            return Dinero.de(new BigDecimal(monto), Moneda.valueOf(moneda));
        }
    }

    private record Respuesta(
            UUID periodoId,
            UUID grupoId,
            OffsetDateTime corteEn,
            Importe pozo,
            Importe confirmado,
            Importe cubiertoMutual) {

        Recaudo aRecaudo() {
            return new Recaudo(
                    periodoId, grupoId, corteEn, pozo.aDinero(), confirmado.aDinero(), cubiertoMutual.aDinero());
        }
    }
}
