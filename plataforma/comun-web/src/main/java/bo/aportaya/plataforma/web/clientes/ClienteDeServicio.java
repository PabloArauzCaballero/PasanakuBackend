package bo.aportaya.plataforma.web.clientes;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Como un servicio le pregunta algo a otro.
 *
 * <p><b>Se pregunta, no se lee la base.</b> Los hechos de otro servicio viven en su
 * esquema y ese esquema no se toca (invariante 11): se piden por su contrato. Y se
 * piden **fuera de la transaccion** — una llamada de red adentro deja la transaccion
 * abierta esperando a un tercero, que es el invariante 6.
 *
 * <p>El token del usuario viaja en la llamada. No es comodidad: el servicio de destino
 * decide con la politica de fila de quien pregunta, asi que preguntar sin token
 * devolveria lo que ve el sistema y no lo que ve el titular. Si no hay token —un
 * trabajo programado— la llamada sale sin el y el destino aplica sus propias reglas.
 *
 * <p><b>Todo tiene tiempo maximo.</b> Un cliente sin timeout no falla: cuelga, y con el
 * cuelga el hilo que lo llamo. Cuando el destino no responde a tiempo, esta clase
 * devuelve vacio en vez de propagar la excepcion: quien pregunta decide si eso es un
 * rechazo o un valor por omision, y esa decision es de negocio, no del transporte.
 */
public class ClienteDeServicio {

    private final RestClient rest;
    private final String destino;

    public ClienteDeServicio(RestClient.Builder constructor, String urlBase, String destino) {
        this.rest = constructor.baseUrl(urlBase).build();
        this.destino = destino;
    }

    /**
     * Una consulta al servicio de destino.
     *
     * @return el cuerpo, o vacio si el destino no lo tiene, no responde o falla. Lo que
     *     significa ese vacio lo decide quien pregunta.
     */
    public <T> Optional<T> consultar(String ruta, Class<T> tipo) {
        return blindar(() -> rest.get()
                .uri(ruta)
                .headers(this::conElTokenDeQuienPregunta)
                .retrieve()
                .body(tipo));
    }

    /** Un pedido con cuerpo, para cuando la consulta no cabe en una URL. */
    public <T> Optional<T> pedir(String ruta, Object cuerpo, Class<T> tipo) {
        return blindar(() -> rest.post()
                .uri(ruta)
                .headers(this::conElTokenDeQuienPregunta)
                .body(cuerpo)
                .retrieve()
                .body(tipo));
    }

    private static final org.slf4j.Logger BITACORA = org.slf4j.LoggerFactory.getLogger(ClienteDeServicio.class);

    private <T> Optional<T> blindar(Supplier<T> llamada) {
        try {
            return Optional.ofNullable(llamada.get());
        } catch (ResourceAccessException seCayo) {
            // Timeout o red caida: loguear para trazabilidad del fallo
            BITACORA.warn("Fallo de red o timeout al llamar al servicio {}", destino, seCayo);
            return Optional.empty();
        } catch (org.springframework.web.client.RestClientResponseException respondioMal) {
            if (respondioMal.getStatusCode().value() == 404) {
                // 404 es recurso ausente: retorno vacio legitimo
                return Optional.empty();
            }
            if (respondioMal.getStatusCode().is4xxClientError()) {
                BITACORA.warn(
                        "El servicio {} respondio error de cliente {}: {}",
                        destino,
                        respondioMal.getStatusCode(),
                        respondioMal.getResponseBodyAsString());
                return Optional.empty();
            }
            throw new ErrorDeDominio("El servicio %s respondio %s: %s".formatted(
                    destino, respondioMal.getStatusCode(), respondioMal.getResponseBodyAsString()));
        }
    }

    /** El token y la traza de quien pregunta, para que el destino aplique SU politica de fila y correlacione logs. */
    public static void propagarElToken(HttpHeaders cabeceras) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt token) {
            cabeceras.setBearerAuth(token.getTokenValue());
        }
        propagarTraza(cabeceras);
    }

    /**
     * El usuario (reclamo {@code sub}) del token de la peticion en curso, o vacio si no hay sesion con un JWT (un trabajo
     * programado). Es lo que un adaptador mira para no actuar en nombre de una persona distinta de la que esta
     * autenticada: la identidad sale del token verificado, nunca de lo que diga el cuerpo.
     */
    public static Optional<String> usuarioDeLaSesion() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        return autenticacion != null && autenticacion.getPrincipal() instanceof Jwt token
                ? Optional.ofNullable(token.getSubject())
                : Optional.empty();
    }

    private void conElTokenDeQuienPregunta(HttpHeaders cabeceras) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt token) {
            cabeceras.setBearerAuth(token.getTokenValue());
        }
        propagarTraza(cabeceras);
    }

    private static void propagarTraza(HttpHeaders cabeceras) {
        String traza = bo.aportaya.plataforma.web.traza.Traza.actual();
        if (traza == null || traza.isBlank() || "sin-traza".equals(traza)) {
            traza = java.util.UUID.randomUUID().toString();
        }
        cabeceras.set(bo.aportaya.plataforma.web.traza.Traza.CABECERA, traza);
    }

    /** El tiempo maximo que se espera a otro servicio. Corto: preguntar no es trabajar. */
    public static Duration tiempoMaximo() {
        return Duration.ofSeconds(3);
    }
}
