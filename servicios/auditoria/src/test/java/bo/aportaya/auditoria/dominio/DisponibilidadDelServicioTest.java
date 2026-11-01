package bo.aportaya.auditoria.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** sistemas/servicios · disponibilidad y presupuesto de error contra un SLO de 99,5 %. */
class DisponibilidadDelServicioTest {

    private static final BigDecimal SLO = new BigDecimal("99.5");

    private static DisponibilidadDelServicio medido(long medidos, long arriba) {
        return new DisponibilidadDelServicio("identidad", medidos, arriba, Optional.empty());
    }

    @Test
    @DisplayName("sin caidas · 100.00 % y el presupuesto entero")
    void sinCaidas() {
        assertThat(medido(43_200, 43_200).disponibilidad()).contains(new BigDecimal("100.00"));
        assertThat(medido(43_200, 43_200).presupuestoRestante(SLO)).contains(new BigDecimal("100.0"));
    }

    @Test
    @DisplayName("limite · exactamente en el SLO: 99.50 % y presupuesto 0.0")
    void enElLimite() {
        assertThat(medido(1_000, 995).disponibilidad()).contains(new BigDecimal("99.50"));
        assertThat(medido(1_000, 995).presupuestoRestante(SLO)).contains(new BigDecimal("0.0"));
    }

    @Test
    @DisplayName("mitad del presupuesto · 1 de 400 minutos caido es 50 % gastado")
    void mitad() {
        assertThat(medido(400, 399).presupuestoRestante(SLO)).contains(new BigDecimal("50.0"));
    }

    @Test
    @DisplayName("SLO incumplido · el presupuesto queda negativo, no en cero")
    void negativo() {
        assertThat(medido(1_000, 990).presupuestoRestante(SLO)).contains(new BigDecimal("-100.0"));
    }

    @Test
    @DisplayName("truncado · la disponibilidad no se redondea hacia arriba ni el presupuesto restante tampoco")
    void truncado() {
        // 2/3 = 66.666… → 66.66; gastado 1/3 de minuto sobre 0.015 permitidos = 6666.66… % → restante -6566.7
        assertThat(medido(3, 2).disponibilidad()).contains(new BigDecimal("66.66"));
        assertThat(medido(3, 2).presupuestoRestante(SLO)).contains(new BigDecimal("-6566.7"));
    }

    @Test
    @DisplayName("sin medicion · ni disponibilidad ni presupuesto: no hay dato, no es 0 ni 100")
    void sinMedicion() {
        assertThat(medido(0, 0).disponibilidad()).isEmpty();
        assertThat(medido(0, 0).presupuestoRestante(SLO)).isEmpty();
    }

    @Test
    @DisplayName("invalido · minutos incoherentes y SLO de 100 % se rechazan")
    void invalidos() {
        assertThatThrownBy(() -> medido(10, 11)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> medido(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> medido(10, 10).presupuestoRestante(new BigDecimal("100")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
