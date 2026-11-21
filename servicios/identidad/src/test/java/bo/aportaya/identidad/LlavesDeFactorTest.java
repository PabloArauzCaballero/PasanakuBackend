package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** F1.S3.M2 · sin llaves validas el proceso no arranca (regla 90.3.4). Llaves SINTETICAS. */
class LlavesDeFactorTest {

    private static final String LLAVE = "llave-sintetica-de-prueba-de-32-caracteres";

    @Test
    @DisplayName("correcto: dos versiones, la vigente es la pedida")
    void dosVersiones() {
        var llaves = new LlavesDeFactor("1:" + LLAVE + "-a, 2:" + LLAVE + "-b", "2");
        assertThat(llaves.vigente()).isEqualTo((short) 2);
        assertThat(llaves.llaveVigente()).isEqualTo(LLAVE + "-b");
        assertThat(llaves.llave((short) 1)).contains(LLAVE + "-a");
        assertThat(llaves.llave((short) 3)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("invalido: sin llaves no arranca")
    void sinLlaves(String configuradas) {
        assertThatThrownBy(() -> new LlavesDeFactor(configuradas, "1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("limite: una llave de 31 caracteres no alcanza; una de 32, si. El mensaje no muestra la llave")
    void largoMinimo() {
        String corta = "x".repeat(31);
        assertThatThrownBy(() -> new LlavesDeFactor("1:" + corta, "1"))
                .isInstanceOf(IllegalStateException.class)
                .message()
                .doesNotContain(corta);
        assertThat(new LlavesDeFactor("1:" + "x".repeat(32), "1").vigente()).isEqualTo((short) 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2", "0", "-1", "uno", ""})
    @DisplayName("invalido: version vigente ausente, cero, negativa o no numerica")
    void vigenteInvalida(String vigente) {
        assertThatThrownBy(() -> new LlavesDeFactor("1:" + LLAVE, vigente)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {LLAVE, "1" + LLAVE, ":" + LLAVE, "0:" + LLAVE})
    @DisplayName("invalido: entradas sin version, version 0 o mal formadas")
    void malFormadas(String configuradas) {
        assertThatThrownBy(() -> new LlavesDeFactor(configuradas, "1")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("invalido: version repetida")
    void repetida() {
        assertThatThrownBy(() -> new LlavesDeFactor("1:" + LLAVE + ",1:" + LLAVE + "-b", "1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
