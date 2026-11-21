package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.SecretoDeInvitacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * F1.L4.M03 · La clave de invitaciones se valida al construir el bean, es decir, al arrancar.
 *
 * <p>Antes el servicio quedaba sano y toda invitacion fallaba con 500 en el primer uso.
 */
class SecretoDeInvitacionArranqueTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"corta", "1234567890123456789012345678901"}) // 31: el borde de abajo
    @DisplayName("sin clave o con menos de 32 caracteres, el bean no se construye")
    void claveAusenteOCortaNoArranca(String clave) {
        assertThatThrownBy(() -> new SecretoDeInvitacion(clave))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVITACIONES_CLAVE")
                .hasMessageContaining("Fail closed");
    }

    @Test
    @DisplayName("con 32 caracteres exactos arranca y firma")
    void claveDe32Arranca() {
        var secreto = new SecretoDeInvitacion("12345678901234567890123456789012");

        assertThat(secreto.firmar("invitacion", "x")).hasSize(64);
    }
}
