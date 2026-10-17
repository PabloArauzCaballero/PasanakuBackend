package bo.aportaya.nucleofinanciero.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El borde con Kafka de {@code aportes.aporte_cobrado}, sin broker: el campo {@code tipo} que se sumo el 2026-10-16 se
 * lee cuando viene y su ausencia no rompe la lectura (productor viejo y nuevo conviven durante el despliegue).
 */
class ConsumidorDeAportesKafkaTest {

    private static final UUID PAGO = UUID.randomUUID();
    private static final UUID OBLIGACION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private static ConsumerRecord<String, String> registro(String cuerpo) {
        var r = new ConsumerRecord<>("aportaya.aportes.aporte_cobrado", 0, 0L, "clave", cuerpo);
        r.headers().add("event_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        return r;
    }

    private static String cuerpo(String extra) {
        return """
                {"pagoId":"%s","obligacionId":"%s","grupoId":"%s","usuarioId":"%s",\
                "monto":"25.00","moneda":"BOB","canal":"BILLETERA_MOVIL"%s}"""
                .formatted(PAGO, OBLIGACION, GRUPO, USUARIO, extra);
    }

    @Test
    @DisplayName("correcto: un recargo por mora llega con tipo RECARGO_MORA y se lee")
    void correcto() {
        var aporte = ConsumidorDeAportesKafka.leer(registro(cuerpo(",\"tipo\":\"RECARGO_MORA\"")));

        assertThat(aporte.tipo()).contains("RECARGO_MORA");
        assertThat(aporte.obligacionId()).isEqualTo(OBLIGACION);
    }

    @Test
    @DisplayName("límite: un evento anterior al campo (sin tipo, o tipo vacío) se lee igual, con tipo ausente")
    void sinTipo() {
        assertThat(ConsumidorDeAportesKafka.leer(registro(cuerpo(""))).tipo()).isEmpty();
        assertThat(ConsumidorDeAportesKafka.leer(registro(cuerpo(",\"tipo\":\"\"")))
                        .tipo())
                .isEmpty();
    }

    @Test
    @DisplayName("inválido: el tipo no reemplaza a los campos obligatorios — sin monto, se rechaza")
    void sinMonto() {
        String sinMonto = cuerpo(",\"tipo\":\"RECARGO_MORA\"").replace("\"monto\":\"25.00\",", "");

        assertThatThrownBy(() -> ConsumidorDeAportesKafka.leer(registro(sinMonto)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monto");
    }
}
