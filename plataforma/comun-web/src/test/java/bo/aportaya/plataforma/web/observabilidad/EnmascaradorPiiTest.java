package bo.aportaya.plataforma.web.observabilidad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EnmascaradorPiiTest {

    private final EnmascaradorPii enmascarador = new EnmascaradorPii();

    @Test
    @DisplayName("Enmascara secretos, contraseñas, bearer tokens y claves")
    void enmascaraSecretos() {
        String mensaje = "Intento fallido con contrasena=\"MiClave123!\" y token: abcdef123456";
        String resultado = enmascarador.transform(null, mensaje);

        assertThat(resultado).doesNotContain("MiClave123!");
        assertThat(resultado).doesNotContain("abcdef123456");
        assertThat(resultado).contains("contrasena=\"[PROTEGIDO]\"");
        assertThat(resultado).contains("token=\"[PROTEGIDO]\"");
    }

    @Test
    @DisplayName("Enmascara números de cuenta o tarjetas dejando solo últimos 4 dígitos")
    void enmascaraCuentasBancarias() {
        String mensaje = "Pago transferido a la cuenta 10000034871928 del usuario";
        String resultado = enmascarador.transform(null, mensaje);

        assertThat(resultado).doesNotContain("10000034871928");
        assertThat(resultado).contains("****1928");
    }

    @Test
    @DisplayName("Enmascara correos electrónicos")
    void enmascaraCorreos() {
        String mensaje = "Notificación enviada a pablo.arauz@aportaya.bo con éxito";
        String resultado = enmascarador.transform(null, mensaje);

        assertThat(resultado).doesNotContain("pablo.arauz@");
        assertThat(resultado).contains("p***@aportaya.bo");
    }

    @Test
    @DisplayName("Texto sin PII se mantiene inalterado")
    void textoSinPiiInalterado() {
        String mensaje = "CU-18 ejecutado con éxito en 12ms para estado PENDIENTE";
        String resultado = enmascarador.transform(null, mensaje);

        assertThat(resultado).isEqualTo(mensaje);
    }
}
