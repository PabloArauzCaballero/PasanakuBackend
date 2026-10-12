package bo.aportaya.grupos.consumo;

import bo.aportaya.grupos.aplicacion.AbrirInscripcionDelGrupo;
import bo.aportaya.grupos.aplicacion.AbrirInscripcionDelGrupo.PrecioCongelado;
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
 * El borde con Kafka de {@link AbrirInscripcionDelGrupo}: escucha {@code tarifas.tarifa_congelada} (CU-20).
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el
 * {@code grupoId} y el {@code tarifarioId}) y se lo entrega. Un mensaje al que le falta una cabecera o un campo, o con
 * un identificador que no es UUID, se RECHAZA (excepcion): perderlo en silencio deja al grupo en borrador para siempre,
 * que es justamente el defecto que este consumidor corrige.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.grupos.apertura.consumo-habilitado}): es el camino normal de CU-20,
 * no una funcion opcional; apagarlo es una decision de operaciones y deja a los grupos nuevos sin poder inscribir. El
 * manejo de errores es el del contenedor de Spring Kafka (reintentos y descarte tras agotarlos); una cola de mensajes
 * muertos con alerta es la decision pendiente de operaciones. Un rechazo NO marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(name = "aportaya.grupos.apertura.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDePreciosCongeladosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDePreciosCongeladosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final AbrirInscripcionDelGrupo apertura;

    public ConsumidorDePreciosCongeladosKafka(AbrirInscripcionDelGrupo apertura) {
        this.apertura = apertura;
    }

    @KafkaListener(topics = "aportaya.tarifas.tarifa_congelada", groupId = "${spring.application.name}-apertura")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        PrecioCongelado precio = leer(registro);
        var resultado = apertura.procesar(precio);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("precio congelado {} (grupo {}) -> {}", precio.eventoId(), precio.grupoId(), resultado);
    }

    static PrecioCongelado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new PrecioCongelado(
                    UUID.fromString(cabecera(registro, "event_id")),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    uuid(cuerpo, "grupoId"),
                    uuid(cuerpo, "tarifarioId"));
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
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
