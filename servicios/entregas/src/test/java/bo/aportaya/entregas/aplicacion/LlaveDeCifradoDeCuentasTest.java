package bo.aportaya.entregas.aplicacion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * F1.L2.M07: entregas no arranca sin una llave de cifrado propia (regla 90.3.4). Sin la propiedad, Spring ya no
 * resuelve {@code ${ENTREGAS_LLAVE_CIFRADO_CUENTAS}}; aca se prueba lo que Spring no ve: vacia, en blanco o igual a la
 * pimienta.
 */
class LlaveDeCifradoDeCuentasTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";

    @ParameterizedTest(name = "llave «{0}»")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", PIMIENTA})
    void sinLlavePropiaNoArranca(String llave) {
        assertThatThrownBy(() -> new CU18RegistrarCuentaDestino(
                        null, null, null, Reloj.delSistema(), PIMIENTA, llave, 1, 3, Duration.ofHours(24)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("llave-cifrado-cuentas");
    }
}
