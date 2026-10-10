package bo.aportaya.grupos.consumo;

import bo.aportaya.grupos.aplicacion.SustitucionAutomatica;
import com.fasterxml.jackson.core.JsonProcessingException;
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
 * El borde con Kafka de {@link SustitucionAutomatica} (H5.S1.M7): escucha {@code organizador.habilitacion_revocada}.
 *
 * <p>No decide nada: lee el sobre del evento (cabeceras {@code event_id} y {@code correlation_id}, cuerpo JSON con el
 * {@code organizadorId}) y se lo entrega. Un mensaje al que le falta una cabecera o el identificador del organizador se
 * RECHAZA (excepcion), no se ignora: perder en silencio una revocacion deja un grupo sin administrador habilitado.
 *
 * <p><b>Apagado por omision</b> ({@code aportaya.grupos.sustitucion.consumo-habilitado}): encenderlo exige que
 * operaciones confirme la configuracion del consumidor de Kafka de este servicio (grupo, reintentos y cola de
 * mensajes muertos). Que el consumo este encendido NO activa la sustitucion automatica: eso lo decide el interruptor
 * {@code aportaya.grupos.sustitucion.automatica}; con el en manual solo se avisa a backoffice.
 */
@Component
@ConditionalOnProperty(name = "aportaya.grupos.sustitucion.consumo-habilitado", havingValue = "true")
public class ConsumidorDeRevocacionesKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeRevocacionesKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final SustitucionAutomatica sustitucion;

    public ConsumidorDeRevocacionesKafka(SustitucionAutomatica sustitucion) {
        this.sustitucion = sustitucion;
    }

    @KafkaListener(
            topics = "aportaya.organizador.habilitacion_revocada",
            groupId = "${spring.application.name}-sustitucion")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        UUID eventoId = UUID.fromString(cabecera(registro, "event_id"));
        UUID correlacion = UUID.fromString(cabecera(registro, "correlation_id"));
        UUID organizadorId = organizadorDe(registro.value());
        var efectos = sustitucion.alRevocarseElOrganizador(eventoId, organizadorId, correlacion);
        // Identificadores y resultados, nunca datos de personas (regla 90.2).
        BITACORA.info("habilitacion revocada {} -> {} grupo(s) procesado(s)", eventoId, efectos.size());
    }

    static UUID organizadorDe(String cuerpo) {
        try {
            var nodo = JSON.readTree(cuerpo);
            if (nodo == null || !nodo.hasNonNull("organizadorId")) {
                throw new IllegalArgumentException("El evento no trae organizadorId");
            }
            return UUID.fromString(nodo.get("organizadorId").asText());
        } catch (JsonProcessingException | IllegalArgumentException ilegible) {
            throw new IllegalArgumentException("El evento no se puede leer: " + ilegible.getMessage(), ilegible);
        }
    }

    private static String cabecera(ConsumerRecord<String, String> registro, String nombre) {
        Header h = registro.headers().lastHeader(nombre);
        if (h == null || h.value() == null) {
            throw new IllegalArgumentException("Falta la cabecera " + nombre);
        }
        return new String(h.value(), StandardCharsets.UTF_8);
    }
}
