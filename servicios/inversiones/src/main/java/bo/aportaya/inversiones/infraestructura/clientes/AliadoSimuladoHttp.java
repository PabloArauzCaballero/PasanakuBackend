package bo.aportaya.inversiones.infraestructura.clientes;

import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptador del aliado SIMULADO ({@code herramientas/aliado_simulado}). No es un banco ni
 * una SAFI: es un contrato interno de simulacion.
 *
 * <p>Tres cosas que no se negocian:
 *
 * <ul>
 *   <li><b>Se verifica la firma de TODA respuesta</b> (HMAC-SHA256 sobre los bytes
 *       originales) y su frescura. Una respuesta que no se puede verificar no confirma
 *       nada: es {@link AliadoNoDisponible}, jamas un exito.
 *   <li><b>Timeout explicito</b> en conexion y en peticion (regla 98.2.1).
 *   <li><b>Sin configuracion no confirma nada</b>: el modo por omision es
 *       {@code deshabilitado}, y {@code GuardiaDeProduccion} no deja arrancar el modo
 *       {@code simulado} en un entorno productivo.
 * </ul>
 */
@Component
public class AliadoSimuladoHttp implements AliadoDeInversion {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration FRESCURA = Duration.ofMinutes(5);
    private static final int MAX_BYTES_RESPUESTA = 65_536;

    private final String modo;
    private final URI base;
    private final String apiKey;
    private final byte[] secreto;
    private final Duration timeout;
    private final HttpClient http;
    // Un rechazo del aliado es una respuesta sana: no cuenta para abrir el circuito.
    private final CircuitBreaker cortacircuitos = CircuitBreaker.of(
            "aliado-de-inversion",
            CircuitBreakerConfig.custom()
                    // Tecnico, no comercial: se abre cuando la mitad de las ultimas veinte llamadas fallo.
                    .slidingWindowSize(20)
                    .minimumNumberOfCalls(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .ignoreExceptions(AliadoRechazo.class)
                    .recordExceptions(AliadoNoDisponible.class)
                    .build());

    public AliadoSimuladoHttp(
            @Value("${aportaya.inversiones.aliado.modo:deshabilitado}") String modo,
            @Value("${aportaya.inversiones.aliado.url:http://127.0.0.1:4030}") URI base,
            @Value("${aportaya.inversiones.aliado.api-key:}") String apiKey,
            @Value("${aportaya.inversiones.aliado.firma-secreto:}") String secreto,
            @Value("${aportaya.inversiones.aliado.timeout:PT5S}") Duration timeout) {
        if (!Set.of("deshabilitado", "simulado").contains(modo)) {
            throw new IllegalArgumentException(
                    "El adaptador real del aliado necesita su propio contrato y certificacion.");
        }
        if ("simulado".equals(modo)
                && (!Set.of("127.0.0.1", "localhost").contains(base.getHost())
                        || !"http".equals(base.getScheme())
                        || base.getRawUserInfo() != null
                        || apiKey.length() < 32
                        || secreto.getBytes(StandardCharsets.UTF_8).length < 32)) {
            throw new IllegalArgumentException(
                    "El aliado ficticio exige loopback, http y claves locales de 32 bytes o mas.");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("El timeout debe ser positivo.");
        }
        this.modo = modo;
        this.base = base;
        this.apiKey = apiKey;
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.timeout = timeout;
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // ------------------------------------------------------------------ contrato
    @Override
    public List<ProductoDelAliado> catalogo() {
        return LecturaDeRespuestas.productos(
                verificado(enviar(pedir("/v1/productos").GET().build()).cuerpo()));
    }

    @Override
    public Optional<ValorDeCuotaDelAliado> valorDeCuota(String codigoProducto) {
        Respuesta r = enviar(
                pedir("/v1/productos/" + codigoProducto + "/valor-cuota").GET().build());
        if (r.estado() == 404) {
            return Optional.empty();
        }
        JsonNode p = verificado(r.cuerpo());
        return Optional.of(new ValorDeCuotaDelAliado(
                p.path("producto").asText(),
                LocalDate.parse(p.path("fecha").asText()),
                new BigDecimal(p.path("valor").asText()),
                Instant.parse(p.path("publicadoEn").asText())));
    }

    @Override
    public OperacionDelAliado suscribir(UUID referencia, String codigoProducto, BigDecimal monto, UUID titularRef) {
        return operacion(
                "/v1/suscripciones",
                referencia,
                Map.of(
                        "referencia", referencia.toString(),
                        "producto", codigoProducto,
                        "monto", monto.toPlainString(),
                        "moneda", "BOB",
                        "titularRef", titularRef.toString()),
                referencia);
    }

    @Override
    public OperacionDelAliado rescatar(UUID referencia, String posicionExterna, Optional<BigDecimal> cuotas) {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("referencia", referencia.toString());
        cuerpo.put("posicion", posicionExterna);
        cuotas.ifPresent(c -> cuerpo.put("cuotas", c.toPlainString()));
        return operacion("/v1/rescates", referencia, cuerpo, referencia);
    }

    @Override
    public Optional<OperacionDelAliado> consultar(UUID referencia) {
        Respuesta r = enviar(pedir("/v1/operaciones/" + referencia).GET().build());
        if (r.estado() == 404) {
            return Optional.empty();
        }
        return Optional.of(LecturaDeRespuestas.operacion(verificado(r.cuerpo()), referencia));
    }

    // ------------------------------------------------------------------ mecanica
    private OperacionDelAliado operacion(String ruta, UUID clave, Map<String, Object> cuerpo, UUID referencia) {
        try {
            HttpRequest peticion = pedir(ruta)
                    .header("Idempotency-Key", clave.toString())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(cuerpo)))
                    .build();
            return LecturaDeRespuestas.operacion(verificado(enviar(peticion).cuerpo()), referencia);
        } catch (IOException e) {
            throw new AliadoNoDisponible("No se pudo armar el pedido");
        }
    }

