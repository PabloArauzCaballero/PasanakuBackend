package bo.aportaya.plataforma.web.seguridad;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * La señal de seguridad de ADR-030: «esta sesion ya no vale», tal como viaja por el tema compactado
 * {@value #TEMA}. La publica {@code identidad} y la lee cada servicio con {@code comun-web}.
 *
 * <p><b>La clave es la sesion, no el {@code jti}.</b> ADR-030 §1 hablaba de {@code jti}, pero identidad no guarda los
 * {@code jti} que firma: lo que revoca es la fila de {@code sesion}. El token ya lleva {@code sesion_id} firmado
 * ({@code EmisorDeAcceso}), asi que revocar la sesion invalida todos sus tokens de una vez, incluidos los que se
 * emitieron al refrescar.
 *
 * <p><b>{@code venceEn} es hasta cuando hay que recordarla.</b> Pasado ese instante ningun token de la sesion puede
 * seguir vivo —el de acceso dura menos que eso y el refresco ya se niega—, asi que la entrada se puede olvidar. Es lo
 * que mantiene la lista y el tema chicos.
 *
 * <p>La carga lleva identificadores y fechas, nada mas: ni el motivo, ni el dispositivo, ni la IP. Un tema que leen
 * catorce procesos no es lugar para datos de la persona (regla 90.2).
 */
public record RevocacionDeSesion(UUID sesionId, UUID usuarioId, Instant revocadaEn, Instant venceEn) {

    /** El unico tema del sistema que no pasa por el outbox (ADR-030 §1). Compactado, clave = sesion. */
    public static final String TEMA = "aportaya.identidad.revocacion";

    private static final ObjectMapper JSON = new ObjectMapper();

    public RevocacionDeSesion {
        Objects.requireNonNull(sesionId, "sesionId");
        Objects.requireNonNull(usuarioId, "usuarioId");
        Objects.requireNonNull(revocadaEn, "revocadaEn");
        Objects.requireNonNull(venceEn, "venceEn");
        if (venceEn.isBefore(revocadaEn)) {
            throw new IllegalArgumentException("venceEn no puede ser anterior a revocadaEn");
        }
    }

    /** La clave de particion y de compactacion: la ultima palabra sobre una sesion es la que queda. */
    public String clave() {
        return sesionId.toString();
    }

    public String aJson() {
        Map<String, String> cuerpo = new LinkedHashMap<>();
        cuerpo.put("sesionId", sesionId.toString());
        cuerpo.put("usuarioId", usuarioId.toString());
        cuerpo.put("revocadaEn", revocadaEn.toString());
        cuerpo.put("venceEn", venceEn.toString());
        try {
            return JSON.writeValueAsString(cuerpo);
        } catch (JsonProcessingException imposible) {
            throw new IllegalStateException("No se pudo serializar la revocacion", imposible);
        }
    }

    /**
     * Lee el valor de un mensaje del tema. Los campos que no conoce los ignora: el productor puede sumar uno sin
     * coordinar el despliegue de los catorce consumidores (regla 98.1.3, compatibilidad hacia adelante).
     *
     * @throws IllegalArgumentException si el valor no es un objeto o le falta, o trae mal, alguno de los cuatro campos
     */
    public static RevocacionDeSesion desdeJson(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La revocacion llego vacia");
        }
        try {
            JsonNode cuerpo = JSON.readTree(valor);
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("La revocacion no es un objeto");
            }
            return new RevocacionDeSesion(
                    UUID.fromString(texto(cuerpo, "sesionId")),
                    UUID.fromString(texto(cuerpo, "usuarioId")),
                    Instant.parse(texto(cuerpo, "revocadaEn")),
                    Instant.parse(texto(cuerpo, "venceEn")));
        } catch (JsonProcessingException | DateTimeParseException ilegible) {
            throw new IllegalArgumentException("La revocacion no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static String texto(JsonNode cuerpo, String campo) {
        JsonNode nodo = cuerpo.get(campo);
        if (nodo == null || !nodo.isTextual() || nodo.asText().isBlank()) {
            throw new IllegalArgumentException("La revocacion no trae " + campo);
        }
        return nodo.asText();
    }
}
