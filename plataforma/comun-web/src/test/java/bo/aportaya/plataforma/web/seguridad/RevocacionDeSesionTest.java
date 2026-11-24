package bo.aportaya.plataforma.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RevocacionDeSesionTest {

    private static final Instant REVOCADA = Instant.parse("2026-11-24T15:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-24T15:15:00Z");

    @Test
    @DisplayName("correcto: ida y vuelta por JSON conserva los cuatro campos y la clave es la sesion")
    void idaYVuelta() {
        var original = new RevocacionDeSesion(UUID.randomUUID(), UUID.randomUUID(), REVOCADA, VENCE);

        var leida = RevocacionDeSesion.desdeJson(original.aJson());

        assertThat(leida).isEqualTo(original);
        assertThat(original.clave()).isEqualTo(original.sesionId().toString());
    }

    @Test
    @DisplayName("limite: un campo que el consumidor no conoce se ignora (compatibilidad hacia adelante)")
    void campoNuevoSeIgnora() {
        UUID sesion = UUID.randomUUID();
        String json =
                """
                {"sesionId":"%s","usuarioId":"%s","revocadaEn":"%s","venceEn":"%s","motivoNuevo":"x"}
                """
                        .formatted(sesion, UUID.randomUUID(), REVOCADA, VENCE);

        assertThat(RevocacionDeSesion.desdeJson(json).sesionId()).isEqualTo(sesion);
    }

    @Test
    @DisplayName("limite: revocada y vencida en el mismo instante es valida")
    void venceEnElMismoInstante() {
        assertThat(new RevocacionDeSesion(UUID.randomUUID(), UUID.randomUUID(), REVOCADA, REVOCADA).venceEn())
                .isEqualTo(REVOCADA);
    }

    @Test
    @DisplayName("invalido: vacio, no-objeto, sin sesion, fecha mal escrita o vencimiento anterior se rechazan")
    void invalidos() {
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson("[1]")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson("{no es json"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson(
                        """
                        {"usuarioId":"%s","revocadaEn":"%s","venceEn":"%s"}
                        """
                                .formatted(UUID.randomUUID(), REVOCADA, VENCE)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sesionId");
        assertThatThrownBy(() -> RevocacionDeSesion.desdeJson(
                        """
                        {"sesionId":"%s","usuarioId":"%s","revocadaEn":"ayer","venceEn":"%s"}
                        """
                                .formatted(UUID.randomUUID(), UUID.randomUUID(), VENCE)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RevocacionDeSesion(UUID.randomUUID(), UUID.randomUUID(), VENCE, REVOCADA))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
