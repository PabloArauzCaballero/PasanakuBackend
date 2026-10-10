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
 * {@code correlation_id}, la cesion como clave del registro, payload JSON de {@code entregas.derecho_cedido} en el
 * cuerpo). No cubre el broker real ni el contenedor de listeners: eso lo ejercita el recorrido de integracion real.
 */
class ConsumidorDeCesionesKafkaTest {

    private static final UUID CESION = UUID.randomUUID();
    private static final UUID EVENTO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private static String cuerpo(UUID turno, UUID grupo, UUID cupo, UUID origen, UUID destino) {
        return "{\"turnoId\":\"%s\",\"grupoId\":\"%s\",\"cupoId\":\"%s\",\"participanteOrigenId\":\"%s\",\"participanteDestinoId\":\"%s\",\"precio\":\"5500.00\",\"moneda\":\"BOB\",\"liquidacionRef\":\"%s\"}"
                .formatted(turno, grupo, cupo, origen, destino, UUID.randomUUID());
    }

    private static ConsumerRecord<String, String> registro(String clave, String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.entregas.derecho_cedido", 0, 0L, clave, cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    @Test
    @DisplayName("correcto: lee el evento, la correlacion, la cesion (clave del registro) y los cinco identificadores del payload")
    void correcto() {
        UUID turno = UUID.randomUUID();
        UUID grupo = UUID.randomUUID();
        UUID cupo = UUID.randomUUID();
        UUID origen = UUID.randomUUID();
        UUID destino = UUID.randomUUID();

        var c = ConsumidorDeCesionesKafka.leer(registro(
                CESION.toString(),
                cuerpo(turno, grupo, cupo, origen, destino),
                "event_id",
                EVENTO.toString(),
                "correlation_id",
                CORRELACION.toString()));

        assertThat(c.eventoId()).isEqualTo(EVENTO);
        assertThat(c.correlationId()).isEqualTo(CORRELACION);
        assertThat(c.cesionId()).isEqualTo(CESION);
        assertThat(c.turnoId()).isEqualTo(turno);
        assertThat(c.grupoId()).isEqualTo(grupo);
        assertThat(c.cupoId()).isEqualTo(cupo);
        assertThat(c.participanteOrigenId()).isEqualTo(origen);
        assertThat(c.participanteDestinoId()).isEqualTo(destino);
    }

    @Test
    @DisplayName("invalido: sin cabecera, sin clave, con un campo que falta o que no es UUID, o con un cuerpo que no es JSON se RECHAZA, nunca se ignora")
    void invalido() {
        UUID u = UUID.randomUUID();
        String bueno = cuerpo(u, u, u, u, u);
        String id = UUID.randomUUID().toString();

        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(registro(CESION.toString(), bueno, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(registro(null, bueno, "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(
                        registro(CESION.toString(), bueno.replace("cupoId", "otro"), "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(registro(
                        CESION.toString(), bueno.replace(u.toString(), "no-es-uuid"), "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(
                        registro(CESION.toString(), "esto no es json", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeCesionesKafka.leer(
                        registro(CESION.toString(), null, "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
