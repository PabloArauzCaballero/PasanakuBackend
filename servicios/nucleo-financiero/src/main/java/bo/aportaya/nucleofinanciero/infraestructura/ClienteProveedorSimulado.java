package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor;
import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * El cliente del contrato ficticio local, comun a recargas y retiros.
 *
 * <p>Sin configuracion no confirma nada. Se distinguen dos clases de falla y la
 * diferencia es la que importa: que el proveedor <b>no conteste</b> (timeout, 5xx, corte)
 * deja el resultado desconocido y se traduce a {@link ErrorDeNegocio}; que conteste algo
 * <b>que no se puede creer</b> (firma que no coincide, referencia ajena, estado
 * contradictorio) lanza {@link DiscrepanciaDelProveedor}, que quien coordina registra.
 */
final class ClienteProveedorSimulado {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int CARACTERES_DE_RESPUESTA_ADMITIDOS = 1 << 14; // 16 KiB: un recibo firmado no se acerca
    private static final String MONTO_DECIMAL = "(?:0|[1-9][0-9]{0,11})\\.[0-9]{2}";

    /** Lo que el proveedor firmo, ya verificado. */
    record Recibo(
            UUID referencia, UUID transaccionProveedor, Dinero monto, String estado, OffsetDateTime liquidadaEn) {}

    private final String modo;
    private final URI base;
    private final String apiKey;
    private final byte[] secreto;
    private final HttpClient http;
    private final Duration timeout;
    private final String tipo;

    ClienteProveedorSimulado(
            String tipo, String modo, String ambiente, URI base, String apiKey, String secreto, Duration timeout) {
        if (!Set.of("deshabilitado", "simulado").contains(modo)) {
            throw new IllegalArgumentException("El adaptador real necesita su propia certificacion y contrato.");
        }
        if ("simulado".equals(modo)
                && (!"simulado".equals(ambiente)
                        || !Set.of("127.0.0.1", "localhost").contains(base.getHost())
                        || !"http".equals(base.getScheme())
                        || base.getRawUserInfo() != null
                        || base.getRawQuery() != null
                        || base.getRawFragment() != null
                        || apiKey.length() < 32
                        || secreto.getBytes(StandardCharsets.UTF_8).length < 32)) {
            throw new IllegalArgumentException(
                    "El proveedor ficticio exige ambiente simulado, loopback y claves locales.");
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("El timeout debe ser positivo.");
        }
        this.tipo = tipo;
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

    void enviar(UUID referencia, Dinero monto) {
        exigirHabilitado();
        try {
            String cuerpo = JSON.writeValueAsString(Map.of(
                    "referencia", referencia.toString(),
                    "tipo", tipo,
                    "monto", monto.monto().toPlainString(),
                    "moneda", monto.moneda().name(),
                    "escenario", "PENDIENTE"));
            HttpRequest peticion = peticion("/v1/operaciones")
                    .header("Idempotency-Key", referencia.toString())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                    .build();
            verificar(transportar(peticion).orElseThrow(ClienteProveedorSimulado::noConfirmado), referencia);
        } catch (IOException e) {
            throw noConfirmado();
        }
    }

    /** El recibo verificado, o vacio si el proveedor responde que no conoce la referencia. */
    Optional<Recibo> consultar(UUID referencia) {
        exigirHabilitado();
        return transportar(peticion("/v1/operaciones/" + referencia).GET().build())
                .map(cuerpo -> verificar(cuerpo, referencia));
    }

    private HttpRequest.Builder peticion(String ruta) {
        return HttpRequest.newBuilder(base.resolve(ruta))
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Accept", "application/json");
    }

    /** El cuerpo de un 2xx; vacio ante un 404; cualquier otra cosa es «no se pudo saber». */
    private Optional<String> transportar(HttpRequest peticion) {
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 404) {
                return Optional.empty();
            }
            if (respuesta.statusCode() < 200
                    || respuesta.statusCode() >= 300
                    || respuesta.body().length() > CARACTERES_DE_RESPUESTA_ADMITIDOS) {
                throw noConfirmado();
            }
            return Optional.of(respuesta.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw noConfirmado();
        } catch (IOException e) {
            throw noConfirmado();
        }
    }

