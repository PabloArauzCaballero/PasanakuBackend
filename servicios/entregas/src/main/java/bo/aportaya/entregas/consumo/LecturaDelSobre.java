package bo.aportaya.entregas.consumo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

/**
 * Lo comun de leer un evento de Kafka: cabeceras del sobre ({@code event_id}, {@code correlation_id}) y campos del
 * cuerpo JSON. Lo que falta se RECHAZA con {@link IllegalArgumentException}: un evento a medias no se adivina.
 */
final class LecturaDelSobre {

    private static final ObjectMapper JSON = new ObjectMapper();

    private LecturaDelSobre() {}

    static JsonNode cuerpo(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = registro.value() == null ? null : JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return cuerpo;
        } catch (JsonProcessingException ilegible) {
            throw new IllegalArgumentException(
                    "El evento no se puede leer: " + ilegible.getOriginalMessage(), ilegible);
        }
    }

    static UUID uuid(JsonNode cuerpo, String campo) {
        return UUID.fromString(texto(cuerpo, campo));
    }

    static String texto(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo) || cuerpo.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return cuerpo.get(campo).asText();
    }

    static int entero(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo) || !cuerpo.get(campo).canConvertToInt()) {
            throw new IllegalArgumentException("El evento no trae " + campo + " entero");
        }
        return cuerpo.get(campo).asInt();
    }

    static UUID cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return UUID.fromString(new String(h.value(), StandardCharsets.UTF_8));
    }
}
