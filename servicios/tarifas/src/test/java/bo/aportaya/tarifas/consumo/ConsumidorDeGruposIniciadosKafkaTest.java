package bo.aportaya.tarifas.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El borde con Kafka de F3.S8.M6, sin broker, contra el ejemplo del pacto
 * {@code contratos/pactos/grupos-tarifas-grupo-iniciado.json}. No cubre el broker real.
 */
class ConsumidorDeGruposIniciadosKafkaTest {

    private static final String EVENTO = UUID.randomUUID().toString();
    private static final String CORRELACION = UUID.randomUUID().toString();

    private static ConsumerRecord<String, String> registro(String cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("aportaya.grupos.grupo_iniciado", 0, 0L, "clave", cuerpo);
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    private static String ejemploDelPacto() throws Exception {
        Path actual = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(actual.resolve("contratos/pactos"))) {
            actual = actual.getParent();
        }
        var pacto = new ObjectMapper()
                .readTree(actual.resolve("contratos/pactos/grupos-tarifas-grupo-iniciado.json")
                        .toFile());
        return pacto.get("ejemplo").toString();
    }

    @Test
    @DisplayName("Correcto · lee del ejemplo del pacto el grupo y el momento del inicio, con su correlación")
    void correcto() throws Exception {
        var iniciado = ConsumidorDeGruposIniciadosKafka.leer(
                registro(ejemploDelPacto(), "event_id", EVENTO, "correlation_id", CORRELACION));

        assertThat(iniciado.eventoId()).hasToString(EVENTO);
        assertThat(iniciado.correlationId()).hasToString(CORRELACION);
        assertThat(iniciado.grupoId()).hasToString("d0000000-0000-4000-8000-000000000001");
        assertThat(iniciado.iniciadoEn()).isEqualTo(OffsetDateTime.parse("2026-10-30T15:00:00Z"));
    }

    @Test
    @DisplayName("Límite · campos que tarifas no usa (cupos, montos) no hacen falta")
    void soloLoQueUsa() {
        var iniciado = ConsumidorDeGruposIniciadosKafka.leer(registro(
                "{\"grupoId\":\"" + UUID.randomUUID() + "\",\"iniciadoEn\":\"2026-10-30T15:00:00-04:00\"}",
                "event_id",
                EVENTO,
                "correlation_id",
                CORRELACION));

        assertThat(iniciado.iniciadoEn()).isEqualTo(OffsetDateTime.parse("2026-10-30T19:00:00Z"));
    }

    @Test
    @DisplayName("Inválido · sin cabecera, sin grupoId, sin iniciadoEn, fecha sin zona o cuerpo que no es objeto")
    void invalido() {
        String grupo = UUID.randomUUID().toString();
        String bueno = "{\"grupoId\":\"" + grupo + "\",\"iniciadoEn\":\"2026-10-30T15:00:00Z\"}";

        assertThatThrownBy(() -> ConsumidorDeGruposIniciadosKafka.leer(registro(bueno, "event_id", EVENTO)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGruposIniciadosKafka.leer(
                        registro(bueno.replace("grupoId", "otro"), "event_id", EVENTO, "correlation_id", CORRELACION)))
                .hasMessageContaining("grupoId");
        assertThatThrownBy(() -> ConsumidorDeGruposIniciadosKafka.leer(registro(
                        bueno.replace("iniciadoEn", "otro"), "event_id", EVENTO, "correlation_id", CORRELACION)))
                .hasMessageContaining("iniciadoEn");
        assertThatThrownBy(() -> ConsumidorDeGruposIniciadosKafka.leer(registro(
                        bueno.replace("15:00:00Z", "15:00:00"), "event_id", EVENTO, "correlation_id", CORRELACION)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ConsumidorDeGruposIniciadosKafka.leer(
                        registro("[1]", "event_id", EVENTO, "correlation_id", CORRELACION)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
