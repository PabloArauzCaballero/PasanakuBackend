package bo.aportaya.aportes.infraestructura.clientes;

import bo.aportaya.aportes.aplicacion.HechosDeGrupos;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le pregunta a `grupos` por su contrato, con el token de quien paga (el destino aplica SU politica de
 * fila). Si grupos no contesta, vacio: para dinero eso se rechaza, no se asume que si. Con tiempo maximo y
 * circuit breaker ({@link ClienteConCircuito}); con el circuito abierto la respuesta es la misma (vacio).
 */
@Component
public class HechosDeGruposPorHttp implements HechosDeGrupos {
    private final ClienteConCircuito grupos;

    @Autowired
    public HechosDeGruposPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.grupos}") String url,
            ConfiguracionDeCircuito circuito) {
        this.grupos = new ClienteConCircuito(constructor, url, "grupos-admisibilidad", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    HechosDeGruposPorHttp(RestClient.Builder constructor, String url) {
        this(constructor, url, ConfiguracionDeCircuito.porOmision());
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia `grupos`. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return grupos.estado();
    }

    @Override
    public Optional<Admisibilidad> admisibilidad(UUID participanteId, UUID periodoId) {
        return grupos.consultar(
                        "/grupos/obligaciones/admisibilidad?participanteId=" + participanteId + "&periodoId="
                                + periodoId,
                        Respuesta.class)
                .map(r -> new Admisibilidad(r.esDelUsuario(), r.periodoAbierto()));
    }

    private record Respuesta(boolean esDelUsuario, boolean periodoAbierto) {}
}
