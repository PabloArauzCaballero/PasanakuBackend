package bo.aportaya.nucleofinanciero.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.web.generado.modelo.Importe;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L1.M01 · todo importe que entra por el contrato de nucleo-financiero pasa por {@code MapeoDeBilletera.dinero}:
 * recarga, retiro, transferencia, retencion, bloqueo, QR y conciliacion. Ahi se rechaza el negativo (400). Puro.
 */
class MapeoDeBilleteraImporteTest {

    private static Importe bob(String monto) {
        var d = new Importe();
        d.setMonto(monto);
        d.setMoneda(Importe.MonedaEnum.BOB);
        return d;
    }

    @Test
    @DisplayName("invalido: un importe negativo se rechaza como entrada mal formada")
    void negativo() {
        assertThatThrownBy(() -> MapeoDeBilletera.dinero(bob("-50.00")))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("monto");
        assertThatThrownBy(() -> MapeoDeBilletera.dinero(bob("-0.01")))
                .isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    @DisplayName("limite: cero y un centavo pasan (si un caso exige mayor que cero, lo dice su regla)")
    void limites() {
        assertThat(MapeoDeBilletera.dinero(bob("0.00")).monto()).isEqualByComparingTo("0.00");
        assertThat(MapeoDeBilletera.dinero(bob("0.01")).monto()).isEqualByComparingTo("0.01");
    }

    @Test
    @DisplayName("correcto: el importe y la moneda llegan intactos")
    void correcto() {
        var d = MapeoDeBilletera.dinero(bob("350.00"));
        assertThat(d.monto()).isEqualByComparingTo("350.00");
        assertThat(d.moneda().name()).isEqualTo("BOB");
    }
}
