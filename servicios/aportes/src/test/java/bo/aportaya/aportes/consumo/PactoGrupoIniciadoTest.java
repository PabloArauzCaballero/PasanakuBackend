package bo.aportaya.aportes.consumo;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * R-GRP-20 · la mitad consumidora del pacto {@code contratos/pactos/grupos-aportes-grupo-iniciado.json}: el MISMO archivo
 * que verifica el productor ({@code CU69EventoDeInicioTest} de grupos). Si grupos cambia la forma del evento, cambia el
 * pacto, y esta prueba deja de leerlo. No cubre el broker real ni el contenedor de listeners.
 */
class PactoGrupoIniciadoTest {

    private static final String PACTO = "contratos/pactos/grupos-aportes-grupo-iniciado.json";

    private static JsonNode pacto() throws IOException {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve(PACTO))) {
            p = p.getParent();
        }
        assertThat(p).as("el pacto existe").isNotNull();
        return new ObjectMapper().readTree(Files.readString(p.resolve(PACTO), StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("el consumidor lee el ejemplo del pacto entero: periodo 1, dos cupos con Bs 500,00 y su titular")
    void leeElEjemploDelPacto() throws IOException {
        JsonNode pacto = pacto();
        assertThat(pacto.get("topico").asText()).isEqualTo("aportaya.grupos.grupo_iniciado");
        assertThat(pacto.get("consumidor").asText()).isEqualTo("aportes");
        JsonNode ejemplo = pacto.get("ejemplo");

        var r = new ConsumerRecord<>(
                pacto.get("topico").asText(), 0, 0L, ejemplo.get("grupoId").asText(), ejemplo.toString());
        UUID evento = UUID.randomUUID();
        UUID correlacion = UUID.randomUUID();
        r.headers().add("event_id", evento.toString().getBytes(StandardCharsets.UTF_8));
        r.headers().add("correlation_id", correlacion.toString().getBytes(StandardCharsets.UTF_8));

        var inicio = ConsumidorDeGrupoIniciadoKafka.leer(r);

        assertThat(inicio.eventoId()).isEqualTo(evento);
        assertThat(inicio.correlationId()).isEqualTo(correlacion);
        assertThat(inicio.grupoId())
                .isEqualTo(UUID.fromString(ejemplo.get("grupoId").asText()));
        assertThat(inicio.periodoId())
                .isEqualTo(
                        UUID.fromString(ejemplo.get("periodo").get("periodoId").asText()));
        assertThat(inicio.numeroDePeriodo()).isEqualTo(1);
        assertThat(inicio.fechaLimitePago()).isEqualTo(LocalDate.parse("2026-11-30"));
        assertThat(inicio.fechaFinGracia()).isEqualTo(LocalDate.parse("2026-12-03"));
        assertThat(inicio.cupos()).hasSize(ejemplo.get("cupos").size());
        assertThat(inicio.cupos()).allSatisfy(c -> {
            assertThat(c.montoAporte().monto()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(c.montoAporte().moneda()).isEqualTo(Moneda.BOB);
        });
        // Lo que el pacto dice que el consumidor exige, el ejemplo lo trae.
        for (JsonNode exigido : pacto.get("consumidorExige")) {
            String ruta = exigido.asText().replace(" (no vacio)", "").replace("[]", "");
            JsonNode nodo = ejemplo;
            for (String paso : ruta.split("\\.")) {
                nodo = nodo.isArray() ? nodo.get(0).get(paso) : nodo.get(paso);
            }
            assertThat(nodo).as(ruta).isNotNull();
        }
    }
}