    private HttpRequest.Builder pedir(String ruta) {
        exigirHabilitado();
        return HttpRequest.newBuilder(base.resolve(ruta))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json");
    }

    private void exigirHabilitado() {
        if (!"simulado".equals(modo)) {
            throw new AliadoNoDisponible("El aliado de inversion no esta habilitado en este entorno.");
        }
    }

    private record Respuesta(int estado, String cuerpo) {}

    /**
     * 2xx y 404 vuelven; 4xx es un rechazo definitivo (el aliado no registro nada); todo lo
     * demas —timeout, corte de conexion, 5xx, respuesta enorme— es «no se sabe que paso».
     */
    private Respuesta enviar(HttpRequest peticion) {
        try {
            return cortacircuitos.executeSupplier(() -> enviarSinProteccion(peticion));
        } catch (CallNotPermittedException abierto) {
            // Degradacion declarada: con el aliado marcado como caido no se intenta; la
            // orden queda INCIERTA y se consulta cuando el circuito vuelva a cerrarse.
            throw new AliadoNoDisponible("El aliado esta marcado como no disponible");
        }
    }

    private Respuesta enviarSinProteccion(HttpRequest peticion) {
        try {
            HttpResponse<String> r = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            int s = r.statusCode();
            if (r.body().length() > MAX_BYTES_RESPUESTA) {
                throw new AliadoNoDisponible("Respuesta desmesurada");
            }
            if (s >= 200 && s < 300 || s == 404 && peticion.method().equals("GET")) {
                return new Respuesta(s, r.body());
            }
            if (s == 401 || s == 403 || s >= 500) {
                throw new AliadoNoDisponible("El aliado respondio " + s);
            }
            throw new AliadoRechazo(codigoDe(r.body()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AliadoNoDisponible("Interrumpido");
        } catch (IOException e) {
            throw new AliadoNoDisponible("Sin respuesta del aliado");
        }
    }

    private static String codigoDe(String cuerpo) {
        try {
            String codigo = JSON.readTree(cuerpo).path("codigo").asText("");
            return codigo.matches("[A-Z0-9_]{1,60}") ? codigo : "RECHAZO_DEL_ALIADO";
        } catch (IOException e) {
            return "RECHAZO_DEL_ALIADO";
        }
    }

    /** Firma y frescura; solo despues se interpreta el contenido. */
    private JsonNode verificado(String sobre) {
        try {
            JsonNode nodo = JSON.readTree(sobre);
            byte[] raw = Base64.getDecoder().decode(nodo.path("payload").asText());
            byte[] firma = HexFormat.of().parseHex(nodo.path("firma").asText());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(raw), firma)) {
                throw new AliadoNoDisponible("Firma invalida");
            }
            JsonNode payload = JSON.readTree(raw);
            Instant emitido = Instant.parse(payload.path("emitidoEn").asText());
            if (!payload.path("simulado").asBoolean(false)
                    || Duration.between(emitido, Instant.now()).abs().compareTo(FRESCURA) > 0) {
                throw new AliadoNoDisponible("Respuesta fuera de ventana o no marcada como simulada");
            }
            return payload;
        } catch (IOException | IllegalArgumentException | GeneralSecurityException | java.time.DateTimeException e) {
            throw new AliadoNoDisponible("Respuesta no verificable");
        }
    }
}
