package bo.aportaya.entregas.consumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.L2.M01 · la mitad CONSUMIDORA de los pactos {@code aportes-entregas-periodo-recaudado.json} y
 * {@code grupos-entregas-turnos-asignados.json}: se leen sus ejemplos, sin broker. Los productores todavia no existen
 * (regla 65): cuando los escriban, su prueba de productor lee el MISMO archivo. No cubre el broker real ni el
 * contenedor de listeners.
 */
class PactosDeLaEntregaAutomaticaTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UUID EVENTO = UUID.randomUUID();
    private static final UUID CORRELACION = UUID.randomUUID();

    private static JsonNode ejemplo(String pacto) throws IOException {
        String archivo = "contratos/pactos/" + pacto;
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve(archivo))) {
            p = p.getParent();
        }
        assertThat(p).as("el pacto existe").isNotNull();
        return JSON.readTree(Files.readString(p.resolve(archivo), StandardCharsets.UTF_8))
                .get("ejemplo");
    }

    private static ConsumerRecord<String, String> registro(Object cuerpo, String... cabeceras) {
        var r = new ConsumerRecord<>("topico", 0, 0L, "clave", String.valueOf(cuerpo));
        for (int i = 0; i < cabeceras.length; i += 2) {
            r.headers().add(cabeceras[i], cabeceras[i + 1].getBytes(StandardCharsets.UTF_8));
        }
        return r;
    }

    private static ConsumerRecord<String, String> conSobre(Object cuerpo) {
        return registro(cuerpo, "event_id", EVENTO.toString(), "correlation_id", CORRELACION.toString());
    }

    @Test
    @DisplayName("correcto: el ejemplo de periodo_recaudado se lee entero, con el sobre")
    void periodoRecaudadoCorrecto() throws IOException {
        JsonNode e = ejemplo("aportes-entregas-periodo-recaudado.json");

        var aviso = ConsumidorDePeriodoRecaudadoKafka.leer(conSobre(e));

        assertThat(aviso.eventoId()).isEqualTo(EVENTO);
        assertThat(aviso.correlationId()).isEqualTo(CORRELACION);
        assertThat(aviso.periodoId()).hasToString(e.get("periodoId").asText());
        assertThat(aviso.grupoId()).hasToString(e.get("grupoId").asText());
        assertThat(aviso.motivo()).isEqualTo("COMPLETO");
    }

    @Test
    @DisplayName("limite: GRACIA_VENCIDA es el otro motivo del pacto y se acepta")
    void periodoRecaudadoGraciaVencida() throws IOException {
        ObjectNode e = (ObjectNode) ejemplo("aportes-entregas-periodo-recaudado.json");
        e.put("motivo", "GRACIA_VENCIDA");

        assertThat(ConsumidorDePeriodoRecaudadoKafka.leer(conSobre(e)).motivo()).isEqualTo("GRACIA_VENCIDA");
    }

    @Test
    @DisplayName("invalido: sin cabecera, sin periodo, con motivo fuera del pacto o con cuerpo ilegible se rechaza")
    void periodoRecaudadoInvalido() throws IOException {
        JsonNode e = ejemplo("aportes-entregas-periodo-recaudado.json");
        ObjectNode sinPeriodo = ((ObjectNode) e.deepCopy()).without("periodoId");
        ObjectNode otroMotivo = ((ObjectNode) e.deepCopy()).put("motivo", "PARCIAL");

        assertThatThrownBy(() ->
                        ConsumidorDePeriodoRecaudadoKafka.leer(registro(e, "correlation_id", CORRELACION.toString())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("event_id");
        assertThatThrownBy(() -> ConsumidorDePeriodoRecaudadoKafka.leer(conSobre(sinPeriodo)))
                .hasMessageContaining("periodoId");
        assertThatThrownBy(() -> ConsumidorDePeriodoRecaudadoKafka.leer(conSobre(otroMotivo)))
                .hasMessageContaining("PARCIAL");
        assertThatThrownBy(() -> ConsumidorDePeriodoRecaudadoKafka.leer(conSobre("no es json")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("correcto: el ejemplo de turnos_asignados se lee con su turno, su periodo, su titular y su version")
    void turnosAsignadosCorrecto() throws IOException {
        JsonNode e = ejemplo("grupos-entregas-turnos-asignados.json");
        JsonNode t = e.get("turnos").get(0);

        var evento = ConsumidorDeTurnosAsignadosKafka.leer(conSobre(e));

        assertThat(evento.eventoId()).isEqualTo(EVENTO);
        assertThat(evento.grupoId()).hasToString(e.get("grupoId").asText());
        assertThat(evento.turnos()).singleElement().satisfies(turno -> {
            assertThat(turno.turnoId()).hasToString(t.get("turnoId").asText());
            assertThat(turno.grupoId()).isEqualTo(evento.grupoId());
            assertThat(turno.periodoId()).hasToString(t.get("periodoId").asText());
            assertThat(turno.cupoId()).hasToString(t.get("cupoId").asText());
            assertThat(turno.titularParticipanteId())
                    .hasToString(t.get("titularParticipanteId").asText());
            assertThat(turno.version()).isEqualTo(1);
        });
    }

    @Test
    @DisplayName("invalido: sin turnos, con un turno sin titular o con version no entera se rechaza el evento entero")
    void turnosAsignadosInvalido() throws IOException {
        JsonNode e = ejemplo("grupos-entregas-turnos-asignados.json");
        ObjectNode sinTurnos = ((ObjectNode) e.deepCopy());
        sinTurnos.putArray("turnos");
        ObjectNode sinTitular = (ObjectNode) e.deepCopy();
        ((ObjectNode) sinTitular.get("turnos").get(0)).remove("titularParticipanteId");
        ObjectNode versionTexto = (ObjectNode) e.deepCopy();
        ((ObjectNode) versionTexto.get("turnos").get(0)).put("version", "uno");

        assertThatThrownBy(() -> ConsumidorDeTurnosAsignadosKafka.leer(conSobre(sinTurnos)))
                .hasMessageContaining("turnos");
        assertThatThrownBy(() -> ConsumidorDeTurnosAsignadosKafka.leer(conSobre(sinTitular)))
                .hasMessageContaining("titularParticipanteId");
        assertThatThrownBy(() -> ConsumidorDeTurnosAsignadosKafka.leer(conSobre(versionTexto)))
                .hasMessageContaining("version");
        assertThatThrownBy(() -> ConsumidorDeTurnosAsignadosKafka.leer(registro(e, "event_id", EVENTO.toString())))
                .hasMessageContaining("correlation_id");
    }
}
