package bo.aportaya.plataforma.web.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La eleccion de la credencial de servicio, en sus tres niveles: correcto, limite e invalido. */
class CredencialDeServicioConfiguradaTest {

    private static final URI IDENTIDAD = URI.create("http://identidad:8080");

    private static CredencialDeServicio con(String token, String telefono, String clave, String semilla, String fijo) {
        return CredencialDeServicioConfigurada.de(
                "entregas", "ENTREGAS", IDENTIDAD, Duration.ofSeconds(1), token, telefono, clave, semilla, fijo);
    }

    @Test
    @DisplayName("correcto: con cuenta de servicio completa entra por identidad y se renueva sola")
    void cuentaCompleta() {
        assertThat(con("", "+59170000000", "clave", "", "000000")).isInstanceOf(CredencialPorLogin.class);
        assertThat(con("", "+59170000000", "clave", "JBSWY3DPEHPK3PXP", "")).isInstanceOf(CredencialPorLogin.class);
    }

    @Test
    @DisplayName("limite: sin cuenta usa el token ya emitido, y sin ninguno de los dos no hay credencial")
    void sinCuenta() {
        assertThat(con("  tok.en.emitido  ", "", "", "", "").vigente()).contains("tok.en.emitido");
        assertThat(con("", "", "", "", "").vigente()).isEmpty();
    }

    @Test
    @DisplayName("invalido: una cuenta a medias no arranca y el mensaje nombra las variables del servicio")
    void cuentaAMedias() {
        assertThatThrownBy(() -> con("", "+59170000000", "", "", "000000"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENTREGAS_SERVICIO_CLAVE")
                .hasMessageContaining("entregas");
        assertThatThrownBy(() -> con("", "+59170000000", "clave", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENTREGAS_SERVICIO_TOTP_SEMILLA");
        assertThatThrownBy(() -> con("", "", "", "", "000000")).isInstanceOf(IllegalStateException.class);
    }
}
