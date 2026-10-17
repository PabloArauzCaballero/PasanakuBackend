package bo.aportaya.garantia.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El borde con Kafka de {@code nucleo_financiero.transaccion_reversada} en garantia, sin broker. */
class ConsumidorDeReversosKafkaTest {

    private static ConsumerRecord<String, String> registro(String cuerpo) {
        var r = new ConsumerRecord<>("aportaya.nucleo_financiero.transaccion_reversada", 0, 0L, "clave", cuerpo);
        r.headers().add("event_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        return r;
    }

    @Test
    @DisplayName("correcto: lee la transaccion original y la de reverso")
    void correcto() {
        UUID original = UUID.randomUUID();
        UUID reverso = UUID.randomUUID();

        var r = ConsumidorDeReversosKafka.leer(registro(
                "{\"transaccionOriginalId\":\"%s\",\"transaccionReversoId\":\"%s\",\"tipo\":\"ERROR_OPERATIVO\"}"
                        .formatted(original, reverso)));

        assertThat(r.transaccionOriginalId()).isEqualTo(original);
        assertThat(r.transaccionReversoId()).isEqualTo(reverso);
    }

    @Test
    @DisplayName("límite: campos de más (tipo, motivo) no molestan")
    void camposDeMas() {
        UUID original = UUID.randomUUID();
        var r = ConsumidorDeReversosKafka.leer(
                registro("{\"transaccionOriginalId\":\"%s\",\"transaccionReversoId\":\"%s\",\"otro\":1}"
                        .formatted(original, UUID.randomUUID())));

        assertThat(r.transaccionOriginalId()).isEqualTo(original);
    }

    @Test
    @DisplayName("inválido: sin la transaccion de reverso no se lee")
    void sinReverso() {
        assertThatThrownBy(() -> ConsumidorDeReversosKafka.leer(
                        registro("{\"transaccionOriginalId\":\"%s\"}".formatted(UUID.randomUUID()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("transaccionReversoId");
    }
}
