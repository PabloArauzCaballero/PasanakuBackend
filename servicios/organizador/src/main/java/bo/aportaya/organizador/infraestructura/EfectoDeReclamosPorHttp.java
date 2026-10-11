package bo.aportaya.organizador.infraestructura;

import bo.aportaya.organizador.dominio.EfectoDeReclamos;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * El efecto de los reclamos de una persona, preguntado a {@code cumplimiento} por su contrato
 * ({@code consultarEfectoDeReclamos}). Es el adaptador real de la lectura de referencia de
 * {@code EfectoDeReclamosDeEnsayo}: PROCEDE solo con un 200 entendible, con el indicador pedido en {@code false}, la
 * lista de casos presente y NINGUN caso con el indicador en {@code true}; todo lo demas FRENA (servicio caido,
 * circuito abierto, 4xx incluido el 403 de quien no tiene {@code RECLAMO_VER}, 5xx, JSON ilegible, tipos
 * equivocados, respuesta de otra persona, respuesta lenta).
 *
 * <p>El token de quien sanciona viaja en la llamada: hoy no hay un token de servicio con alcance acotado (decision
 * abierta de seguridad, ver el contrato). Quien sanciona aca es {@code ADMIN_PLATAFORMA}, que si tiene
 * {@code RECLAMO_VER}. Tiempo maximo y circuit breaker ({@link ClienteConCircuito}); sin reintento: la consulta no
 * escribe y se repite sola en el proximo pedido.
 */
@Component
public class EfectoDeReclamosPorHttp implements EfectoDeReclamos {

    private final ClienteConCircuito cumplimiento;

    @Autowired
    public EfectoDeReclamosPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.cumplimiento}") String urlDeCumplimiento,
            ConfiguracionDeCircuito circuito) {
        this.cumplimiento = new ClienteConCircuito(constructor, urlDeCumplimiento, "cumplimiento", circuito);
    }

    /** Con los parametros del plan de resiliencia: la usan las pruebas del adaptador. */
    EfectoDeReclamosPorHttp(RestClient.Builder constructor, String urlDeCumplimiento) {
        this(constructor, urlDeCumplimiento, ConfiguracionDeCircuito.porOmision());
    }

    /** Para pruebas y diagnostico: el estado del circuito hacia {@code cumplimiento}. */
    io.github.resilience4j.circuitbreaker.CircuitBreaker.State estadoDelCircuito() {
        return cumplimiento.estado();
    }

    @Override
    public Decision consultar(UUID usuarioId, Que que) {
        try {
            return cumplimiento
                    .consultar("/reclamos/usuarios/" + usuarioId + "/efecto", JsonNode.class)
                    .map(cuerpo -> leer(cuerpo, usuarioId, que))
                    .orElse(Decision.FRENA);
        } catch (RuntimeException noSePudo) {
            return Decision.FRENA;
        }
    }

    /** La lectura de referencia del contrato, sobre un cuerpo ya parseado. */
    static Decision leer(JsonNode cuerpo, UUID usuarioId, Que que) {
        String campo = que == Que.SANCION_FIRME ? "bloqueaSancionFirme" : "bloqueaDeudaEjecutable";
        JsonNode agregado = cuerpo.path(campo);
        JsonNode casos = cuerpo.path("casos");
        if (!agregado.isBoolean() || !casos.isArray() || agregado.asBoolean()) {
            return Decision.FRENA;
        }
        // La respuesta tiene que ser de la persona por la que se pregunto: otra cosa no es interpretable.
        if (cuerpo.hasNonNull("usuarioId")
                && !usuarioId.toString().equals(cuerpo.get("usuarioId").asText())) {
            return Decision.FRENA;
        }
        // Defensa en profundidad: aunque el agregado diga que no, un solo caso que frena manda.
        for (JsonNode caso : casos) {
            JsonNode porCaso = caso.path(campo);
            if (!porCaso.isBoolean() || porCaso.asBoolean()) {
                return Decision.FRENA;
            }
        }
        return Decision.PROCEDE;
    }
}
