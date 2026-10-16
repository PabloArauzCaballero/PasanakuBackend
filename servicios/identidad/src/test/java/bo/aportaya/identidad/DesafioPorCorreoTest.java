package bo.aportaya.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import bo.aportaya.identidad.infraestructura.DesafioLocal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/** El segundo factor por correo, contra un doble del enviador: correcto, limite e invalido. */
class DesafioPorCorreoTest {

    private static final String DESTINO = "ana@example.com";

    private final CorreoEnMemoria correo = new CorreoEnMemoria();
    private final DesafioLocal desafio = new DesafioLocal(correo);
    private final UUID usuario = UUID.randomUUID();

    @Test
    @DisplayName("correcto: el codigo sale una vez por correo de acceso y entra una sola vez")
    void codigoLlegaYValida() {
        desafio.emitir(usuario, "CORREO", DESTINO);

        assertThat(correo.enviados).hasSize(1);
        var enviado = correo.ultimo();
        assertThat(enviado.destino()).isEqualTo(DESTINO);
        assertThat(enviado.deAcceso()).isTrue();
        assertThat(enviado.codigo()).matches("\\d{6}");
        assertThat(enviado.vigencia()).isEqualTo(Duration.ofMinutes(5));

        assertThat(desafio.validar(usuario, "CORREO", enviado.codigo())).isTrue();
        assertThat(desafio.validar(usuario, "CORREO", enviado.codigo())).isFalse();
    }

    @Test
    @DisplayName("limite: pedirlo de nuevo reemplaza el codigo anterior")
    void segundoPedidoReemplazaAlPrimero() {
        desafio.emitir(usuario, "CORREO", DESTINO);
        String primero = correo.ultimo().codigo();
        String segundo;
        do {
            desafio.emitir(usuario, "CORREO", DESTINO);
            segundo = correo.ultimo().codigo();
        } while (segundo.equals(primero)); // uno en un millon: se pide otro

        assertThat(desafio.validar(usuario, "CORREO", primero)).isFalse();
        desafio.emitir(usuario, "CORREO", DESTINO);
        assertThat(desafio.validar(usuario, "CORREO", correo.ultimo().codigo())).isTrue();
    }

    @Test
    @DisplayName("limite: un factor que no viaja (TOTP) no manda ningun correo")
    void totpNoSaleDelProceso() {
        desafio.emitir(usuario, "TOTP", DESTINO);

        assertThat(correo.enviados).isEmpty();
    }

    @Test
    @DisplayName("invalido: si el proveedor falla, el ingreso no se rompe y no queda un codigo vivo")
    void proveedorCaido() {
        correo.fallar(new ResourceAccessException("gmail no responde"));

        assertThatCode(() -> desafio.emitir(usuario, "CORREO", DESTINO)).doesNotThrowAnyException();
        assertThat(correo.enviados).isEmpty();
        assertThat(desafio.validar(usuario, "CORREO", "000000")).isFalse();
    }

    @Test
    @DisplayName("invalido: sin destino no se envia nada y ningun codigo entra")
    void sinDestino() {
        assertThatCode(() -> desafio.emitir(usuario, "CORREO", " ")).doesNotThrowAnyException();

        assertThat(correo.enviados).isEmpty();
        assertThat(desafio.validar(usuario, "CORREO", "123456")).isFalse();
    }
}
