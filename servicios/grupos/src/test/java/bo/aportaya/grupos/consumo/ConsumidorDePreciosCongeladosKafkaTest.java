package bo.aportaya.grupos.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El borde con Kafka, sin broker: lo que se lee del sobre que arma {@code Relevo} (cabeceras {@code event_id} y
 * {@code correlation_id}, payload JSON de {@code tarifas.tarifa_congelada}). No cubre el broker real ni el contenedor de
 * listeners: eso lo ejercita el recorrido de integracion real.
 */
class ConsumidorDePreciosCongeladosKafkaTest {

    private static final UUID EVENTO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private static ConsumerRecord<String, String> registro(String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.tarifas.tarifa_congelada", 0, 0L, "clave", cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    private static String cuerpo(UUID grupo, UUID tarifario) {
        return "{\"grupoId\":\"%s\",\"tarifarioId\":\"%s\",\"hashSnapshot\":\"%s\"}"
                .formatted(grupo, tarifario, "a".repeat(64));
    }

    @Test
    @DisplayName("correcto: lee el evento, la correlacion, el grupo y el tarifario; ignora lo que no usa")
    void correcto() {
        UUID grupo = UUID.randomUUID();
        UUID tarifario = UUID.randomUUID();

        var precio = ConsumidorDePreciosCongeladosKafka.leer(registro(
                cuerpo(grupo, tarifario), "event_id", EVENTO.toString(), "correlation_id", CORRELACION.toString()));

        assertThat(precio.eventoId()).isEqualTo(EVENTO);
        assertThat(precio.correlationId()).isEqualTo(CORRELACION);
        assertThat(precio.grupoId()).isEqualTo(grupo);
        assertThat(precio.tarifarioId()).isEqualTo(tarifario);
    }

    @Test
    @DisplayName(
            "invalido: sin cabecera, con un campo que falta o que no es UUID, o con un cuerpo que no es JSON se RECHAZA")
    void invalido() {
        UUID u = UUID.randomUUID();
        String bueno = cuerpo(u, u);
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(registro(bueno, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(registro(bueno, "event_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(
                        registro(bueno.replace("grupoId", "otro"), "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("grupoId");
        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(
                        registro(bueno.replace(u.toString(), "no-es-uuid"), "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(
                        registro("esto no es json", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDePreciosCongeladosKafka.leer(
                        registro("[1,2]", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
