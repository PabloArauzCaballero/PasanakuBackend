package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
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
 * <p>Tiempo maximo y circuit breaker ({@link ClienteConCircuito}), sin reintento y sin respuesta parcial:
 * cualquier falla (red, 4xx, 5xx, JSON roto, circuito abierto) es vacio y quien pregunta no avanza (denegar por
 * omision). FALLBACK declarado: con {@code aportes} caido el pozo no se puede afirmar, asi que la entrega NO se
 * liquida (el usuario ve «procesando», regla 98.4.1); NO se usa el ultimo recaudo conocido (decision de
 * Tesoreria pendiente, ver evidencia/carril-S2/DEUDA-circuit-breaker-clientes-http.md).
 */
@Component
public class RecaudoPorHttp implements RecaudoDelPozo {

    private final ClienteConCircuito cliente;

    @Autowired
    public RecaudoPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.aportes}") String urlDeAportes,
            ConfiguracionDeCircuito circuito) {
        this.cliente = new ClienteConCircuito(constructor, urlDeAportes, "aportes", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    RecaudoPorHttp(RestClient.Builder constructor, String urlDeAportes) {
        this(constructor, urlDeAportes, ConfiguracionDeCircuito.porOmision());
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia `aportes`. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return cliente.estado();
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
