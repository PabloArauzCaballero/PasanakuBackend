package bo.aportaya.garantia.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El borde con Kafka de {@code nucleo_financiero.aporte_aplicado} en garantia, sin broker. El campo {@code destino} se
 * sumo el 2026-10-16: sin el, el evento es anterior y era siempre al pozo del grupo.
 */
class ConsumidorDeAportesAplicadosKafkaTest {

    private static final UUID TRANSACCION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private static ConsumerRecord<String, String> registro(String cuerpo) {
        var r = new ConsumerRecord<>("aportaya.nucleo_financiero.aporte_aplicado", 0, 0L, "clave", cuerpo);
        r.headers().add("event_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        return r;
    }

    private static String cuerpo(String extra) {
        return """
                {"pagoId":"%s","obligacionId":"%s","grupoId":"%s","transaccionId":"%s","asientoId":"%s",\
                "monto":"25.00","moneda":"BOB"%s}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), GRUPO, TRANSACCION, UUID.randomUUID(), extra);
    }

    @Test
    @DisplayName("correcto: un recargo llega con destino FONDO_GARANTIA, su transaccion, su grupo y su importe")
    void correcto() {
        var e = ConsumidorDeAportesAplicadosKafka.leer(registro(cuerpo(",\"destino\":\"FONDO_GARANTIA\"")));

        assertThat(e.destino()).isEqualTo("FONDO_GARANTIA");
        assertThat(e.transaccionId()).isEqualTo(TRANSACCION);
        assertThat(e.grupoId()).isEqualTo(GRUPO);
        assertThat(e.monto().monto()).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("límite: un evento anterior al campo (sin destino) se lee como GRUPO, que es lo que era")
    void sinDestino() {
        assertThat(ConsumidorDeAportesAplicadosKafka.leer(registro(cuerpo(""))).destino())
                .isEqualTo("GRUPO");
    }

    @Test
    @DisplayName("inválido: sin moneda no se lee — un importe sin moneda no existe (regla 91.1.2)")
    void sinMoneda() {
        String sinMoneda = cuerpo(",\"destino\":\"FONDO_GARANTIA\"").replace(",\"moneda\":\"BOB\"", "");

        assertThatThrownBy(() -> ConsumidorDeAportesAplicadosKafka.leer(registro(sinMoneda)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moneda");
    }

    @Test
    @DisplayName(
            "pacto con nucleo-financiero · consumidor: lee el ejemplo de aporte_aplicado con destino FONDO_GARANTIA")
    void pactoConNucleo() throws IOException {
        JsonNode pacto = pacto("contratos/pactos/nucleo-financiero-garantia-aporte-aplicado.json");
        JsonNode ejemplo = pacto.path("ejemplo");

        var e = ConsumidorDeAportesAplicadosKafka.leer(registro(ejemplo.toString()));

        assertThat(e.destino()).isEqualTo("FONDO_GARANTIA");
        assertThat(e.transaccionId().toString())
                .isEqualTo(ejemplo.path("transaccionId").asText());
        assertThat(e.grupoId().toString()).isEqualTo(ejemplo.path("grupoId").asText());
        assertThat(e.monto().monto()).isEqualByComparingTo(ejemplo.path("monto").asText());
        for (JsonNode campo : pacto.path("consumidorExige")) {
            assertThat(ejemplo.has(campo.asText())).as(campo.asText()).isTrue();
        }
        assertThat(pacto.path("topico").asText()).isEqualTo("aportaya.nucleo_financiero.aporte_aplicado");
    }

    private static JsonNode pacto(String archivo) throws IOException {
        Path raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isRegularFile(raiz.resolve(archivo))) {
            raiz = raiz.getParent();
        }
        return new ObjectMapper().readTree(Files.readString(raiz.resolve(archivo), StandardCharsets.UTF_8));
    }
}
