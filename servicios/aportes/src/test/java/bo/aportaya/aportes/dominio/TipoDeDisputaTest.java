package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import org.junit.jupiter.api.Test;

class TipoDeDisputaTest {

    @Test
    void aceptaSoloLosTiposAdmitidosPorLaBase() {
        assertThat(TipoDeDisputa.exigir("CONTRACARGO")).isEqualTo("CONTRACARGO");
        assertThatThrownBy(() -> TipoDeDisputa.exigir(null))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no admitido");
        assertThatThrownBy(() -> TipoDeDisputa.exigir("FRAUDE_DECLARADO"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no admitido");
    }
}
