package bo.aportaya.grupos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** El corazon puro de H7.S1.M3: tres fechas distintas que no se pueden confundir. Sin base, sin reloj. */
class VencimientoLocalTest {

    private static final ZoneId LA_PAZ = ZoneId.of("America/La_Paz");
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static Map<LocalDate, String> feriados(LocalDate... dias) {
        Map<LocalDate, String> m = new HashMap<>();
        for (LocalDate d : dias) {
            m.put(d, "feriado de prueba " + d);
        }
        return m;
    }

    @Test
    @DisplayName("El borde del dia: 04:00Z es la medianoche de La Paz, y un segundo antes todavia es el dia anterior")
    void bordeDeMedianoche() {
        var justoAntes = VencimientoLocal.de(Instant.parse("2026-05-05T03:59:59Z"), LA_PAZ, Map.of());
        var justoEn = VencimientoLocal.de(Instant.parse("2026-05-05T04:00:00Z"), LA_PAZ, Map.of());

        assertThat(justoAntes.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(justoAntes.fechaEnUtc()).isEqualTo(LocalDate.of(2026, 5, 5));
        assertThat(justoEn.fechaContractual()).isEqualTo(LocalDate.of(2026, 5, 5));
        assertThat(justoEn.fechaEnUtc()).isEqualTo(LocalDate.of(2026, 5, 5));
    }

    @ParameterizedTest(name = "{0} en {1} es el dia {2}")
    @CsvSource({
        "2026-05-02T02:00:00Z, America/La_Paz, 2026-05-01",
        "2026-05-02T02:00:00Z, UTC, 2026-05-02",
        "2027-01-01T03:30:00Z, America/La_Paz, 2026-12-31",
        "2027-01-01T03:30:00Z, UTC, 2027-01-01",
        "2026-02-28T23:59:59Z, America/La_Paz, 2026-02-28",
        "2028-02-29T03:59:59Z, America/La_Paz, 2028-02-28",
        "2028-02-29T04:00:00Z, America/La_Paz, 2028-02-29",
    })
    void diaContractualSegunLaZona(String instante, String zona, String esperado) {
        assertThat(VencimientoLocal.diaEn(Instant.parse(instante), ZoneId.of(zona)))
                .isEqualTo(LocalDate.parse(esperado));
    }

    @Test
    @DisplayName("Un dia habil se liquida el mismo dia: contractual = liquidacion, sin dias saltados")
    void diaHabilNoSeCorre() {
        var v = VencimientoLocal.de(Instant.parse("2026-05-05T14:00:00Z"), LA_PAZ, Map.of()); // martes

        assertThat(v.fechaContractual()).isEqualTo(v.fechaDeLiquidacion());
        assertThat(v.corridaPorNoHabil()).isFalse();
        assertThat(v.diasSalteados()).isEmpty();
    }

    @Test
    @DisplayName("Un feriado de viernes corre a lunes: nunca a sabado, y nunca hacia atras")
    void feriadoDeViernes() {
        var v = VencimientoLocal.de(Instant.parse("2026-05-01T14:00:00Z"), LA_PAZ, feriados(LocalDate.of(2026, 5, 1)));

        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(v.fechaDeLiquidacion().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(v.fechaDeLiquidacion()).isAfterOrEqualTo(v.fechaContractual());
        assertThat(v.diasSalteados()).hasSize(3);
    }

    @Test
    @DisplayName("Feriados consecutivos pegados al fin de semana (carnaval de lunes y martes) se saltan todos")
    void feriadosEncadenados() {
        var carnaval = feriados(LocalDate.of(2026, 2, 16), LocalDate.of(2026, 2, 17));

        var v = VencimientoLocal.de(Instant.parse("2026-02-14T16:00:00Z"), LA_PAZ, carnaval); // sabado 14

        assertThat(v.fechaContractual()).isEqualTo(LocalDate.of(2026, 2, 14));
        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 2, 18));
        assertThat(v.diasSalteados())
                .extracting(VencimientoLocal.DiaNoHabil::fecha)
                .hasSize(4);
    }

    @Test
    @DisplayName("El motivo de cada dia saltado es el del calendario, y sabado o domingo se nombran")
    void motivos() {
        var v = VencimientoLocal.de(Instant.parse("2026-05-01T14:00:00Z"), LA_PAZ, feriados(LocalDate.of(2026, 5, 1)));

        assertThat(v.diasSalteados())
                .extracting(VencimientoLocal.DiaNoHabil::motivo)
                .containsExactly("feriado de prueba 2026-05-01", "Sabado", "Domingo");
    }

    @Test
    @DisplayName("Un feriado en fin de semana no se cuenta dos veces")
    void feriadoEnSabado() {
        var v = VencimientoLocal.de(Instant.parse("2026-08-08T14:00:00Z"), LA_PAZ, feriados(LocalDate.of(2026, 8, 8)));

        assertThat(v.fechaDeLiquidacion()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(v.diasSalteados()).hasSize(2);
    }

    @Test
    @DisplayName("Un calendario que no deja ningun dia habil en la ventana es un error de datos, no una fecha")
    void calendarioImposible() {
        Map<LocalDate, String> todos = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            todos.put(LocalDate.of(2026, 3, 1).plusDays(i), "bloqueo de prueba");
        }

        assertThatThrownBy(() -> VencimientoLocal.de(Instant.parse("2026-03-02T14:00:00Z"), LA_PAZ, todos))
                .isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    @DisplayName("El resultado no se puede alterar desde afuera")
    void listaInmutable() {
        var v = VencimientoLocal.de(Instant.parse("2026-05-01T14:00:00Z"), UTC, feriados(LocalDate.of(2026, 5, 1)));

        assertThatThrownBy(() -> v.diasSalteados().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
