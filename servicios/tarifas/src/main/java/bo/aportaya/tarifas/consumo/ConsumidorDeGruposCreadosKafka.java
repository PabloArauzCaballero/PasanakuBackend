package bo.aportaya.tarifas.consumo;

import bo.aportaya.tarifas.aplicacion.CongelarTarifaDelGrupo;
import bo.aportaya.tarifas.aplicacion.CongelarTarifaDelGrupo.GrupoCreado;
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
 * El borde con Kafka de {@link CongelarTarifaDelGrupo}: escucha {@code grupos.grupo_creado} (CU-20).
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el
 * {@code grupoId} y el {@code tarifarioId}) y se lo entrega. Un mensaje al que le falta una cabecera o un campo, o con
 * un identificador que no es UUID, se RECHAZA (excepcion): perderlo en silencio deja a un grupo sin precio congelado y
 * en borrador para siempre.
 *
 * <p><b>Encendido por omision</b> ({@code aportaya.congelamiento.consumo-habilitado}): es el camino normal de la
 * creacion de un grupo; apagarlo es una decision de operaciones. El manejo de errores es el del contenedor de Spring
 * Kafka (reintentos y descarte tras agotarlos); una cola de mensajes muertos con alerta es la decision pendiente de
 * operaciones. Un rechazo NO marca el evento como consumido.
 */
@Component
@ConditionalOnProperty(name = "aportaya.congelamiento.consumo-habilitado", havingValue = "true", matchIfMissing = true)
public class ConsumidorDeGruposCreadosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeGruposCreadosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final CongelarTarifaDelGrupo congelar;

    public ConsumidorDeGruposCreadosKafka(CongelarTarifaDelGrupo congelar) {
        this.congelar = congelar;
    }

    @KafkaListener(topics = "aportaya.grupos.grupo_creado", groupId = "${spring.application.name}-congelamiento")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        GrupoCreado grupo = leer(registro);
        var resultado = congelar.procesar(grupo);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("grupo creado {} (grupo {}) -> {}", grupo.eventoId(), grupo.grupoId(), resultado);
    }

    static GrupoCreado leer(ConsumerRecord<String, String> registro) {
        try {
            JsonNode cuerpo = JSON.readTree(registro.value());
            if (cuerpo == null || !cuerpo.isObject()) {
                throw new IllegalArgumentException("El cuerpo del evento no es un objeto");
            }
            return new GrupoCreado(
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
