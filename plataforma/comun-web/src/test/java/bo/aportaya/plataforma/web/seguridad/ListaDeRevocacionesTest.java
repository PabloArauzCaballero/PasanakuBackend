package bo.aportaya.plataforma.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListaDeRevocacionesTest {

    private static final Instant REVOCADA = Instant.parse("2026-11-24T15:00:00Z");
    private static final Instant VENCE = Instant.parse("2026-11-24T15:15:00Z");

    private final ListaDeRevocaciones lista = new ListaDeRevocaciones();

    @Test
    @DisplayName("correcto: una sesion revocada se reconoce hasta su vencimiento; otra sesion no")
    void reconoceLaRevocada() {
        UUID sesion = UUID.randomUUID();
        lista.registrar(new RevocacionDeSesion(sesion, UUID.randomUUID(), REVOCADA, VENCE));

        assertThat(lista.estaRevocada(sesion, REVOCADA.plusSeconds(1))).isTrue();
        assertThat(lista.estaRevocada(UUID.randomUUID(), REVOCADA.plusSeconds(1)))
                .isFalse();
    }

    @Test
    @DisplayName("limite: en el instante del vencimiento ya no se recuerda, y purgar la quita")
    void venceYSePurga() {
        UUID sesion = UUID.randomUUID();
        lista.registrar(new RevocacionDeSesion(sesion, UUID.randomUUID(), REVOCADA, VENCE));

        assertThat(lista.estaRevocada(sesion, VENCE)).isFalse();
        assertThat(lista.purgar(VENCE.minusSeconds(1))).isZero();
        assertThat(lista.purgar(VENCE)).isEqualTo(1);
        assertThat(lista.tamano()).isZero();
    }

    @Test
    @DisplayName("limite: la misma sesion dos veces conserva el vencimiento mas lejano, no el ultimo que llega")
    void duplicadoConservaElMasLejano() {
        UUID sesion = UUID.randomUUID();
        UUID usuario = UUID.randomUUID();
        lista.registrar(new RevocacionDeSesion(sesion, usuario, REVOCADA, VENCE.plusSeconds(60)));
        lista.registrar(new RevocacionDeSesion(sesion, usuario, REVOCADA, VENCE));

        assertThat(lista.tamano()).isEqualTo(1);
        assertThat(lista.estaRevocada(sesion, VENCE.plusSeconds(30))).isTrue();
    }

    @Test
    @DisplayName("al arrancar la lista no esta al dia: recien despues de marcarla lo esta")
    void naceSinEstarAlDia() {
        assertThat(lista.estaAlDia()).isFalse();
        lista.marcarAlDia();
        assertThat(lista.estaAlDia()).isTrue();
    }
}
