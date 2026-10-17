package bo.aportaya.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.Moneda;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DatosSinteticosTest {

    private static List<String> muestra(DatosSinteticos d) {
        return List.of(
                d.nombres(),
                d.apellidos(),
                d.correo(),
                d.telefono(),
                d.documento(),
                d.nacimiento().toString(),
                d.monto("10.00", "500.00", Moneda.BOB).toString());
    }

    @Test
    @DisplayName("correcto: la misma semilla da exactamente los mismos datos; otra semilla, otros")
    void determinista() {
        assertThat(muestra(DatosSinteticos.conSemilla(7))).isEqualTo(muestra(DatosSinteticos.conSemilla(7)));
        assertThat(muestra(DatosSinteticos.conSemilla(7))).isNotEqualTo(muestra(DatosSinteticos.conSemilla(8)));
    }

    @Test
    @DisplayName("límite: los montos caen dentro del rango, con dos decimales, incluso con rango cero")
    void montos() {
        var d = DatosSinteticos.porDefecto();
        IntStream.range(0, 500).forEach(i -> {
            var m = d.monto("0.01", "0.03", Moneda.BOB);
            assertThat(m.monto()).isBetween(new java.math.BigDecimal("0.01"), new java.math.BigDecimal("0.03"));
            assertThat(m.monto().scale()).isEqualTo(2);
        });
        assertThat(d.monto("5.00", "5.00", Moneda.USD).monto()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("límite: correos y teléfonos no se repiten dentro de una instancia y nunca son de dominios reales")
    void unicos() {
        var d = DatosSinteticos.porDefecto();
        var correos = new HashSet<String>();
        var telefonos = new HashSet<String>();
        IntStream.range(0, 300).forEach(i -> {
            correos.add(d.correo());
            telefonos.add(d.telefono());
        });
        assertThat(correos).hasSize(300).allSatisfy(c -> assertThat(c).endsWith("@sintetico.pasanaku.test"));
        assertThat(telefonos)
                .hasSize(300)
                .allSatisfy(t -> assertThat(t).startsWith("+59178").hasSize(12));
        assertThat(d.nacimiento()).isBefore(LocalDate.now().minusYears(20));
    }

    @Test
    @DisplayName("inválido: un rango con más de dos decimales no se acepta (no se redondea en silencio)")
    void rangoInvalido() {
        assertThatThrownBy(() -> DatosSinteticos.porDefecto().monto("0.001", "1.00", Moneda.BOB))
                .isInstanceOf(ArithmeticException.class);
    }
}
