package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.TitularDeLaBilletera;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a {@code identidad} si el titular declarado es el de la billetera.
 *
 * <p>Cuando {@code identidad} no responde, la respuesta es <b>no</b>. No es pesimismo:
 * es denegar por omision (invariante 9). Dejar registrar una cuenta porque el servicio
 * que verifica estaba caido es exactamente el hueco por el que entra una cuenta ajena.
 *
 * <p>Con tiempo maximo y circuit breaker ({@link ClienteConCircuito}); con el circuito abierto el veredicto
 * es el mismo «no» sin esperar la red.
 */
@Component
public class TitularPorHttp implements TitularDeLaBilletera {

    private final ClienteConCircuito identidad;

    @Autowired
    public TitularPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.identidad}") String urlDeIdentidad,
            ConfiguracionDeCircuito circuito) {
        this.identidad = new ClienteConCircuito(constructor, urlDeIdentidad, "identidad", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    TitularPorHttp(RestClient.Builder constructor, String urlDeIdentidad) {
        this(constructor, urlDeIdentidad, ConfiguracionDeCircuito.porOmision());
    }

    @Override
    public boolean esElMismo(UUID usuarioId, String nombreDeclarado, String documentoDeclarado) {
        try {
            var respuesta = identidad.ejecutar(rest -> rest.post()
                    .uri("/usuarios/{id}/titularidad/verificacion", usuarioId)
                    .headers(ClienteDeServicio::propagarElToken)
                    .body(Map.of("nombreCompleto", nombreDeclarado, "documento", documentoDeclarado))
                    .retrieve()
                    .body(Veredicto.class));
            return respuesta != null && respuesta.coincide();
        } catch (RuntimeException noSePudo) {
            return false;
        }
    }

    private record Veredicto(boolean coincide) {}
}
