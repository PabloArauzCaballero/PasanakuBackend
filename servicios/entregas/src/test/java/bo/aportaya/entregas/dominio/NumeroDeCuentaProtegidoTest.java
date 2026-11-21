package bo.aportaya.entregas.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.CifradorSimetrico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SEG-007: el cifrado que se guarda para pagar sale del mismo numero que el enmascarado que se muestra y el hash que
 * deduplica. Lo usa {@code CU18RegistrarCuentaDestino}; el cliente ya no manda ningun cifrado (prueba web del 400).
 */
class NumeroDeCuentaProtegidoTest {

    private static final String PIMIENTA = "pimienta-de-prueba-no-es-la-de-produccion";
    private static final String LLAVE = "llave-de-cifrado-de-prueba-no-es-la-de-produccion";
    private static final String NUMERO = "4012345678";

    @Test
    @DisplayName("descifrar lo que se guarda devuelve exactamente el numero del que salen el enmascarado y el hash")
    void idaYVuelta() {
        var protegido = NumeroDeCuentaProtegido.de(NUMERO, PIMIENTA, 1, LLAVE);

        assertThat(protegido.cifrado()).startsWith(CifradorSimetrico.PREFIJO).doesNotContain(NUMERO);
        assertThat(CifradorSimetrico.descifrar(protegido.cifrado(), LLAVE)).isEqualTo(NUMERO);
        assertThat(protegido.enmascarada()).isEqualTo(CuentaEnmascarada.de(NUMERO, PIMIENTA, 1));
    }

    @Test
    @DisplayName("otro numero da otro enmascarado y otro cifrado: no hay forma de mezclar dos cuentas")
    void otroNumeroOtraCuenta() {
        var una = NumeroDeCuentaProtegido.de(NUMERO, PIMIENTA, 1, LLAVE);
        var otra = NumeroDeCuentaProtegido.de("4098765432", PIMIENTA, 1, LLAVE);

        assertThat(otra.enmascarada().hash()).isNotEqualTo(una.enmascarada().hash());
        assertThat(CifradorSimetrico.descifrar(otra.cifrado(), LLAVE)).isEqualTo("4098765432");
    }

    @Test
    @DisplayName("F1.L2.M07 · el cifrado usa su llave propia: la pimienta del hash no lo descifra")
    void laPimientaNoDescifra() {
        var protegido = NumeroDeCuentaProtegido.de(NUMERO, PIMIENTA, 1, LLAVE);

        assertThatThrownBy(() -> CifradorSimetrico.descifrar(protegido.cifrado(), PIMIENTA))
                .isInstanceOf(SecurityException.class);
    }
}
