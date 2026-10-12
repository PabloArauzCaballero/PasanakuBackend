package bo.aportaya.aportes.consumo;

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
 * Carril Q · el borde con Kafka de la compensacion, sin broker, contra el ejemplo del pacto
 * {@code contratos/pactos/aportes-nucleo-financiero.json} (el MISMO archivo que verifica el productor, en
 * {@code CUAporteNoAplicadoTest} de nucleo-financiero). No cubre el broker real ni el contenedor de listeners.
 */
class ConsumidorDeAportesNoAplicadosKafkaTest {

    private static JsonNode pacto() throws IOException {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve("contratos/pactos/aportes-nucleo-financiero.json"))) {
            p = p.getParent();
        }
        assertThat(p).as("el pacto existe").isNotNull();
        return new ObjectMapper()
                .readTree(Files.readString(
                        p.resolve("contratos/pactos/aportes-nucleo-financiero.json"), StandardCharsets.UTF_8));
    }

    private static ConsumerRecord<String, String> registro(String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.nucleo_financiero.aporte_no_aplicado", 0, 0L, "clave", cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    @Test
    @DisplayName("pacto · correcto: el consumidor entiende el ejemplo que publica nucleo (pago, obligacion, motivo, cabeceras)")
    void correcto() throws IOException {
        JsonNode pacto = pacto();
        UUID evento = UUID.randomUUID();
        UUID correlacion = UUID.randomUUID();

        var aviso = ConsumidorDeAportesNoAplicadosKafka.leer(registro(
                pacto.path("ejemplo").toString(),
                "event_id",
                evento.toString(),
                "correlation_id",
                correlacion.toString()));

        assertThat(aviso.eventoId()).isEqualTo(evento);
        assertThat(aviso.correlationId()).isEqualTo(correlacion);
        assertThat(aviso.pagoId().toString())
                .isEqualTo(pacto.path("ejemplo").path("pagoId").asText());
        assertThat(aviso.obligacionId().toString())
                .isEqualTo(pacto.path("ejemplo").path("obligacionId").asText());
        assertThat(aviso.motivo()).isEqualTo("SALDO_INSUFICIENTE");
        // Lo que el consumidor exige esta declarado en el pacto, y el ejemplo lo trae todo.
        for (JsonNode campo : pacto.path("consumidorExige")) {
            assertThat(pacto.path("ejemplo").has(campo.asText())).as(campo.asText()).isTrue();
        }
        assertThat(pacto.path("topico").asText()).isEqualTo("aportaya.nucleo_financiero.aporte_no_aplicado");
    }

    @Test
    @DisplayName("pacto · limite: los campos de mas (grupo, importe, codigo) no rompen la lectura")
    void camposDeMas() throws IOException {
        var aviso = ConsumidorDeAportesNoAplicadosKafka.leer(registro(
                pacto().path("ejemplo").toString(),
                "event_id",
                UUID.randomUUID().toString(),
                "correlation_id",
                UUID.randomUUID().toString()));
        assertThat(aviso.pagoId()).isNotNull();
    }

    @Test
    @DisplayName("pacto · invalido: falta una cabecera o un campo, un id que no es UUID o un cuerpo que no es JSON se"
            + " RECHAZAN (excepcion): nunca se ignoran")
    void invalido() throws IOException {
        String ejemplo = pacto().path("ejemplo").toString();
        String id = UUID.randomUUID().toString();
        assertThatThrownBy(() -> ConsumidorDeAportesNoAplicadosKafka.leer(registro(ejemplo, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        ConsumidorDeAportesNoAplicadosKafka.leer(registro(ejemplo, "event_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeAportesNoAplicadosKafka.leer(
                        registro(ejemplo, "event_id", "no-es-uuid", "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeAportesNoAplicadosKafka.leer(
                        registro("esto no es json", "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                        ConsumidorDeAportesNoAplicadosKafka.leer(registro(null, "event_id", id, "correlation_id", id)))
                .isInstanceOf(IllegalArgumentException.class);
        for (String sinCampo : new String[] {"pagoId", "obligacionId", "motivo"}) {
            var recortado = ((com.fasterxml.jackson.databind.node.ObjectNode)
                            pacto().path("ejemplo").deepCopy())
                    .without(sinCampo);
            assertThatThrownBy(() -> ConsumidorDeAportesNoAplicadosKafka.leer(
                            registro(recortado.toString(), "event_id", id, "correlation_id", id)))
                    .as("sin " + sinCampo)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
