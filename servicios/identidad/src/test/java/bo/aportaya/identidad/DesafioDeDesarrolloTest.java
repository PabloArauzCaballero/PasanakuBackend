package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.identidad.infraestructura.DesafioDeDesarrollo;
import bo.aportaya.identidad.infraestructura.DesafioLocal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El perfil `local` (el de TEST): el correo sale de verdad y el codigo fijo sigue entrando. */
class DesafioDeDesarrolloTest {

    private final CorreoEnMemoria correo = new CorreoEnMemoria();
    private final DesafioDeDesarrollo desafio = new DesafioDeDesarrollo(new DesafioLocal(correo));
    private final UUID usuario = UUID.randomUUID();

    @Test
    @DisplayName("correcto: CORREO manda el codigo real y ese codigo entra")
    void correoSaleYElCodigoRealEntra() {
        desafio.emitir(usuario, "CORREO", "ana@example.com");

        assertThat(correo.enviados).hasSize(1);
        assertThat(desafio.validar(usuario, "CORREO", correo.ultimo().codigo())).isTrue();
    }

    @Test
    @DisplayName("limite: con CORREO el codigo fijo de desarrollo tambien entra")
    void codigoFijoSigueEntrando() {
        desafio.emitir(usuario, "CORREO", "ana@example.com");

        assertThat(desafio.validar(usuario, "CORREO", DesafioDeDesarrollo.CODIGO))
                .isTrue();
    }

    @Test
    @DisplayName("limite: TOTP no manda correo y sigue entrando con el codigo fijo")
    void totpSinCambios() {
        desafio.emitir(usuario, "TOTP", "ana@example.com");

        assertThat(correo.enviados).isEmpty();
        assertThat(desafio.validar(usuario, "TOTP", DesafioDeDesarrollo.CODIGO)).isTrue();
    }

    @Test
    @DisplayName("invalido: un codigo que no es ni el enviado ni el fijo no entra")
    void codigoAjenoNoEntra() {
        desafio.emitir(usuario, "CORREO", "ana@example.com");
        String otro = correo.ultimo().codigo().equals("111111") ? "222222" : "111111";

        assertThat(desafio.validar(usuario, "CORREO", otro)).isFalse();
        assertThat(desafio.validar(usuario, "TOTP", otro)).isFalse();
    }
}
