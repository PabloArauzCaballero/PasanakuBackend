package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import bo.aportaya.identidad.infraestructura.LlavesDeFactor;
import bo.aportaya.identidad.infraestructura.VerificadorTotp;
import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import bo.aportaya.plataforma.dominio.Reloj;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * F1.S3.M2 · la regla del TOTP, sin base: vectores de RFC 6238 (apendice B, SHA-1, ultimos
 * seis digitos), ventana de ±1 paso, un solo uso por paso, y ningun codigo fijo.
 */
class VerificadorTotpTest {

    /** La semilla de los vectores de la RFC ("12345678901234567890" en Base32). SINTETICA. */
    private static final String SEMILLA = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private static final String LLAVE_1 = "llave-sintetica-uno-de-32-caracteres-min";
    private static final String LLAVE_2 = "llave-sintetica-dos-de-32-caracteres-min";

    @Test
    @DisplayName("correcto: los vectores de RFC 6238 se aceptan en su paso")
    void vectoresDelRfc() {
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "287082", Instant.ofEpochSecond(59), Optional.empty()))
                .contains(Instant.ofEpochSecond(30));
        assertThat(VerificadorTotp.pasoAceptado(
                        SEMILLA, "081804", Instant.ofEpochSecond(1111111109L), Optional.empty()))
                .contains(Instant.ofEpochSecond(1111111080L));
        assertThat(VerificadorTotp.pasoAceptado(
                        SEMILLA, "005924", Instant.ofEpochSecond(1234567890L), Optional.empty()))
                .isPresent();
        assertThat(VerificadorTotp.pasoAceptado(
                        SEMILLA, "279037", Instant.ofEpochSecond(2000000000L), Optional.empty()))
                .isPresent();
    }

    @Test
    @DisplayName("invalido: 000000 no es el codigo y se rechaza (no hay codigo fijo)")
    void ceroCeroCeroNoEntra() {
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "000000", Instant.ofEpochSecond(59), Optional.empty()))
                .isEmpty();
        assertThat(VerificadorTotp.pasoAceptado(
                        SEMILLA, "000000", Instant.ofEpochSecond(2000000000L), Optional.empty()))
                .isEmpty();
    }

    @Test
    @DisplayName("limite: el codigo del paso anterior o siguiente se acepta (desfase de reloj); dos pasos, no")
    void ventanaDeUnPaso() {
        // 287082 es del paso [30, 60). A los 89 s (paso siguiente) todavia entra; a los 90 s ya son dos pasos.
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "287082", Instant.ofEpochSecond(89), Optional.empty()))
                .contains(Instant.ofEpochSecond(30));
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "287082", Instant.ofEpochSecond(0), Optional.empty()))
                .contains(Instant.ofEpochSecond(30));
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "287082", Instant.ofEpochSecond(90), Optional.empty()))
                .isEmpty();
    }

    @Test
    @DisplayName("invalido: un codigo ya usado (mismo paso) o de un paso anterior al ultimo uso se rechaza")
    void unSoloUso() {
        Optional<Instant> usado = Optional.of(Instant.ofEpochSecond(30));
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, "287082", Instant.ofEpochSecond(59), usado))
                .isEmpty();
        // El codigo del paso siguiente si entra despues del uso anterior.
        String siguiente = bo.aportaya.plataforma.web.clientes.CodigoTotp.de(SEMILLA, Instant.ofEpochSecond(60));
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, siguiente, Instant.ofEpochSecond(61), usado))
                .contains(Instant.ofEpochSecond(60));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "28708", "2870822", "28708a", " 287082", "２８７０８２"})
    @DisplayName("limite: lo que no son exactamente seis digitos ASCII se rechaza sin calcular")
    void malFormados(String codigo) {
        assertThat(VerificadorTotp.pasoAceptado(SEMILLA, codigo, Instant.ofEpochSecond(59), Optional.empty()))
                .isEmpty();
    }

    @Test
    @DisplayName("correcto: la semilla se descifra con la llave de SU version, aunque la vigente sea otra")
    void descifraConLaVersionDeLaFila() {
        var llaves = new LlavesDeFactor("1:" + LLAVE_1 + ",2:" + LLAVE_2, "2");
        var verificador = verificador(llaves);
        String cifradaConLaVieja = CifradorSimetrico.cifrar(SEMILLA, LLAVE_1);

        assertThat(verificador.semillaDe(UUID.randomUUID(), cifradaConLaVieja, (short) 1))
                .contains(SEMILLA);
    }

    @Test
    @DisplayName("invalido: version de llave retirada, llave equivocada o dato corrupto: el factor no autentica")
    void noDescifrable() {
        var llaves = new LlavesDeFactor("2:" + LLAVE_2, "2");
        var verificador = verificador(llaves);
        String cifradaConLaVieja = CifradorSimetrico.cifrar(SEMILLA, LLAVE_1);

        assertThat(verificador.semillaDe(UUID.randomUUID(), cifradaConLaVieja, (short) 1))
                .isEmpty();
        assertThat(verificador.semillaDe(UUID.randomUUID(), cifradaConLaVieja, (short) 2))
                .isEmpty();
        // Lo que sembraba 61_dev ('enc:v1:demo-totp-admin-dev') no es un cifrado valido.
        assertThat(verificador.semillaDe(UUID.randomUUID(), "enc:v1:demo-totp-admin-dev", (short) 2))
                .isEmpty();
    }

    @Test
    @DisplayName("invalido: un codigo mal formado se rechaza antes de tocar la base")
    void malFormadoSinBase() {
        var verificador = verificador(new LlavesDeFactor("1:" + LLAVE_1, "1"));
        // Datos y repositorio son null: si validar tocara la base, fallaria con NPE.
        assertThat(verificador.validar(UUID.randomUUID(), "abc")).isFalse();
        assertThat(verificador.validar(UUID.randomUUID(), null)).isFalse();
    }

    private static VerificadorTotp verificador(LlavesDeFactor llaves) {
        return new VerificadorTotp(
                null, null, llaves, Reloj.fijo(Instant.ofEpochSecond(59)), mock(PlatformTransactionManager.class));
    }
}
