package bo.aportaya.nucleofinanciero.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H3.S1 · la maquina de estados del retiro, sin Spring y sin base: toda la tabla,
 * validas e invalidas, una por una.
 */
class EstadoDeRetiroTest {

    private static final Set<EstadoDeRetiro> TODOS = EnumSet.allOf(EstadoDeRetiro.class);

    @Test
    @DisplayName("PENDIENTE puede pasar a EN_REVISION, AUTORIZADA o RECHAZADA, y a nada mas")
    void desdePendiente() {
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.EN_REVISION))
                .isTrue();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.AUTORIZADA))
                .isTrue();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.RECHAZADA))
                .isTrue();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.EN_PROCESO))
                .isFalse();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.PAGADA)).isFalse();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.REVERSADA))
                .isFalse();
        assertThat(EstadoDeRetiro.PENDIENTE.puedePasarA(EstadoDeRetiro.PENDIENTE))
                .isFalse();
    }

    @Test
    @DisplayName("EN_REVISION solo puede pasar a AUTORIZADA o RECHAZADA")
    void desdeEnRevision() {
        assertThat(EstadoDeRetiro.EN_REVISION.puedePasarA(EstadoDeRetiro.AUTORIZADA))
                .isTrue();
        assertThat(EstadoDeRetiro.EN_REVISION.puedePasarA(EstadoDeRetiro.RECHAZADA))
                .isTrue();
        for (EstadoDeRetiro invalido :
                EnumSet.complementOf(EnumSet.of(EstadoDeRetiro.AUTORIZADA, EstadoDeRetiro.RECHAZADA))) {
            assertThat(EstadoDeRetiro.EN_REVISION.puedePasarA(invalido))
                    .as("EN_REVISION -> %s tiene que ser invalida", invalido)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("AUTORIZADA solo puede pasar a EN_PROCESO o RECHAZADA")
    void desdeAutorizada() {
        assertThat(EstadoDeRetiro.AUTORIZADA.puedePasarA(EstadoDeRetiro.EN_PROCESO))
                .isTrue();
        assertThat(EstadoDeRetiro.AUTORIZADA.puedePasarA(EstadoDeRetiro.RECHAZADA))
                .isTrue();
        assertThat(EstadoDeRetiro.AUTORIZADA.puedePasarA(EstadoDeRetiro.PAGADA)).isFalse();
        assertThat(EstadoDeRetiro.AUTORIZADA.puedePasarA(EstadoDeRetiro.PENDIENTE))
                .isFalse();
    }

    @Test
    @DisplayName("EN_PROCESO solo puede pasar a PAGADA o RECHAZADA")
    void desdeEnProceso() {
        assertThat(EstadoDeRetiro.EN_PROCESO.puedePasarA(EstadoDeRetiro.PAGADA)).isTrue();
        assertThat(EstadoDeRetiro.EN_PROCESO.puedePasarA(EstadoDeRetiro.RECHAZADA))
                .isTrue();
        assertThat(EstadoDeRetiro.EN_PROCESO.puedePasarA(EstadoDeRetiro.AUTORIZADA))
                .isFalse();
        assertThat(EstadoDeRetiro.EN_PROCESO.puedePasarA(EstadoDeRetiro.EN_REVISION))
                .isFalse();
    }

    @Test
    @DisplayName("PAGADA solo puede pasar a REVERSADA — es la unica correccion valida (append-only)")
    void desdePagada() {
        assertThat(EstadoDeRetiro.PAGADA.puedePasarA(EstadoDeRetiro.REVERSADA)).isTrue();
        for (EstadoDeRetiro invalido : EnumSet.complementOf(EnumSet.of(EstadoDeRetiro.REVERSADA))) {
            assertThat(EstadoDeRetiro.PAGADA.puedePasarA(invalido))
                    .as("PAGADA -> %s tiene que ser invalida", invalido)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("RECHAZADA y REVERSADA son estados finales: no salen a ningun lado")
    void estadosFinales() {
        for (EstadoDeRetiro destino : TODOS) {
            assertThat(EstadoDeRetiro.RECHAZADA.puedePasarA(destino)).isFalse();
            assertThat(EstadoDeRetiro.REVERSADA.puedePasarA(destino)).isFalse();
        }
    }

    @Test
    @DisplayName("ningun estado se transiciona a si mismo")
    void sinAutotransicion() {
        for (EstadoDeRetiro estado : TODOS) {
            assertThat(estado.puedePasarA(estado)).isFalse();
        }
    }
}
