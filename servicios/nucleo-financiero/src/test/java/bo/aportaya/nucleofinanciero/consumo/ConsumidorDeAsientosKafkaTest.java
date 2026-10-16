package bo.aportaya.nucleofinanciero.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El borde con Kafka, sin broker: lo que se lee del sobre que arma {@code Relevo} ({@code event_id}, {@code type},
 * {@code correlation_id} en cabeceras, payload JSON en el cuerpo). No cubre el broker real ni el contenedor de listeners.
 */
class ConsumidorDeAsientosKafkaTest {

    private static ConsumerRecord<String, String> registro(String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.garantia.respaldo_reservado", 0, 0L, "clave", cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    @Test
    @DisplayName("correcto: lee el id del evento, su tipo, la correlacion y el payload")
    void correcto() {
        UUID evento = UUID.randomUUID();
        UUID correlacion = UUID.randomUUID();
        var e = ConsumidorDeAsientosKafka.leer(registro(
                "{\"asiento\":{\"origenTipo\":\"AJUSTE\"}}",
                "event_id",
                evento.toString(),
                "type",
                "garantia.respaldo_reservado",
                "correlation_id",
                correlacion.toString()));
        assertThat(e.eventoId()).isEqualTo(evento);
        assertThat(e.tipo()).isEqualTo("garantia.respaldo_reservado");
        assertThat(e.correlationId()).isEqualTo(correlacion);
        assertThat(e.carga().path("asiento").path("origenTipo").asText()).isEqualTo("AJUSTE");
    }

    @Test
    @DisplayName("invalido: falta una cabecera, el id no es UUID o el cuerpo no es JSON se RECHAZA, nunca se ignora")
    void invalido() {
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> ConsumidorDeAsientosKafka.leer(registro("{}", "type", "t", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeAsientosKafka.leer(
                        registro("{}", "event_id", "no-es-uuid", "type", "t", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeAsientosKafka.leer(
                        registro("esto no es json", "event_id", id, "type", "t", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeAsientosKafka.leer(
                        registro(null, "event_id", id, "type", "t", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
