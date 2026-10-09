package bo.aportaya.garantia.infraestructura;

import bo.aportaya.garantia.dominio.puertos.RecaudoDelPeriodo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code aportes} cuanto del pozo es caja confirmada.
 *
 * <p>El token de quien llama viaja con la pregunta ({@link ClienteDeServicio}): {@code aportes}
 * decide con su propio permiso. Timeout corto y sin reintento: preguntar no es trabajar, y la
 * cobertura se puede reintentar entera porque es idempotente. Si no responde, o responde mal,
 * el resultado es vacio y quien pregunta rechaza (denegar por omision). No hay circuit breaker
 * propio todavia: queda como riesgo residual declarado.
 */
@Component
public class RecaudoPorHttp implements RecaudoDelPeriodo {

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

    private record Faltante(UUID obligacionId, Importe monto) {}

    private record Respuesta(
            UUID periodoId,
            UUID grupoId,
            OffsetDateTime corteEn,
            Importe pozo,
            Importe confirmado,
            Importe cubiertoMutual,
            List<Faltante> pendientes) {

        Recaudo aRecaudo() {
            return new Recaudo(
                    periodoId,
                    grupoId,
                    corteEn,
                    pozo.aDinero(),
                    confirmado.aDinero(),
                    cubiertoMutual.aDinero(),
                    pendientes.stream()
                            .map(p -> new Pendiente(p.obligacionId(), p.monto().aDinero()))
                            .toList());
        }
    }
}
