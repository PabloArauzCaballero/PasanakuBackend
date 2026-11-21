package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.dominio.PuedeAutenticarse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Cada valor de {@code ck_usuario_estado} tiene su veredicto, y lo desconocido no entra. */
class PuedeAutenticarseTest {

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVO", "PRE_REGISTRO", "INCOMPLETO_KYC", "PENDIENTE_VERIFICACION"})
    @DisplayName("correcto: los estados del alta, del KYC y el activo entran")
    void habilitados(String estado) {
        assertThat(PuedeAutenticarse.segun(Optional.of(estado))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUSPENDIDO", "BLOQUEADO_SEGURIDAD", "BAJA_VOLUNTARIA", "ELIMINADO"})
    @DisplayName("invalido: suspendida, bloqueada, de baja o eliminada no entra")
    void noHabilitados(String estado) {
        assertThat(PuedeAutenticarse.segun(Optional.of(estado))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "activo", "ACTIVO ", "ESTADO_NUEVO_SIN_CLASIFICAR"})
    @DisplayName("limite: un estado que nadie clasifico, o mal escrito, no entra (falla cerrada)")
    void desconocidos(String estado) {
        assertThat(PuedeAutenticarse.segun(Optional.of(estado))).isFalse();
    }

    @Test
    @DisplayName("limite: sin fila de usuario, no entra")
    void sinUsuario() {
        assertThat(PuedeAutenticarse.segun(Optional.empty())).isFalse();
    }
}
