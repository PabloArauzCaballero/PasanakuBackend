package bo.aportaya.garantia.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El calculo de lo que la reserva puede cubrir, sin base de datos. */
class RespaldoEmpresarialTest {

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static RespaldoEmpresarial.Reserva reserva(String reservado, String aplicado, String recuperado) {
        return new RespaldoEmpresarial.Reserva(bob(reservado), bob(aplicado), bob(recuperado), bob("0.00"));
    }

    @Test
    @DisplayName(
            "Dado un faltante de Bs 1.000 y una reserva de Bs 8.000 · Cuando se decide · Entonces cubre y no queda nada sin cubrir")
    void correcto() {
        var decision = RespaldoEmpresarial.decidir(reserva("8000.00", "0.00", "0.00"), bob("1000.00"));

        assertThat(decision.cubre()).isTrue();
        assertThat(decision.sinCubrir()).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName("Dado un faltante igual al disponible · Cuando se decide · Entonces cubre justo (limite)")
    void limiteExacto() {
        var decision = RespaldoEmpresarial.decidir(reserva("1000.00", "0.00", "0.00"), bob("1000.00"));

        assertThat(decision.cubre()).isTrue();
    }

    @Test
    @DisplayName(
            "Dado un faltante de un centavo mas que el disponible · Cuando se decide · Entonces NO cubre y dice cuanto falta (invalido)")
    void unCentavoDeMas() {
        var decision = RespaldoEmpresarial.decidir(reserva("1000.00", "0.00", "0.00"), bob("1000.01"));

        assertThat(decision.cubre()).isFalse();
        assertThat(decision.sinCubrir()).isEqualTo(bob("0.01"));
    }

    @Test
    @DisplayName(
            "Dada una recuperacion · Cuando se mira el disponible · Entonces NO se repone (no financia el pago de hoy) pero la exposicion baja")
    void recuperarNoReponeElDisponible() {
        var r = reserva("8000.00", "1000.00", "1000.00");

        assertThat(r.disponible()).isEqualTo(bob("7000.00"));
        assertThat(r.exposicion()).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName(
            "Dado un pago mayor a lo adelantado · Cuando se recupera · Entonces solo vuelve lo adelantado y el resto es excedente")
    void recuperarConExcedente() {
        var r = RespaldoEmpresarial.recuperar(bob("1000.00"), bob("400.00"), bob("700.00"));

        assertThat(r.aplicado()).isEqualTo(bob("600.00"));
        assertThat(r.excedente()).isEqualTo(bob("100.00"));
    }

    @Test
    @DisplayName(
            "Dada una linea ya recuperada del todo · Cuando llega otro pago · Entonces no se aplica nada (sin doble cobro)")
    void lineaAgotada() {
        var r = RespaldoEmpresarial.recuperar(bob("1000.00"), bob("1000.00"), bob("1000.00"));

        assertThat(r.aplicado().esCero()).isTrue();
        assertThat(r.excedente()).isEqualTo(bob("1000.00"));
    }
}
