package bo.aportaya.nucleofinanciero.consumo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

/**
 * El sobre de un evento del outbox: cabeceras {@code event_id} y {@code correlation_id} y un cuerpo JSON. Lo comparten
 * los consumidores de apertura de billeteras. Un sobre al que le falta una cabecera o un campo, o con un identificador que
 * no es UUID, se RECHAZA (excepcion): ignorarlo en silencio deja a una persona o a un grupo sin billetera.
 */
final class SobreDeEvento {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final ConsumerRecord<String, String> registro;
    private final JsonNode cuerpo;

    private SobreDeEvento(ConsumerRecord<String, String> registro, JsonNode cuerpo) {
        this.registro = registro;
        this.cuerpo = cuerpo;
    }

    static SobreDeEvento de(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new SobreDeEvento(registro, cuerpo);
        } catch (JsonProcessingException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    UUID eventoId() {
        return UUID.fromString(cabecera("event_id"));
    }

    UUID correlationId() {
        return UUID.fromString(cabecera("correlation_id"));
    }

    UUID uuid(String campo) {
        if (!cuerpo.hasNonNull(campo) || cuerpo.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return UUID.fromString(cuerpo.get(campo).asText());
    }

    private String cabecera(String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
