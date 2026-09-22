package bo.aportaya.plataforma.mensajeria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * El envelope: 8 cabeceras desde las columnas y {@code metadatos} de {@code evento_dominio}
 * (H2.S2.M4, contrato en {@code docs/auditoria-produccion/contratos/evento-kafka.md}).
 *
 * <p>Separado de {@link Relevo} solo por tamano (regla de las 300 lineas, H2.S3.M4) — es la misma
 * pieza de siempre, construir-el-mensaje-a-partir-de-la-fila, movida a su propio archivo.
 */
final class EnvelopeDeEvento {

    private static final Logger BITACORA = LoggerFactory.getLogger(EnvelopeDeEvento.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private EnvelopeDeEvento() {}

    static Message<String> construir(Record evento, String esquema) {
        UUID id = (UUID) evento.get("id");
        String tipo = String.valueOf(evento.get("tipo"));
        UUID agregadoId = (UUID) evento.get("agregado_id");
        String payload = String.valueOf(evento.get("payload"));
        UUID correlationId = (UUID) evento.get("correlation_id");
        UUID causationId = (UUID) evento.get("causation_id");
        OffsetDateTime ocurridoEn = evento.get("ocurrido_en", OffsetDateTime.class);
        String metadatos = String.valueOf(evento.get("metadatos"));

        var builder = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, "aportaya." + tipo)
                // La clave de particion es el agregado: lo de un mismo agregado llega en orden.
                .setHeader(KafkaHeaders.KEY, agregadoId.toString())
                .setHeader("event_id", id.toString())
                .setHeader("type", tipo)
                .setHeader("version", "1")
                .setHeader("occurred_at", ocurridoEn.toInstant().toString())
                .setHeader("producer", esquema)
                .setHeader("correlation_id", correlationId.toString())
                .setHeader("trace_id", trazaDe(metadatos, correlationId));
        if (causationId != null) {
            builder.setHeader("causation_id", causationId.toString());
        }
        return builder.build();
    }

    /**
     * {@code trace_id} sale del MDC de {@code Traza} al momento de publicar — pero el relevo
     * corre en un hilo programado, sin peticion en curso, asi que ahi nunca hay nada. Se lee de
     * {@code metadatos.trace_id} (lo que la peticion original guardo al emitir, H4.S2.M2) y, a
     * falta de eso todavia (el emisor no lo escribe hasta H4), se usa {@code correlation_id}: la
     * cabecera nunca falta, aunque la traza real de punta a punta se cierre recien en H4.
     */
    private static String trazaDe(String metadatosJson, UUID correlationId) {
        try {
            JsonNode nodo = JSON.readTree(metadatosJson).get("trace_id");
            if (nodo != null && !nodo.isNull()) {
                return nodo.asText();
            }
        } catch (Exception e) {
            BITACORA.debug("metadatos sin trace_id parseable, se usa correlation_id", e);
        }
        return correlationId.toString();
    }
}
