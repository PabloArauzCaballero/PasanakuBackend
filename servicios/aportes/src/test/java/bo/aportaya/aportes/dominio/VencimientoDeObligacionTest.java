package bo.aportaya.aportes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Las fechas contractual y de liquidacion de una obligacion, sin base y sin reloj. */
class VencimientoDeObligacionTest {

    private static Map<LocalDate, String> feriados(LocalDate... dias) {
        Map<LocalDate, String> m = new HashMap<>();
        for (LocalDate d : dias) {
            m.put(d, "feriado de prueba " + d);
        }
        return m;
    }

    @Test
    @DisplayName("Un vencimiento en dia habil no se mueve ni mueve la gracia")
    void diaHabil() {
        var v = VencimientoDeObligacion.de(LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 15), Map.of());

        assertThat(v.fechaDeLiquidacion()).isEqualTo(v.fechaContractual());
        assertThat(v.finDeGracia()).isEqualTo(LocalDate.of(2026, 5, 15));
        assertThat(v.corridaPorNoHabil()).isFalse();
        assertThat(v.diasSalteados()).isEmpty();
    }

    @Test
    @DisplayName("Un feriado de viernes liquida el lunes y la gracia conserva sus tres dias")
    void feriadoDeViernes() {
        var v = VencimientoDeObligacion.de(
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), feriados(LocalDate.of(2026, 5, 1)));

        assertThat(v.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(v.finDeGracia()).isEqualTo(LocalDate.of(2026, 5, 7));
        assertThat(v.corridaPorNoHabil()).isTrue();
        assertThat(v.diasSalteados()).hasSize(3);
    }

    @Test
    @DisplayName("La gracia que cae en fin de semana pasa al lunes: nunca hacia atras")
    void graciaEnFinDeSemana() {
        // Vence el miercoles 6, gracia hasta el sabado 9: el fin de gracia es el lunes 11.
        var v = VencimientoDeObligacion.de(LocalDate.of(2026, 5, 6), LocalDate.of(2026, 5, 9), Map.of());

        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(v.finDeGracia()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(v.finDeGracia().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(v.finDeGracia()).isAfterOrEqualTo(v.finDeGraciaContractual());
    }

    @Test
    @DisplayName("Un fin de gracia anterior al vencimiento es un dato roto, no un plazo")
    void graciaAntesDelVencimiento() {
        assertThatThrownBy(() ->
                        VencimientoDeObligacion.de(LocalDate.of(2026, 5, 12), LocalDate.of(2026, 5, 10), Map.of()))
                .isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("Feriados que se encadenan con el fin de semana se saltan todos, con su motivo")
    void encadenados() {
        var v = VencimientoDeObligacion.de(
                LocalDate.of(2026, 2, 13),
                LocalDate.of(2026, 2, 13),
                feriados(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17)));

        // Viernes 13 es habil: no se mueve. El problema es solo si el vencimiento cae sobre el encadenado.
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 2, 13));

        var carnaval = VencimientoDeObligacion.de(
                LocalDate.of(2026, 2, 14),
                LocalDate.of(2026, 2, 14),
                feriados(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17)));
        assertThat(carnaval.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 2, 18));
        assertThat(carnaval.diasSalteados())
                .extracting(VencimientoDeObligacion.DiaSalteado::motivo)
                .containsExactly("Sabado", "Domingo", "feriado de prueba 2026-02-16", "feriado de prueba 2026-02-17");
    }
}
