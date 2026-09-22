package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.identidad.infraestructura.Argon2Hasheador;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-01 · la credencial nace con la cuenta.
 *
 * <p><b>Por que existe esta prueba.</b> El alta creaba la persona y no creaba su
 * credencial. Los ocho pasos terminaban bien, la fila de {@code usuario} quedaba
 * escrita, y despues el formulario de ingreso pedia una contrasena que nunca se habia
 * elegido: una cuenta a la que no se podia entrar. No lo detectaba nadie porque cada
 * pieza, por separado, funcionaba.
 */
class CU01CredencialTest extends BaseDeCU01 {

    private static final Argon2Hasheador HASHEADOR = new Argon2Hasheador("pimienta-de-prueba");

    @Test
    @DisplayName("Dado un alta válida · Cuando se registra · Entonces queda una credencial con la que se puede entrar")
    void laCredencialQuedaGuardadaYVerifica() {
        char[] clave = "una-clave-larga-2026".toCharArray();
        var salida = registrarConClave("+59177000501", "5000001", clave.clone());

        var hash = hashDeLaCredencial(salida.usuarioId());
        assertThat(hash)
                .as("sin credencial, la cuenta existe y no se puede usar")
                .isPresent();

        // Lo que importa no es que haya una fila: es que el ingreso pueda verificar
        // contra ella. Se comprueba con el mismo hasheador que usa CU-04.
        assertThat(HASHEADOR.coincide("una-clave-larga-2026".toCharArray(), hash.get()))
                .as("la clave elegida en el alta tiene que servir para ingresar")
                .isTrue();
        assertThat(HASHEADOR.coincide("otra-clave-cualquiera".toCharArray(), hash.get()))
                .isFalse();
    }

    @Test
    @DisplayName("Dado un alta válida · Cuando se registra · Entonces la credencial guarda con qué KDF se calculó")
    void quedaRegistradoElAlgoritmo() {
        var salida = registrarConClave("+59177000502", "5000002", "una-clave-larga-2026".toCharArray());

        // Endurecer el KDF maniana no puede invalidar los hashes de ayer: hay que saber
        // con que se calculo cada uno.
        assertThat(algoritmoDeLaCredencial(salida.usuarioId())).isEqualTo("ARGON2ID");
    }

    @Test
    @DisplayName("Dada una clave más corta que la política · Cuando se registra · Entonces no se crea el usuario")
    void laClaveCortaNoDejaUsuarioAMedias() {
        long antes = usuariosTotales();

        assertThatThrownBy(() -> registrarConClave("+59177000503", "5000003", "corta".toCharArray()))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("corta");

        // La clave se evalua ANTES de escribir: rechazarla despues dejaria un usuario
        // sin credencial, que es justo el estado que este caso de uso evita.
        assertThat(usuariosTotales()).isEqualTo(antes);
    }

    @Test
    @DisplayName("Dada una clave que contiene el teléfono · Cuando se registra · Entonces se rechaza")
    void laClaveDerivadaDeDatosPersonalesSeRechaza() {
        assertThatThrownBy(() -> registrarConClave("+59177000504", "5000004", "mi77000504segura".toCharArray()))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("telefono");
    }
}
