package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F3.L2.M01 · La carga de {@code grupos.turnos_asignados} contra lo que exige {@code entregas} en su pacto
 * ({@code contratos/pactos/grupos-entregas-turnos-asignados.json}, propuesto en el PR #490). Los campos exigidos
 * se copian acá del pacto; cuando #490 entre a {@code dev}, esta prueba pasa a leerlo del archivo.
 */
class TurnoAsignadoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> EXIGIDOS_POR_TURNO =
            List.of("turnoId", "numero", "periodoId", "cupoId", "titularParticipanteId", "version");

    private static TurnoAsignado turno(int numero) {
        return new TurnoAsignado(UUID.randomUUID(), numero, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    @Test
    @DisplayName("Correcto · la carga trae grupoId y, por turno, todo lo que exige el consumidor")
    void camposDelPacto() {
        UUID grupo = UUID.randomUUID();
        JsonNode carga = JSON.valueToTree(TurnoAsignado.carga(grupo, List.of(turno(1), turno(2))));

        assertThat(carga.get("grupoId").asText()).isEqualTo(grupo.toString());
        assertThat(carga.get("turnos")).hasSize(2);
        carga.get("turnos").forEach(t -> assertThat(EXIGIDOS_POR_TURNO)
                .allSatisfy(c -> assertThat(t.has(c)).as(c).isTrue()));
        assertThat(carga.get("turnos").get(0).get("version").asLong()).isEqualTo(1);
        assertThat(carga.get("turnos").get(1).get("numero").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("Límite · un grupo sin turnos publica la lista vacía, no la omite")
    void sinTurnos() {
        Map<String, Object> carga = TurnoAsignado.carga(UUID.randomUUID(), List.of());

        assertThat(carga).containsKey("turnos");
        assertThat((List<?>) carga.get("turnos")).isEmpty();
    }

    @Test
    @DisplayName("Inválido · la carga no lleva nada más que identificadores, orden y versión (regla 90.2)")
    void soloIdentificadores() {
        JsonNode t = JSON.valueToTree(TurnoAsignado.carga(UUID.randomUUID(), List.of(turno(1))))
                .get("turnos")
                .get(0);

        assertThat(t.fieldNames()).toIterable().containsExactlyInAnyOrderElementsOf(EXIGIDOS_POR_TURNO);
    }
}
