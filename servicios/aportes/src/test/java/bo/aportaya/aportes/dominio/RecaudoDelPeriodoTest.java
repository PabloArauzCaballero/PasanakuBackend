package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El calculo del faltante al corte, sin base de datos. */
class RecaudoDelPeriodoTest {

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static RecaudoDelPeriodo.Obligacion o(String esperado, String pagado, String mutual) {
        return new RecaudoDelPeriodo.Obligacion(UUID.randomUUID(), bob(esperado), bob(pagado), bob(mutual));
    }

    @Test
    @DisplayName(
            "Dado un pozo de Bs 6.000 (12 x 500) con 10 pagadas · Cuando se calcula el corte · Entonces confirmado 5.000, faltante 1.000 y dos pendientes de 500")
    void ejemploDelPlan() {
        List<RecaudoDelPeriodo.Obligacion> lista = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lista.add(o("500.00", "500.00", "0.00"));
        }
        lista.add(o("500.00", "0.00", "0.00"));
        lista.add(o("500.00", "0.00", "0.00"));

        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, lista);

        assertThat(r.pozo()).isEqualTo(bob("6000.00"));
        assertThat(r.confirmado()).isEqualTo(bob("5000.00"));
        assertThat(r.faltante()).isEqualTo(bob("1000.00"));
        assertThat(r.pendientes()).hasSize(2).allSatisfy(p -> assertThat(p.monto())
                .isEqualTo(bob("500.00")));
    }

    @Test
    @DisplayName(
            "Dado un pago parcial · Cuando se calcula · Entonces solo cuenta lo pagado y el pendiente es la diferencia")
    void pagoParcial() {
        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "300.00", "0.00")));

        assertThat(r.confirmado()).isEqualTo(bob("300.00"));
        assertThat(r.pendientes().get(0).monto()).isEqualTo(bob("200.00"));
    }

    @Test
    @DisplayName("Dado un pago de mas · Cuando se calcula · Entonces el exceso NO cuenta como caja de este pozo")
    void pagoDeMas() {
        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "700.00", "0.00")));

        assertThat(r.confirmado()).isEqualTo(bob("500.00"));
        assertThat(r.faltante()).isEqualTo(bob("0.00"));
        assertThat(r.pendientes()).isEmpty();
    }

    @Test
    @DisplayName(
            "Dado lo cubierto por el fondo mutual · Cuando se calcula · Entonces se cuenta aparte y el respaldo solo cubre lo que ninguno puso")
    void fondoMutualAparte() {
        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "0.00", "350.00")));

        assertThat(r.confirmado()).isEqualTo(bob("0.00"));
        assertThat(r.cubiertoMutual()).isEqualTo(bob("350.00"));
        assertThat(r.faltante()).isEqualTo(bob("150.00"));
    }

    @Test
    @DisplayName(
            "Dado un fondo mutual que cubre mas de lo que faltaba · Cuando se calcula · Entonces no se cuenta de mas (limite)")
    void mutualNoSuperaLoQueFalta() {
        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "400.00", "500.00")));

        assertThat(r.cubiertoMutual()).isEqualTo(bob("100.00"));
        assertThat(r.faltante()).isEqualTo(bob("0.00"));
    }

    @Test
    @DisplayName("Dado un pozo completo · Cuando se calcula · Entonces el faltante es cero y no hay pendientes")
    void pozoCompleto() {
        var r = RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "500.00", "0.00")));

        assertThat(r.faltante().esCero()).isTrue();
        assertThat(r.pendientes()).isEmpty();
    }

    @Test
    @DisplayName(
            "Dada una obligacion en otra moneda · Cuando se calcula · Entonces se rechaza AP-CU21-05 (no se suma sin tipo de cambio)")
    void monedasMezcladas() {
        var enDolares = new RecaudoDelPeriodo.Obligacion(
                UUID.randomUUID(), Dinero.de("100.00", Moneda.USD), Dinero.cero(Moneda.USD), Dinero.cero(Moneda.USD));

        assertThatThrownBy(
                        () -> RecaudoDelPeriodo.calcular(Moneda.BOB, List.of(o("500.00", "0.00", "0.00"), enDolares)))
                .isInstanceOfSatisfying(
                        ErrorDeNegocio.class,
                        e -> assertThat(e.codigo().valor()).isEqualTo("AP-CU21-05"));
    }
}
