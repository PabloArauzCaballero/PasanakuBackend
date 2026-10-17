package bo.aportaya.auditoria.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsumidorDeIndicadoresKafkaTest {

    private static ConsumerRecord<String, String> registro(String cuerpo) {
        var r = new ConsumerRecord<>("aportaya.nucleo_financiero.indicador_calculado", 0, 0L, "clave", cuerpo);
        r.headers().add("event_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        return r;
    }

    private static final String BUENO =
            """
            {"codigo":"SALDO_CARGADO","nombre":"Saldo cargado","valor":"1500.00","unidad":"BOB",\
            "periodo":"2026-09","casos":6,"provisorio":false,"dimension":"GLOBAL"}""";

    @Test
    @DisplayName("correcto: lee código, valor exacto, unidad, período y casos")
    void correcto() {
        var e = ConsumidorDeIndicadoresKafka.leer(registro(BUENO));

        assertThat(e.codigo()).isEqualTo("SALDO_CARGADO");
        assertThat(e.valor()).isEqualByComparingTo("1500.00");
        assertThat(e.periodo()).isEqualTo("2026-09");
        assertThat(e.casos()).isEqualTo(6);
        assertThat(e.provisorio()).isFalse();
    }

    @Test
    @DisplayName("límite: sin provisorio se lee como definitivo")
    void sinProvisorio() {
        assertThat(ConsumidorDeIndicadoresKafka.leer(registro(BUENO.replace(",\"provisorio\":false", "")))
                        .provisorio())
                .isFalse();
    }

    @Test
    @DisplayName("inválido: período mal formado, sin casos o valor no numérico se rechazan")
    void rechazos() {
        assertThatThrownBy(() -> ConsumidorDeIndicadoresKafka.leer(registro(BUENO.replace("2026-09", "09/2026"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeIndicadoresKafka.leer(registro(BUENO.replace("\"casos\":6,", ""))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeIndicadoresKafka.leer(registro(BUENO.replace("\"1500.00\"", "\"mucho\""))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
