package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F3.L2.M01 · el adaptador provisorio del metodo de desembolso (DECISION_REQUIRED), en sus tres niveles. */
class MetodoDeDesembolsoConfiguradoTest {

    @Test
    @DisplayName("correcto: devuelve el metodo configurado para cualquier beneficiario")
    void devuelveElConfigurado() {
        assertThat(new MetodoDeDesembolsoConfigurado("TRANSFERENCIA_BANCARIA").para(UUID.randomUUID()))
                .contains("TRANSFERENCIA_BANCARIA");
    }

    @Test
    @DisplayName("limite: la compensacion interna esta en el CHECK de la base pero no es un desembolso al beneficiario")
    void compensacionInternaNoLevanta() {
        assertThatThrownBy(() -> new MetodoDeDesembolsoConfigurado("COMPENSACION_INTERNA"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("invalido: encendida sin metodo, el proceso no levanta")
    void sinMetodoNoLevanta() {
        assertThatThrownBy(() -> new MetodoDeDesembolsoConfigurado(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DECISION_REQUIRED");
    }
}
