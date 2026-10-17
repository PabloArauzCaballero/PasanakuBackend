package bo.aportaya.plataforma.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PeriodoDeTableroTest {

    @Test
    @DisplayName("correcto: AAAA, AAAA-MM y AAAA-MM-DD dan su rango de días, inclusivo")
    void rangos() {
        assertThat(PeriodoDeTablero.de("2026").rango())
                .isEqualTo(Periodo.de(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));
        assertThat(PeriodoDeTablero.de("2024-02").rango().fin()).isEqualTo(LocalDate.of(2024, 2, 29));
        assertThat(PeriodoDeTablero.de("2026-10-15").rango().dias()).isEqualTo(1);
    }

    @Test
    @DisplayName("límite: un día común cierra solo ayer; el 1 del mes cierra también el mes; el 1 de enero, el año")
    void cerrados() {
        assertThat(PeriodoDeTablero.cerradosAl(LocalDate.of(2026, 10, 16)))
                .extracting(PeriodoDeTablero::codigo)
                .containsExactly("2026-10-15");
        assertThat(PeriodoDeTablero.cerradosAl(LocalDate.of(2026, 10, 1)))
                .extracting(PeriodoDeTablero::codigo)
                .containsExactly("2026-09-30", "2026-09");
        assertThat(PeriodoDeTablero.cerradosAl(LocalDate.of(2027, 1, 1)))
                .extracting(PeriodoDeTablero::codigo)
                .containsExactly("2026-12-31", "2026-12", "2026");
    }

    @Test
    @DisplayName("límite: el relleno trae días y meses cerrados, y los años cerrados que tocan; nunca el en curso")
    void relleno() {
        var r = PeriodoDeTablero.relleno(LocalDate.of(2026, 10, 16), 13, 2);

        assertThat(r)
                .extracting(PeriodoDeTablero::codigo)
                .startsWith("2026-10-14", "2026-10-15", "2025-09")
                .contains("2026-09", "2025")
                .doesNotContain("2026-10", "2026", "2026-10-16");
        assertThat(PeriodoDeTablero.relleno(LocalDate.of(2026, 10, 16), 0, 0)).isEmpty();
    }

    @Test
    @DisplayName("inválido: formatos fuera de AAAA[-MM[-DD]], fechas imposibles y relleno negativo se rechazan")
    void rechazos() {
        for (String malo : new String[] {"26", "2026-13", "2026-02-30", "09/2026", "abcd", "2026-1"}) {
            assertThatThrownBy(() -> PeriodoDeTablero.de(malo)).as(malo).isInstanceOf(ErrorDeDominio.class);
        }
        assertThatThrownBy(() -> PeriodoDeTablero.de(null)).isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> PeriodoDeTablero.relleno(LocalDate.now(), -1, 0))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> PeriodoDeTablero.relleno(LocalDate.now(), 0, -1))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> new PeriodoDeTablero(null, Periodo.de(LocalDate.now(), LocalDate.now())))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PeriodoDeTablero("2026", null)).isInstanceOf(NullPointerException.class);
    }
}
