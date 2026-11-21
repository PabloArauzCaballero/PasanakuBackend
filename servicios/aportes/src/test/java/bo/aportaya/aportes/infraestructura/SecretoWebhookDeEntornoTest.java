package bo.aportaya.aportes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M05 · el secreto del webhook es obligatorio y largo; si falta, aportes no arranca. Puro. */
class SecretoWebhookDeEntornoTest {

    private static final String VALIDO = "x".repeat(SecretoWebhookDeEntorno.LARGO_MINIMO);

    @Test
    @DisplayName("invalido: sin la variable, o en blanco, el arranque falla nombrando APORTES_WEBHOOK_SECRETO")
    void falta() {
        assertThatThrownBy(() -> new SecretoWebhookDeEntorno(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APORTES_WEBHOOK_SECRETO");
        assertThatThrownBy(() -> new SecretoWebhookDeEntorno("   ")).hasMessageContaining("APORTES_WEBHOOK_SECRETO");
    }

    @Test
    @DisplayName("limite: 31 caracteres no alcanzan; 32 si")
    void largo() {
        assertThatThrownBy(() -> new SecretoWebhookDeEntorno(VALIDO.substring(1)))
                .hasMessageContaining("menos de 32");
        assertThat(new SecretoWebhookDeEntorno(VALIDO).secretoDe("CUALQUIERA")).contains(VALIDO);
    }
}
