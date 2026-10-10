package bo.aportaya.inversiones.infraestructura.clientes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * La credencial de servicio obtenida de identidad con el ingreso de siempre ({@code POST /sesiones}, CU-04): la cuenta
 * de servicio entra como operador (telefono, credencial y segundo factor) y recibe el MISMO token RS256 que valida
 * cualquier servicio contra el JWKS. No hay un segundo emisor ni un secreto compartido entre servicios (98.6).
 *
 * <p>El token vive 15 minutos: se renueva cuando faltan {@link #MARGEN}. Un solo hilo renueva; los demas esperan el
 * resultado. Si identidad no responde o rechaza, NO se reintenta antes de {@link #ESPERA_TRAS_FALLO}: cada intento
 * fallido cuenta para el bloqueo de la cuenta (DEMASIADOS_INTENTOS) y un bucle de reintentos la bloquearia. Mientras
 * tanto la respuesta es vacia y el libro no se toca (falla cerrado). Ni la clave, ni el codigo, ni el token se registran.
 */
public final class CredencialPorLogin implements CredencialDeServicio {

    static final Duration MARGEN = Duration.ofSeconds(90);
    static final Duration ESPERA_TRAS_FALLO = Duration.ofSeconds(30);
    private static final Duration VIDA_SI_NO_DECLARA = Duration.ofMinutes(5);
    private static final Logger BITACORA = LoggerFactory.getLogger(CredencialPorLogin.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final URI identidad;
    private final Duration timeout;
    private final String telefono;
    private final String clave;
    private final Function<Instant, String> factor;
    private final Clock reloj;
    private final HttpClient http;

    private String token;
    private Instant venceElToken = Instant.MIN;
    private Instant noAntesDe = Instant.MIN;

    /** @param factor el segundo factor para un instante: un TOTP calculado o el codigo fijo de TEST. */
    public CredencialPorLogin(
            URI identidad,
            Duration timeout,
            String telefono,
            String clave,
            Function<Instant, String> factor,
            Clock reloj) {
        this.identidad = identidad;
        this.timeout = timeout;
        this.telefono = telefono;
        this.clave = clave;
        this.factor = factor;
        this.reloj = reloj;
        this.http = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public static Function<Instant, String> totp(String semillaBase32) {
        return ahora -> CodigoTotp.de(semillaBase32, ahora);
    }

    public static Function<Instant, String> fijo(String codigo) {
        return ahora -> codigo;
    }

    @Override
    public synchronized Optional<String> vigente() {
        Instant ahora = reloj.instant();
        if (token != null && ahora.isBefore(venceElToken.minus(MARGEN))) {
            return Optional.of(token);
        }
        if (ahora.isBefore(noAntesDe)) {
            return Optional.empty();
        }
        try {
            ingresar(ahora);
            return Optional.ofNullable(token);
        } catch (RuntimeException | IOException e) {
            token = null;
            noAntesDe = ahora.plus(ESPERA_TRAS_FALLO);
            BITACORA.warn(
                    "No se pudo obtener la credencial de servicio de identidad: {}",
                    e.getClass().getSimpleName());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private void ingresar(Instant ahora) throws IOException, InterruptedException {
        String cuerpo = JSON.writeValueAsString(Map.of(
                "telefonoE164",
                telefono,
                "credencial",
                clave,
                "huellaDispositivo",
                "servicio-inversiones",
                "plataforma",
                "WEB",
                "factor",
                Map.of("tipo", "TOTP", "valor", factor.apply(ahora))));
        HttpRequest pedido = HttpRequest.newBuilder(identidad.resolve("/sesiones"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        HttpResponse<String> r = http.send(pedido, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            throw new IllegalStateException("identidad respondio " + r.statusCode());
        }
        JsonNode salida = JSON.readTree(r.body());
        String emitido = salida.path("tokenAcceso").asText("");
        if (emitido.isBlank()) {
            // Pidio un factor adicional o no abrio sesion: sin token no hay credencial.
            throw new IllegalStateException("identidad no entrego token de acceso");
        }
        token = emitido;
        venceElToken = vencimientoDe(emitido, ahora);
    }

    /** El {@code exp} del propio token (sin verificarlo: lo verifica quien lo recibe); si no lo trae, un plazo corto. */
    static Instant vencimientoDe(String jwt, Instant ahora) {
        try {
            String[] partes = jwt.split("\\.");
            JsonNode carga = JSON.readTree(Base64.getUrlDecoder().decode(partes[1]));
            if (carga.hasNonNull("exp")) {
                return Instant.ofEpochSecond(carga.get("exp").asLong());
            }
        } catch (IOException | RuntimeException ilegible) {
            // cae al plazo corto
        }
        return ahora.plus(VIDA_SI_NO_DECLARA);
    }
}
