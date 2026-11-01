package bo.aportaya.plataforma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CifradorSimetricoTest {

    private static final String SECRETO = "clave-super-secreta-pimienta-banco-12345";
    private static final String NUMERO_CUENTA = "10000034871928";

    @Test
    @DisplayName("Cifrar y descifrar recupera exactamente el texto original")
    void cicloCompletoCifradoDescifrado() {
        String cifrado = CifradorSimetrico.cifrar(NUMERO_CUENTA, SECRETO);

        assertThat(cifrado).isNotNull();
        assertThat(cifrado).startsWith("enc:v1:");
        assertThat(cifrado).doesNotContain(NUMERO_CUENTA);

        String recuperado = CifradorSimetrico.descifrar(cifrado, SECRETO);
        assertThat(recuperado).isEqualTo(NUMERO_CUENTA);
    }

    @Test
    @DisplayName("Dos cifrados del mismo valor generan textos distintos por el IV aleatorio")
    void ivAleatorioGeneraSalidasDistintas() {
        String cifrado1 = CifradorSimetrico.cifrar(NUMERO_CUENTA, SECRETO);
        String cifrado2 = CifradorSimetrico.cifrar(NUMERO_CUENTA, SECRETO);

        assertThat(cifrado1).isNotEqualTo(cifrado2);
        assertThat(CifradorSimetrico.descifrar(cifrado1, SECRETO)).isEqualTo(NUMERO_CUENTA);
        assertThat(CifradorSimetrico.descifrar(cifrado2, SECRETO)).isEqualTo(NUMERO_CUENTA);
    }

    @Test
    @DisplayName("Descifrar con secreto incorrecto falla por autenticación GCM")
    void claveIncorrectaLanzaSecurityException() {
        String cifrado = CifradorSimetrico.cifrar(NUMERO_CUENTA, SECRETO);

        assertThatThrownBy(() -> CifradorSimetrico.descifrar(cifrado, "clave-incorrecta-atacante"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Error de autenticación o integridad");
    }

    @Test
    @DisplayName("Manipulación o corrupción del payload cifrado falla por verificación de autenticación")
    void datoManipuladoFalla() {
        String cifrado = CifradorSimetrico.cifrar(NUMERO_CUENTA, SECRETO);
        // Alterar un carácter del payload base64
        char ultimoChar = cifrado.charAt(cifrado.length() - 1);
        char alterado = (ultimoChar == 'A') ? 'B' : 'A';
        String manipulado = cifrado.substring(0, cifrado.length() - 1) + alterado;

        assertThatThrownBy(() -> CifradorSimetrico.descifrar(manipulado, SECRETO))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Valores nulos se manejan de forma segura y consistente")
    void manejoDeNulos() {
        assertThat(CifradorSimetrico.cifrar(null, SECRETO)).isNull();
        assertThat(CifradorSimetrico.descifrar(null, SECRETO)).isNull();

        assertThatThrownBy(() -> CifradorSimetrico.cifrar("algo", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CifradorSimetrico.descifrar("enc:v1:algo", ""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
