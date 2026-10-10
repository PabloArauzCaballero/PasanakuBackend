package bo.aportaya.inversiones.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** TOTP de RFC 6238 (vectores del apendice B, SHA-1, 6 digitos = los 6 ultimos de los 8 publicados). */
class CodigoTotpTest {

    private static final String SEMILLA = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"; // "12345678901234567890"

    @Test
    @DisplayName("correcto · coincide con los vectores publicados de RFC 6238")
    void vectoresDelRfc() {
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(59))).isEqualTo("287082");
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(1111111109L))).isEqualTo("081804");
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(1111111111L))).isEqualTo("050471");
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(1234567890L))).isEqualTo("005924");
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(2000000000L))).isEqualTo("279037");
    }

    @Test
    @DisplayName("limite · el codigo cambia al cruzar la ventana de 30 segundos y no antes")
    void ventanaDeTreintaSegundos() {
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(30)))
                .isEqualTo(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(59)));
        assertThat(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(59)))
                .isNotEqualTo(CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(60)));
    }

    @Test
    @DisplayName("rechaza · una semilla que no es Base32")
    void semillaInvalida() {
        assertThatThrownBy(() -> CodigoTotp.de("no-es-base32-1!", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
