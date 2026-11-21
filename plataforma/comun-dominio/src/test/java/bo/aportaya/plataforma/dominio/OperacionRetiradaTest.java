package bo.aportaya.plataforma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OperacionRetiradaTest {

    @Test
    @DisplayName("lleva su codigo AP-CU y su mensaje, y es un error del dominio")
    void codigoYMensaje() {
        var e = new OperacionRetirada(CodigoError.de(22, 10), "Ya no existe.");

        assertThat(e.codigo().valor()).isEqualTo("AP-CU22-10");
        assertThat(e).hasMessage("Ya no existe.").isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("sin codigo no se construye: un 410 sin codigo no le dice al cliente que se retiro")
    void sinCodigo() {
        assertThatThrownBy(() -> new OperacionRetirada(null, "x")).isInstanceOf(NullPointerException.class);
    }
}
