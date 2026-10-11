package bo.aportaya.inversiones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El interes de un DPF contra valores calculados A MANO (no con la formula que se prueba):
 * los de la investigacion del modelo financiero (INFORME.md, seccion «Interes de DPF») y
 * los de PLAN §4.3. La tasa del 4 % es hipotetica de esos documentos, no una cotizacion.
 */
class CalculosDeDpfTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("10.000 al 4 % nominal por 180 dias: 197,26 sobre 365 y 200,00 sobre 360")
    void convencionesDeBase() {
        assertThat(DevengoDeDpf.interes(d("10000.00"), d("0.040000"), 180, 365)).isEqualByComparingTo("197.26");
        assertThat(DevengoDeDpf.interes(d("10000.00"), d("0.040000"), 180, 360)).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("PLAN 4.3: 600.000 al 4 % por 7 dias sobre 365 son 460,27")
    void ejemploDelPlan() {
        assertThat(DevengoDeDpf.interes(d("600000.00"), d("0.040000"), 7, 365)).isEqualByComparingTo("460.27");
    }

    @Test
    @DisplayName("la retencion se calcula sobre el interes y redondea half-even: 19,726 es 19,73")
    void retencion() {
        assertThat(DevengoDeDpf.retencion(d("197.26"), d("0.100000"))).isEqualByComparingTo("19.73");
        assertThat(DevengoDeDpf.retencion(d("56.25"), d("0.100000"))).isEqualByComparingTo("5.62"); // 5,625 -> par
    }

    @Test
    @DisplayName("el acumulado no pasa del plazo y los dias negativos valen cero")
    void limitesDelAcumulado() {
        assertThat(DevengoDeDpf.acumulado(d("10000.00"), d("0.040000"), 400, 180, 365))
                .isEqualByComparingTo("197.26");
        assertThat(DevengoDeDpf.acumulado(d("10000.00"), d("0.040000"), -3, 180, 365))
                .isEqualByComparingTo("0.00");
        assertThat(DevengoDeDpf.acumulado(d("10000.00"), d("0.040000"), 0, 180, 365))
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "la suma de los devengos diarios es EXACTAMENTE el interes del plazo (el residuo se asigna, no se pierde)")
    void sumaDeDevengosDiarios() {
        for (String principal : List.of("500.00", "1234.56", "10000.00", "99999.99")) {
            for (String tasa : List.of("0.040000", "0.045000", "0.073300")) {
                BigDecimal anterior = BigDecimal.ZERO;
                BigDecimal suma = BigDecimal.ZERO;
                for (int dia = 1; dia <= 180; dia++) {
                    BigDecimal acumulado = DevengoDeDpf.acumulado(d(principal), d(tasa), dia, 180, 365);
                    suma = suma.add(acumulado.subtract(anterior));
                    anterior = acumulado;
                }
                assertThat(suma)
                        .as("principal %s tasa %s", principal, tasa)
                        .isEqualByComparingTo(DevengoDeDpf.interes(d(principal), d(tasa), 180, 365));
            }
        }
    }

    @Test
    @DisplayName("un devengo diario nunca es negativo")
    void devengoMonotono() {
        BigDecimal anterior = BigDecimal.ZERO;
        for (int dia = 0; dia <= 200; dia++) {
            BigDecimal acumulado = DevengoDeDpf.acumulado(d("777.77"), d("0.040000"), dia, 180, 365);
            assertThat(acumulado.subtract(anterior).signum()).isGreaterThanOrEqualTo(0);
            anterior = acumulado;
        }
    }

    @Test
    @DisplayName("base de dias cero o dias negativos se rechazan")
    void entradasInvalidas() {
        assertThatThrownBy(() -> DevengoDeDpf.interes(d("1.00"), d("0.04"), 1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DevengoDeDpf.interes(d("1.00"), d("0.04"), -1, 365))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el neto de un DPF es principal + interes - retencion, sin residuo")
    void descomposicionDelDpf() {
        var d = Descomposicion.dpf(d("10000.00"), d("197.26"), d("19.73"));
        assertThat(d.neto()).isEqualByComparingTo("10177.53");
        assertThat(d.costoBase()).isEqualByComparingTo("10000.00");
        assertThat(d.principal().add(d.interes()).subtract(d.impuesto()).subtract(d.comision()))
                .isEqualByComparingTo(d.neto());
    }

    @Test
    @DisplayName("la division con residuo: 100,00 en tres partes a centavos suma 100,00 asignando el resto al ultimo")
    void residuoDeRedondeoAsignado() {
        // Mismo criterio que usa la liberacion de costo base: el ultimo se lleva lo que falta.
        BigDecimal costo = d("100.00");
        BigDecimal cuotas = d("3.000000");
        BigDecimal primera = ValoracionDeCuotas.costoBaseLiberado(costo, d("1.000000"), cuotas);
        BigDecimal segunda =
                ValoracionDeCuotas.costoBaseLiberado(costo.subtract(primera), d("1.000000"), cuotas.subtract(d("1")));
        BigDecimal tercera = ValoracionDeCuotas.costoBaseLiberado(
                costo.subtract(primera).subtract(segunda), d("1.000000"), d("1.000000"));
        assertThat(primera).isEqualByComparingTo("33.33");
        assertThat(segunda).isEqualByComparingTo("33.34"); // 66,67 / 2 = 33,335 -> par = 33,34
        assertThat(tercera).isEqualByComparingTo("33.33");
        assertThat(primera.add(segunda).add(tercera)).isEqualByComparingTo(costo.setScale(2, RoundingMode.UNNECESSARY));
    }
}