    private Recibo verificar(String respuesta, UUID referencia) {
        String huella = sha256(respuesta);
        JsonNode sobre = leer(respuesta, huella);
        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(sobre.path("payload").asText());
        } catch (IllegalArgumentException e) {
            throw new DiscrepanciaDelProveedor(Tipo.RESPUESTA_INVALIDA, huella, "El payload no es Base64.");
        }
        exigirFirma(raw, sobre.path("firma").asText(), huella);
        JsonNode payload = leer(new String(raw, StandardCharsets.UTF_8), huella);
        try {
            Instant emitido = Instant.parse(payload.path("emitidoEn").asText());
            if (!referencia.toString().equals(payload.path("referencia").asText())) {
                throw new DiscrepanciaDelProveedor(
                        Tipo.REFERENCIA_DISTINTA, huella, "El recibo firmado corresponde a otra referencia.");
            }
            String estado = payload.path("estado").asText();
            if (!payload.path("simulado").asBoolean(false)
                    || !tipo.equals(payload.path("tipo").asText())
                    || !payload.path("monto").isTextual()
                    || !payload.path("monto").asText().matches(MONTO_DECIMAL)
                    || Duration.between(emitido, Instant.now()).abs().compareTo(Duration.ofMinutes(5)) > 0
                    || !Set.of("PENDIENTE", "CONFIRMADO", "RECHAZADO").contains(estado)) {
                throw new DiscrepanciaDelProveedor(
                        Tipo.RESPUESTA_INVALIDA,
                        huella,
                        "El recibo no cumple el contrato (tipo, monto, frescura o estado).");
            }
            OffsetDateTime liquidada = payload.path("liquidadaEn").isNull()
                            || payload.path("liquidadaEn").isMissingNode()
                    ? null
                    : OffsetDateTime.parse(payload.path("liquidadaEn").asText());
            // Un CONFIRMADO sin fecha de liquidacion, o una liquidacion en un estado que no
            // liquida, es el proveedor contradiciendose a si mismo: no se interpreta.
            if (("CONFIRMADO".equals(estado)) != (liquidada != null)) {
                throw new DiscrepanciaDelProveedor(
                        Tipo.ESTADO_CONTRADICTORIO, huella, "El estado y la fecha de liquidacion se contradicen.");
            }
            return new Recibo(
                    referencia,
                    UUID.fromString(payload.path("transaccionProveedor").asText()),
                    Dinero.de(
                            payload.path("monto").asText(),
                            Moneda.valueOf(payload.path("moneda").asText())),
                    estado,
                    liquidada);
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new DiscrepanciaDelProveedor(
                    Tipo.RESPUESTA_INVALIDA, huella, "Un campo del recibo no se puede leer.");
        }
    }

    private void exigirFirma(byte[] raw, String firmaHex, String huella) {
        try {
            byte[] firma = HexFormat.of().parseHex(firmaHex);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(raw), firma)) {
                throw new DiscrepanciaDelProveedor(Tipo.FIRMA_INVALIDA, huella, "La firma del recibo no coincide.");
            }
        } catch (IllegalArgumentException e) {
            throw new DiscrepanciaDelProveedor(Tipo.FIRMA_INVALIDA, huella, "La firma del recibo no es hexadecimal.");
        } catch (GeneralSecurityException e) {
            throw noConfirmado();
        }
    }

    private static JsonNode leer(String texto, String huella) {
        try {
            JsonNode nodo = JSON.readTree(texto);
            if (nodo == null || !nodo.isObject()) {
                throw new IOException("no es un objeto");
            }
            return nodo;
        } catch (IOException e) {
            throw new DiscrepanciaDelProveedor(Tipo.RESPUESTA_INVALIDA, huella, "La respuesta no es JSON valido.");
        }
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("Toda JVM trae SHA-256", imposible);
        }
    }

    private void exigirHabilitado() {
        if (!"simulado".equals(modo)) {
            throw noConfirmado();
        }
    }

    static ErrorDeNegocio noConfirmado() {
        return new ErrorDeNegocio(
                CodigoError.de(10, 5),
                "El proveedor no confirmo la operacion. Consulta su estado antes de reintentar.");
    }
}
