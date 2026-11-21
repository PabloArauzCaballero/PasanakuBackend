package bo.aportaya.nucleofinanciero.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.dominio.SelloDelDia.Eslabon;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M13 · el sello del cierre diario cubre lo que dice proteger. Puro: sin base ni Spring. */
class SelloDelDiaTest {

    private static final UUID CUENTA = UUID.fromString("eeeeeeee-0000-4000-8000-0000000000c1");
    private static final LocalDate DIA = LocalDate.of(2026, 11, 20);
    private static final BigDecimal CIEN = new BigDecimal("100.00");
    private static final BigDecimal CERO = new BigDecimal("0.00");

    private static String sello(String moneda, BigDecimal disponible, BigDecimal retenido, String anterior) {
        return SelloDelDia.sellar(CUENTA, DIA, moneda, disponible, retenido, anterior);
    }

    @Test
    @DisplayName("el material es canonico: cuenta, fecha, moneda, importes a dos decimales y sello anterior")
    void materialCanonico() {
        assertThat(SelloDelDia.material(CUENTA, DIA, "BOB", new BigDecimal("100"), CERO, null))
                .isEqualTo(CUENTA + "|2026-11-20|BOB|100.00|0.00|-");
        // La escala no cambia el sello: 100 y 100.00 son el mismo saldo.
        assertThat(sello("BOB", new BigDecimal("100"), CERO, null)).isEqualTo(sello("BOB", CIEN, CERO, null));
    }

    @Test
    @DisplayName("cambiar el saldo disponible, el retenido, la moneda o el sello anterior cambia el sello")
    void cadaCampoCuenta() {
        String base = sello("BOB", CIEN, CERO, "a".repeat(64));
        assertThat(sello("BOB", new BigDecimal("100.01"), CERO, "a".repeat(64))).isNotEqualTo(base);
        assertThat(sello("BOB", CIEN, new BigDecimal("0.01"), "a".repeat(64))).isNotEqualTo(base);
        assertThat(sello("USD", CIEN, CERO, "a".repeat(64))).isNotEqualTo(base);
        assertThat(sello("BOB", CIEN, CERO, "b".repeat(64))).isNotEqualTo(base);
        assertThat(sello("BOB", CIEN, CERO, null)).isNotEqualTo(base);
    }

    private static List<Eslabon> cadenaDe(int dias) {
        List<Eslabon> cadena = new ArrayList<>();
        String anterior = null;
        for (int i = 0; i < dias; i++) {
            LocalDate fecha = DIA.plusDays(i);
            BigDecimal saldo = CIEN.add(BigDecimal.valueOf(i));
            String s = SelloDelDia.sellar(CUENTA, fecha, "BOB", saldo, CERO, anterior);
            cadena.add(new Eslabon(CUENTA, fecha, "BOB", saldo, CERO, s, anterior));
            anterior = s;
        }
        return cadena;
    }

    @Test
    @DisplayName("una cadena intacta no tiene ruptura; una vacia tampoco")
    void cadenaIntacta() {
        assertThat(SelloDelDia.primeraRuptura(cadenaDe(4))).isEmpty();
        assertThat(SelloDelDia.primeraRuptura(List.of())).isEmpty();
    }

    @Test
    @DisplayName("alterar el saldo de un dia anterior senala ese dia, no uno posterior")
    void saldoAlterado() {
        var cadena = cadenaDe(4);
        var e = cadena.get(1);
        cadena.set(
                1,
                new Eslabon(
                        e.cuentaId(),
                        e.fecha(),
                        e.moneda(),
                        new BigDecimal("999.00"),
                        e.retenido(),
                        e.sello(),
                        e.selloAnterior()));
        assertThat(SelloDelDia.primeraRuptura(cadena)).map(Eslabon::fecha).contains(DIA.plusDays(1));
    }

    @Test
    @DisplayName("reemplazar un eslabon entero, con su sello recalculado, rompe el dia siguiente")
    void eslabonReemplazado() {
        var cadena = cadenaDe(4);
        var e = cadena.get(1);
        BigDecimal falso = new BigDecimal("999.00");
        String reSellado = SelloDelDia.sellar(e.cuentaId(), e.fecha(), "BOB", falso, CERO, e.selloAnterior());
        cadena.set(1, new Eslabon(e.cuentaId(), e.fecha(), "BOB", falso, CERO, reSellado, e.selloAnterior()));
        assertThat(SelloDelDia.primeraRuptura(cadena)).map(Eslabon::fecha).contains(DIA.plusDays(2));
    }

    @Test
    @DisplayName("cambiar la moneda de un dia lo senala")
    void monedaAlterada() {
        var cadena = cadenaDe(3);
        var e = cadena.get(2);
        cadena.set(
                2,
                new Eslabon(
                        e.cuentaId(), e.fecha(), "USD", e.disponible(), e.retenido(), e.sello(), e.selloAnterior()));
        assertThat(SelloDelDia.primeraRuptura(cadena)).map(Eslabon::fecha).contains(DIA.plusDays(2));
    }
}
