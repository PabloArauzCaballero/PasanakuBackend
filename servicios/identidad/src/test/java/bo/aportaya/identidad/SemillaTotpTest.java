package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.aplicacion.EnrolarTotp;
import bo.aportaya.identidad.infraestructura.SemillaTotp;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import bo.aportaya.plataforma.web.clientes.CodigoTotp;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.S3.M2 · la semilla nueva es Base32 de 160 bits y la URI la lee una app de autenticacion. */
class SemillaTotpTest {

    @Test
    @DisplayName("correcto: 32 caracteres Base32 (160 bits) y distintas en cada enrolamiento")
    void semillaNueva() {
        var azar = new SecureRandom();
        var vistas = new HashSet<String>();
        for (int i = 0; i < 50; i++) {
            String semilla = SemillaTotp.nueva(azar);
            assertThat(semilla).hasSize(32).matches("[A-Z2-7]{32}");
            vistas.add(semilla);
        }
        assertThat(vistas).hasSize(50);
    }

    @Test
    @DisplayName("correcto: una semilla nueva produce codigos que el verificador acepta (ida y vuelta)")
    void idaYVuelta() {
        String semilla = SemillaTotp.nueva(new SecureRandom());
        Instant ahora = Instant.ofEpochSecond(1_800_000_015L);
        String codigo = CodigoTotp.de(semilla, ahora);
        assertThat(VerificadorTotp.pasoAceptado(semilla, codigo, ahora, Optional.empty()))
                .isPresent();
    }

    @Test
    @DisplayName("correcto: la URI otpauth lleva emisor, cuenta codificada y parametros de RFC 6238")
    void uri() {
        String uri = EnrolarTotp.uri("SINT 001", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        assertThat(uri)
                .startsWith("otpauth://totp/AportaYa%3ASINT%20001?")
                .contains("secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
                .contains("issuer=AportaYa")
                .contains("algorithm=SHA1&digits=6&period=30");
    }
}
