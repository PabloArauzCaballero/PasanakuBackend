package bo.aportaya.nucleofinanciero.consumo;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos;
import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.EventoRecibido;
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
 * El borde con Kafka de {@link ConsumoDeAsientos}: lee el sobre del evento y se lo entrega.
 *
 * <p>No decide nada: el envelope de {@code Relevo} trae {@code event_id}, {@code type} y
 * {@code correlation_id} en cabeceras y el payload JSON del hecho en el cuerpo. Un mensaje al que le falta
 * una cabecera o cuyo cuerpo no es JSON se RECHAZA (excepcion) en vez de ignorarse: perder en silencio
 * un asiento es el peor de los fallos de un libro.
 *
 * <p><b>Apagado por omision</b> ({@code aportaya.contabilidad.consumo-de-asientos.habilitado}): sin el
 * mapeo de cuentas logicas a codigos del plan (decision de contabilidad, ambiguedad A5) cada evento
 * seria rechazado. Se enciende cuando ese mapeo existe. El manejo de errores es el del contenedor de
 * Spring Kafka (reintentos y descarte tras agotarlos): una cola de mensajes muertos con alerta es la
 * decision pendiente de operaciones; mientras tanto el rechazo deja traza y NO marca el evento como
 * consumido, asi que reprocesar el tema repite solo lo que falto.
 */
@Component
@ConditionalOnProperty(name = "aportaya.contabilidad.consumo-de-asientos.habilitado", havingValue = "true")
public class ConsumidorDeAsientosKafka {

    private static final Logger BITACORA = LoggerFactory.getLogger(ConsumidorDeAsientosKafka.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ConsumoDeAsientos consumo;

    public ConsumidorDeAsientosKafka(ConsumoDeAsientos consumo) {
        this.consumo = consumo;
    }

    @KafkaListener(
            topics = {
                "aportaya.garantia.respaldo_reservado",
                "aportaya.garantia.respaldo_ampliado",
                "aportaya.garantia.respaldo_liberado",
                "aportaya.garantia.respaldo_recuperado",
                "aportaya.garantia.cobertura_empresarial_aplicada",
                "aportaya.garantia.cobertura_empresarial_reversada"
            },
            groupId = "${spring.application.name}-asientos")
    public void alRecibir(ConsumerRecord<String, String> registro) {
        var evento = leer(registro);
        var salida = consumo.procesar(evento);
        // Identificadores, nunca importes ni datos de personas (regla 90.2).
        BITACORA.info("evento {} ({}) -> {}", evento.eventoId(), evento.tipo(), salida.resultado());
    }

    static EventoRecibido leer(ConsumerRecord<String, String> registro) {
        try {
            return new EventoRecibido(
                    UUID.fromString(cabecera(registro, "event_id")),
                    cabecera(registro, "type"),
                    UUID.fromString(cabecera(registro, "correlation_id")),
                    JSON.readTree(registro.value()));
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
