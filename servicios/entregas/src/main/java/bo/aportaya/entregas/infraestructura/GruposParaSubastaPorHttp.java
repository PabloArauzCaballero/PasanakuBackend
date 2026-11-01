package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.puertos.GruposParaSubasta;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link GruposParaSubasta} por HTTP, bajo el mismo circuito que el resto de las llamadas a {@code grupos}. Solo se
 * registra la operacion y la clase de la falla: ni cuerpos, ni tokens, ni importes (regla 90.2.1).
 */
@Component
public class GruposParaSubastaPorHttp implements GruposParaSubasta {

    private static final Logger BITACORA = LoggerFactory.getLogger(GruposParaSubastaPorHttp.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ClienteConCircuito grupos;
    private final CredencialDeServicio servicio;

    @Autowired
    public GruposParaSubastaPorHttp(
            RestClient.Builder constructor,
            @Value("${aportaya.servicios.grupos}") String urlDeGrupos,
            ConfiguracionDeCircuito circuito,
            CredencialDeServicio servicio) {
        this.grupos = new ClienteConCircuito(constructor, urlDeGrupos, "grupos", circuito);
        this.servicio = servicio;
    }

    @Override
    public Subastabilidad subastabilidad(UUID grupoId, UUID cupoId) {
        try {
            var r = grupos.ejecutar(rest -> rest.get()
                    .uri("/grupos/{g}/cupos/{c}/subastabilidad", grupoId, cupoId)
                    .headers(ClienteDeServicio::propagarElToken)
                    .retrieve()
                    .body(RespuestaSubastabilidad.class));
            if (r == null || r.participanteId() == null || r.montoAporte() == null) {
                return new Subastabilidad(Respuesta.NO_DISPONIBLE, Optional.empty());
            }
            return new Subastabilidad(
                    Respuesta.OK,
                    Optional.of(new Cupo(
                            r.participanteId(),
                            r.subastable(),
                            Optional.ofNullable(r.motivo()),
                            Dinero.de(
                                    r.montoAporte().monto(),
                                    Moneda.valueOf(r.montoAporte().moneda())),
                            r.nombreDelGrupo(),
                            r.periodicidad())));
        } catch (RestClientResponseException rechazo) {
            String codigo = codigoDe(rechazo);
            Respuesta respuesta = "AP-CU64-06".equals(codigo)
                    ? Respuesta.NO_ES_TITULAR
                    : "AP-CU64-01".equals(codigo) ? Respuesta.NO_EXISTE : registrar("subastabilidad", rechazo);
            return new Subastabilidad(respuesta, Optional.empty());
        } catch (RuntimeException noSePudo) {
            return new Subastabilidad(registrar("subastabilidad", noSePudo), Optional.empty());
        }
    }

    @Override
    public Elegibilidad elegibilidad(UUID grupoId) {
        try {
            var r = grupos.ejecutar(rest -> rest.get()
                    .uri("/grupos/{g}/elegibilidad-para-subasta", grupoId)
                    .headers(ClienteDeServicio::propagarElToken)
                    .retrieve()
                    .body(RespuestaElegibilidad.class));
            return r == null
                    ? new Elegibilidad(Respuesta.NO_DISPONIBLE, false, Optional.empty())
                    : new Elegibilidad(Respuesta.OK, r.elegible(), Optional.ofNullable(r.motivo()));
        } catch (RestClientResponseException rechazo) {
            Respuesta respuesta =
                    rechazo.getStatusCode().value() == 422 ? Respuesta.NO_EXISTE : registrar("elegibilidad", rechazo);
            return new Elegibilidad(respuesta, false, Optional.empty());
        } catch (RuntimeException noSePudo) {
            return new Elegibilidad(registrar("elegibilidad", noSePudo), false, Optional.empty());
        }
    }

    @Override
    public Traspaso traspasar(
            UUID grupoId, UUID cupoId, UUID subastaId, UUID ganadorUsuarioId, UUID salienteParticipanteId) {
        Optional<String> token = servicio.vigente();
        if (token.isEmpty()) {
            // Sin credencial de servicio no se mueve un cupo (falla cerrado); el cierre lo reintenta.
            BITACORA.warn("traspasar sin credencial de servicio: no se pide");
            return new Traspaso(Respuesta.NO_DISPONIBLE, Optional.empty());
        }
        try {
            var r = grupos.ejecutar(rest -> rest.post()
                    .uri("/grupos/{g}/cupos/{c}/traspasos-por-subasta", grupoId, cupoId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.get())
                    .header(
                            "Idempotency-Key",
                            llaveDe("subasta:" + subastaId + ":traspaso").toString())
                    .body(Map.of(
                            "subastaId", subastaId.toString(),
                            "ganadorUsuarioId", ganadorUsuarioId.toString(),
                            "salienteParticipanteId", salienteParticipanteId.toString()))
                    .retrieve()
                    .body(RespuestaTraspaso.class));
            return r == null || r.traspasoId() == null
                    ? new Traspaso(Respuesta.NO_DISPONIBLE, Optional.empty())
                    : new Traspaso(Respuesta.OK, Optional.of(r.traspasoId()));
        } catch (RestClientResponseException rechazo) {
            Respuesta respuesta =
                    "AP-CU64-01".equals(codigoDe(rechazo)) ? Respuesta.RECHAZADO : registrar("traspasar", rechazo);
            return new Traspaso(respuesta, Optional.empty());
        } catch (RuntimeException noSePudo) {
            return new Traspaso(registrar("traspasar", noSePudo), Optional.empty());
        }
    }

    static UUID llaveDe(String clave) {
        return UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8));
    }

    /** El codigo de un 422 del contrato de errores, nunca el texto del mensaje. */
    private static String codigoDe(RestClientResponseException rechazo) {
        if (rechazo.getStatusCode().value() != 422) {
            return "";
        }
        try {
            return JSON.readTree(rechazo.getResponseBodyAsString())
                    .path("codigo")
                    .asText();
        } catch (java.io.IOException | RuntimeException ilegible) {
            return "";
        }
    }

    private static Respuesta registrar(String operacion, Exception falla) {
        String que = falla instanceof RestClientResponseException r
                ? "HTTP " + r.getStatusCode().value()
                : falla.getClass().getSimpleName();
        BITACORA.warn("grupos no confirmo {}: {}", operacion, que);
        return Respuesta.NO_DISPONIBLE;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Importe(String monto, String moneda) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaSubastabilidad(
            UUID participanteId,
            boolean subastable,
            String motivo,
            Importe montoAporte,
            String nombreDelGrupo,
            String periodicidad) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaElegibilidad(boolean elegible, String motivo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RespuestaTraspaso(UUID traspasoId, UUID participanteEntranteId) {}
}
