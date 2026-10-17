package bo.aportaya.garantia.consumo;

import bo.aportaya.garantia.aplicacion.IngresoAlFondo;
import bo.aportaya.garantia.aplicacion.IngresoAlFondo.TransaccionReversada;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * El borde con Kafka de {@link IngresoAlFondo#revertir}: escucha {@code nucleo_financiero.transaccion_reversada}. Casi
 * ningun reverso es de un recargo por mora; esos pasan sin escribir nada. Un mensaje mal formado se RECHAZA.
 */
@Component
@ConditionalOnProperty(
        name = "aportaya.garantia.consumo-de-aportes-aplicados.habilitado",
        havingValue = "true",
        matchIfMissing = true)
public class ConsumidorDeReversosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeReversosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final IngresoAlFondo ingreso;

    public ConsumidorDeReversosKafka(IngresoAlFondo ingreso) {
        this.ingreso = ingreso;
    }

    @KafkaListener(
            topics = "aportaya.nucleo_financiero.transaccion_reversada",
            groupId = "${spring.application.name}-reversos")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        TransaccionReversada reverso = leer(registro);
        var resultado = ingreso.revertir(reverso);
        if (resultado != IngresoAlFondo.Resultado.NO_ES_DEL_FONDO) {
            BITACORA.info(
                    "reverso {} (original {}) -> {}", reverso.eventoId(), reverso.transaccionOriginalId(), resultado);
        }
    }

    static TransaccionReversada leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new TransaccionReversada(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "transaccionOriginalId"),
                    uuid(cuerpo, "transaccionReversoId"));
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static UUID uuid(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo) || cuerpo.get(campo).asText().isBlank()) {
            throw new IllegalArgumentException("El evento no trae " + campo);
        }
        return UUID.fromString(cuerpo.get(campo).asText());
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
