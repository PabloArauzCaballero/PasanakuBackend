package bo.aportaya.grupos.consumo;

import bo.aportaya.grupos.aplicacion.AplicarCesionDeDerecho;
import bo.aportaya.grupos.aplicacion.AplicarCesionDeDerecho.Cesion;
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
 * El borde con Kafka de {@link AplicarCesionDeDerecho}: escucha {@code entregas.derecho_cedido}.
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con
 * {@code turnoId}, {@code grupoId}, {@code cupoId}, {@code participanteOrigenId} y {@code participanteDestinoId}, mas el
 * id de la cesion, que el relevo pone como clave del registro) y se lo entrega. Un mensaje
 * al que le falta una cabecera o un campo se RECHAZA (excepcion): perder en silencio una cesion deja al comprador
 * pagando por un cupo que no es suyo.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.grupos.cesion.consumo-habilitado}): la venta ya movio el dinero en
 * otro servicio, asi que apagarlo es una decision de operaciones, no un valor por omision. El manejo de errores es el del
 * contenedor de Spring Kafka (reintentos y descarte tras agotarlos); una cola de mensajes muertos con alerta es la decision
 * pendiente de operaciones. Mientras tanto un rechazo NO marca el evento como consumido: reprocesar el tema repite solo lo
 * que falto.
 */
@Component
@ConditionalOnProperty(name = "aportaya.grupos.cesion.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDeCesionesKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeCesionesKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AplicarCesionDeDerecho aplicar;

    public ConsumidorDeCesionesKafka(AplicarCesionDeDerecho aplicar) {
        this.aplicar = aplicar;
    }

    @KafkaListener(topics = "aportaya.entregas.derecho_cedido", groupId = "${spring.application.name}-cesiones")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        Cesion cesion = leer(registro);
        var salida = aplicar.procesar(cesion);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("derecho cedido {} (cesion {}) -> {}", cesion.eventoId(), cesion.cesionId(), salida.resultado());
    }

    static Cesion leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new Cesion(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    agregado(registro),
                    uuid(cuerpo, "turnoId"),
                    uuid(cuerpo, "grupoId"),
                    uuid(cuerpo, "cupoId"),
                    uuid(cuerpo, "participanteOrigenId"),
                    uuid(cuerpo, "participanteDestinoId"));
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    /** El relevo usa el agregado (la cesion) como clave del registro: es lo que garantiza el orden por cesion. */
    private static UUID agregado(ConsumerRecord<String, String> registro) {
        if (registro.key() == null) {
            throw new IllegalArgumentException("El registro no trae la clave (el id de la cesion)");
        }
        return UUID.fromString(registro.key());
    }

    private static UUID uuid(JsonNode cuerpo, String campo) {
        if (!cuerpo.hasNonNull(campo)) {
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
