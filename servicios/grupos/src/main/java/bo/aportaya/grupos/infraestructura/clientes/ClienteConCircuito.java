package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Como este servicio le pregunta algo a otro con tiempo maximo Y circuit breaker (regla 98.2.1 y 98.2.3).
 *
 * <p>Hace lo que {@link ClienteDeServicio} y le agrega el segundo escalon: despues de una ventana de fallos
 * seguidos deja de llamar un rato, en vez de que cada pedido espere su propio timeout contra un dependido
 * caido. {@link ClienteDeServicio} no se puede usar de base porque se traga la falla de red en un
 * {@code Optional.empty()} y el breaker nunca la veria; aca el breaker envuelve la llamada y la clasifica.
 *
 * <p>Que cuenta como FALLO del dependido: red caida o timeout, respuesta 5xx y cuerpo ilegible. Un 4xx es
 * una respuesta del negocio (no existe, no te corresponde) y NO abre el circuito.
 *
 * <p>FALLBACK declarado (falla cerrado): con el circuito abierto, la red caida o un 4xx la respuesta es
 * {@link Optional#empty()}, y quien pregunta deniega o no avanza; un 5xx se propaga como
 * {@link ErrorDeDominio} (misma conducta que {@link ClienteDeServicio}) y un cuerpo ilegible como la excepcion
 * del cliente. Nunca se supone una respuesta buena. Los parametros salen de la configuracion
 * ({@link ConfiguracionDeCircuito}) y son SINTETICOS: los fija el dueno de cada servicio con su trafico real.
 *
 * <p>El token de quien pregunta viaja en la llamada (como en {@link ClienteDeServicio}).
 */
public final class ClienteConCircuito {

    private static final Logger BITACORA = LoggerFactory.getLogger(ClienteConCircuito.class);

    private final RestClient rest;
    private final String destino;
    private final CircuitBreaker circuito;

    public ClienteConCircuito(
            RestClient.Builder constructor, String urlBase, String destino, ConfiguracionDeCircuito configuracion) {
        this.rest = constructor.baseUrl(urlBase).build();
        this.destino = destino;
        this.circuito = CircuitBreaker.of(
                "cliente-" + destino,
                CircuitBreakerConfig.custom()
                        .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(configuracion.ventana())
                        // Hasta no llenar la ventana no se juzga: un fallo aislado no es una caida.
                        .minimumNumberOfCalls(configuracion.ventana())
                        .failureRateThreshold(configuracion.umbralDeFallos())
                        .waitDurationInOpenState(configuracion.esperaAbierto())
                        .permittedNumberOfCallsInHalfOpenState(configuracion.llamadasDePrueba())
                        .recordException(ClienteConCircuito::cuentaComoFallo)
                        .build());
        // Solo el destino y la transicion: nada de lo que viaja en la llamada (regla 90.2.1).
        this.circuito
                .getEventPublisher()
                .onStateTransition(
                        evento -> BITACORA.warn("circuito hacia {}: {}", this.destino, evento.getStateTransition()));
    }

    /** Un GET al dependido; vacio si no responde, esta el circuito abierto o contesta 4xx. */
    public <T> Optional<T> consultar(String ruta, Class<T> tipo) {
        return protegida(() -> rest.get()
                .uri(ruta)
                .headers(ClienteDeServicio::propagarElToken)
                .retrieve()
                .body(tipo));
    }

    /**
     * Una CONSULTA cuyo dato personal no puede viajar en la URL (regla 90.2.2): un POST con el contacto en el cuerpo y
     * sin {@code Idempotency-Key}, porque no escribe nada. Mismas reglas que {@link #consultar}.
     */
    public <T> Optional<T> consultarPorCuerpo(String ruta, Object cuerpo, Class<T> tipo) {
        return protegida(() -> rest.post()
                .uri(ruta)
                .headers(ClienteDeServicio::propagarElToken)
                .body(cuerpo)
                .retrieve()
                .body(tipo));
    }

    /** Un POST con clave de idempotencia al dependido; mismas reglas que {@link #consultar}. */
    public <T> Optional<T> pedir(String ruta, Object cuerpo, String claveDeIdempotencia, Class<T> tipo) {
        return protegida(() -> rest.post()
                .uri(ruta)
                .headers(ClienteDeServicio::propagarElToken)
                .header("Idempotency-Key", claveDeIdempotencia)
                .body(cuerpo)
                .retrieve()
                .body(tipo));
    }

    /**
     * Una llamada a medida (otro metodo, otras cabeceras) bajo el mismo circuito. A diferencia de
     * {@link #consultar}, la falla SE PROPAGA: con el circuito abierto sale un {@link ResourceAccessException},
     * igual que una red caida, para que quien llama la trate como tal.
     */
    public <T> T ejecutar(Function<RestClient, T> llamada) {
        try {
            return circuito.executeSupplier(() -> llamada.apply(rest));
        } catch (CallNotPermittedException abierto) {
            throw new ResourceAccessException("circuito abierto hacia " + destino);
        }
    }

    /** Para pruebas y diagnostico: CLOSED, OPEN o HALF_OPEN. */
    public CircuitBreaker.State estado() {
        return circuito.getState();
    }

    private <T> Optional<T> protegida(Supplier<T> llamada) {
        try {
            return Optional.ofNullable(circuito.executeSupplier(llamada));
        } catch (CallNotPermittedException abierto) {
            return Optional.empty();
        } catch (ResourceAccessException seCayo) {
            return Optional.empty();
        } catch (RestClientResponseException respondio) {
            if (respondio.getStatusCode().is4xxClientError()) {
                return Optional.empty();
            }
            throw new ErrorDeDominio("El servicio %s respondio %s".formatted(destino, respondio.getStatusCode()));
        }
    }

    /** Red, timeout, 5xx y cuerpo ilegible son del dependido; un 4xx es una respuesta suya, valida. */
    private static boolean cuentaComoFallo(Throwable falla) {
        if (falla instanceof RestClientResponseException respuesta) {
            return respuesta.getStatusCode().is5xxServerError();
        }
        return falla instanceof RestClientException;
    }
}
