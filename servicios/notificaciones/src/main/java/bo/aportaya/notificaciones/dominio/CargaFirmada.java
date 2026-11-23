package bo.aportaya.notificaciones.dominio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.UUID;

/**
 * CU-82 · Lo que el proveedor de mensajeria FIRMO. Puro.
 *
 * <p>F1.L5.M23: antes la firma cubria solo {@code cargaUtil}, y el remitente, el contenido y la clave de
 * idempotencia viajaban aparte, sin firmar: un par (carga, firma) capturado se reenviaba con otro remitente —y
 * daba de baja o «confirmaba un pago» a nombre de otra persona— o con otra clave, para procesarlo de nuevo. Ahora
 * todo sale de la carga firmada, que es JSON con este formato:
 *
 * <pre>{"idMensaje":"...","canal":"SMS","remitente":"...","contenido":"...",
 *  "emitidoEn":"2026-11-21T12:00:00Z","notificacionRelacionadaId":"(opcional, uuid)"}</pre>
 *
 * <p>{@code emitidoEn} permite rechazar la repeticion de un mensaje viejo ({@link #dentroDeVentana}); el
 * {@code idMensaje} es la clave de deduplicacion (una sola vez aunque llegue dos).
 */
public record CargaFirmada(
        String idMensaje,
        String canal,
        String remitente,
        String contenido,
        Instant emitidoEn,
        Optional<UUID> notificacionRelacionadaId) {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** La carga, o vacio si no es JSON, le falta un campo obligatorio o uno no tiene formato. */
    public static Optional<CargaFirmada> leer(String cargaUtil) {
        if (cargaUtil == null) {
            return Optional.empty();
        }
        try {
            JsonNode n = JSON.readTree(cargaUtil);
            if (n == null || !n.isObject()) {
                return Optional.empty();
            }
            String id = texto(n, "idMensaje");
            String canal = texto(n, "canal");
            String remitente = texto(n, "remitente");
            String contenido = texto(n, "contenido");
            String emitido = texto(n, "emitidoEn");
            if (id == null || canal == null || remitente == null || contenido == null || emitido == null) {
                return Optional.empty();
            }
            String relacionada = texto(n, "notificacionRelacionadaId");
            return Optional.of(new CargaFirmada(
                    id,
                    canal,
                    remitente,
                    contenido,
                    Instant.parse(emitido),
                    Optional.ofNullable(relacionada).map(UUID::fromString)));
        } catch (java.io.IOException | DateTimeParseException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Emitido a no mas de {@code ventana} de {@code ahora}, para atras o para adelante (reloj desfasado). */
    public boolean dentroDeVentana(Instant ahora, Duration ventana) {
        return !emitidoEn.isBefore(ahora.minus(ventana)) && !emitidoEn.isAfter(ahora.plus(ventana));
    }

    private static String texto(JsonNode n, String campo) {
        JsonNode v = n.get(campo);
        return v == null || v.isNull() || !v.isTextual() || v.asText().isBlank() ? null : v.asText();
    }
}
