package bo.aportaya.inversiones.infraestructura.clientes;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ClienteDeServicio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * El libro de la billetera por su contrato HTTP ({@code nucleo-financiero}).
 *
 * <p>Degradacion declarada (regla 98.2.4): con el libro caido NO se invierte. Cada
 * operacion falla cerrada con {@link LibroNoDisponible}; las que ya tienen intencion
 * persistida ({@code instruccion_libro}) quedan pendientes y se reintentan con la MISMA
 * clave, de modo que repetirlas no mueva plata dos veces. Un 422 por saldo insuficiente es
 * el unico rechazo que se traduce a una decision; cualquier otra respuesta que no sea un
 * exito se trata como «no se sabe» y queda para revision.
 *
 * <p>El contrato de retencion y de saldo existe en {@code nucleo-financiero}; el debito
 * de inversion, el credito de rescate, el motivo {@code INVERSION_VOLUNTARIA} y el campo
 * {@code comprometidoEnPozos} son lo que ESTE servicio le pide al carril del nucleo
 * ({@code evidencia/carril-D/contrato-requerido-nucleo.md}). Hasta que existan en el
 * productor, esta clase solo esta probada contra el consumidor.
 */
@Component
public class LibroPorHttp implements LibroDelTitular {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SALDO_INSUFICIENTE = "AP-CU13-01";

    private final URI base;
    private final Duration timeout;
    private final CredencialDeServicio credencial;
    private final HttpClient http;
    // Un 422 de negocio es una respuesta sana del libro: no abre el circuito.
    private final CircuitBreaker cortacircuitos = CircuitBreaker.of(
            "libro-del-titular",
            CircuitBreakerConfig.custom()
                    // Tecnico, no comercial: se abre cuando la mitad de las ultimas veinte llamadas fallo.
                    .slidingWindowSize(20)
                    .minimumNumberOfCalls(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .ignoreExceptions(SaldoInsuficiente.class)
                    .recordExceptions(LibroNoDisponible.class)
                    .build());

    /** Sin credencial de servicio: solo para pruebas del adaptador; debitar y acreditar fallan cerrado. */
    public LibroPorHttp(URI base, Duration timeout) {
        this(base, timeout, CredencialDeServicio.ninguna());
    }

    /** Con un token ya emitido: pruebas del adaptador. */
    public LibroPorHttp(URI base, Duration timeout, String tokenDeServicio) {
        this(base, timeout, CredencialDeServicio.fija(tokenDeServicio));
    }

    @Autowired
    public LibroPorHttp(
            @Value("${aportaya.servicios.nucleo-financiero:http://nucleo-financiero:8080}") URI base,
            @Value("${aportaya.inversiones.libro.timeout:PT3S}") Duration timeout,
            CredencialDeServicio credencial) {
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("El timeout debe ser positivo.");
        }
        this.base = base;
        this.timeout = timeout;
        this.credencial = credencial;
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public SaldoDelTitular saldo(UUID cuentaId) {
        JsonNode r =
                enviar(pedir("/billetera/" + cuentaId + "/saldo", false).GET().build());
        if (r.path("comprometidoEnPozos").isMissingNode() || r.path("titularId").isMissingNode()) {
            // Sin saber de quien es la cuenta ni cuanto esta afectado a un pozo, no se invierte «por las dudas».
            throw new LibroNoDisponible("El libro no informa el titular ni lo comprometido en pozos");
        }
        return new SaldoDelTitular(
                UUID.fromString(r.path("titularId").asText()),
                dinero(r.path("disponible")),
                dinero(r.path("retenido")),
                dinero(r.path("comprometidoEnPozos")));
    }

    @Override
    public UUID retener(UUID cuentaId, Dinero monto, String clave, UUID referenciaOrden) {
        Map<String, Object> cuerpo = Map.of(
                "cuentaBilleteraId", cuentaId.toString(),
                "monto",
                        Map.of(
                                "monto",
                                monto.monto().toPlainString(),
                                "moneda",
                                monto.moneda().name()),
                "motivo", "INVERSION_VOLUNTARIA",
                "referenciaTipo", "ORDEN_INVERSION",
                "referenciaId", referenciaOrden.toString());
        return UUID.fromString(enviar(post("/billetera/retenciones", clave, cuerpo, false))
                .path("retencionId")
                .asText());
    }

    @Override
    public void liberar(UUID retencionId, String clave) {
        enviar(post(
                "/billetera/retenciones/" + retencionId + "/cierre", clave, Map.of("desenlace", "LIBERADA"), false));
    }

    @Override
    public UUID debitarInversion(UUID retencionId, Dinero monto, String clave) {
        Map<String, Object> cuerpo = Map.of(
                "retencionId", retencionId.toString(),
                "monto",
                        Map.of(
                                "monto",
                                monto.monto().toPlainString(),
                                "moneda",
                                monto.moneda().name()));
        return UUID.fromString(enviar(post("/billetera/inversiones/debitos", clave, cuerpo, true))
                .path("transaccionId")
                .asText());
    }

    @Override
    public UUID acreditarRescate(UUID cuentaId, Dinero monto, String clave, UUID referenciaRescate) {
        Map<String, Object> cuerpo = Map.of(
                "cuentaBilleteraId", cuentaId.toString(),
                "monto",
                        Map.of(
                                "monto",
                                monto.monto().toPlainString(),
                                "moneda",
                                monto.moneda().name()),
                "referenciaId", referenciaRescate.toString());
        return UUID.fromString(enviar(post("/billetera/inversiones/acreditaciones", clave, cuerpo, true))
                .path("transaccionId")
                .asText());
    }

    // ------------------------------------------------------------------ mecanica
    private HttpRequest.Builder pedir(String ruta, boolean soloServicio) {
        HttpRequest.Builder b =
                HttpRequest.newBuilder(base.resolve(ruta)).timeout(timeout).header("Accept", "application/json");
        String autorizacion = autorizacion(soloServicio);
        if (autorizacion != null) {
            b.header("Authorization", autorizacion);
        }
        return b;
    }

    /**
     * Quien firma la llamada. Debitar la inversion y acreditar el rescate mueven plata hacia y
     * desde la custodia: el libro las acepta SOLO con identidad de servicio, nunca con el token de
     * una persona (que ademas no existe cuando las reintenta el reloj). Sin credencial de servicio
     * configurada no se intenta: falla cerrado. El resto de las operaciones lleva el token de quien
     * pregunta, como en los demas clientes, y si no hay sesion (un reintento del reloj) la
     * credencial de servicio, si esta configurada. El valor de la credencial no se registra jamas.
     */
    private String autorizacion(boolean soloServicio) {
        if (!soloServicio) {
            HttpHeaders token = new HttpHeaders();
            ClienteDeServicio.propagarElToken(token);
            String delUsuario = token.getFirst(HttpHeaders.AUTHORIZATION);
            if (delUsuario != null) {
                return delUsuario;
            }
        }
        var delServicio = credencial.vigente();
        if (delServicio.isEmpty()) {
            if (soloServicio) {
                throw new LibroNoDisponible("No hay credencial de servicio para mover plata hacia o desde la custodia");
            }
            return null;
        }
        return "Bearer " + delServicio.get();
    }

    private HttpRequest post(String ruta, String clave, Map<String, Object> cuerpo, boolean soloServicio) {
        try {
            return pedir(ruta, soloServicio)
                    // El libro pide un UUID; se deriva de la clave de la instruccion y es estable.
                    .header(
                            "Idempotency-Key",
                            UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8))
                                    .toString())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cuerpo)))
                    .build();
        } catch (IOException e) {
            throw new LibroNoDisponible("No se pudo armar el pedido", e);
        }
    }

    private JsonNode enviar(HttpRequest peticion) {
        try {
            return cortacircuitos.executeSupplier(() -> sinProteccion(peticion));
        } catch (CallNotPermittedException abierto) {
            throw new LibroNoDisponible("El libro esta marcado como no disponible");
        }
    }

    private JsonNode sinProteccion(HttpRequest peticion) {
        try {
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            int s = r.statusCode();
            if (s >= 200 && s < 300) {
                return JSON.readTree(r.body());
            }
            if (s == 422
                    && SALDO_INSUFICIENTE.equals(
                            JSON.readTree(r.body()).path("codigo").asText())) {
                throw new SaldoInsuficiente();
            }
            throw new LibroNoDisponible("El libro respondio " + s);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LibroNoDisponible("Interrumpido", e);
        } catch (IOException e) {
            throw new LibroNoDisponible("Sin respuesta del libro", e);
        }
    }

    private static Dinero dinero(JsonNode n) {
        return Dinero.de(
                n.path("monto").asText(), Moneda.valueOf(n.path("moneda").asText()));
    }
}
