package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.aplicacion.VistaRescate;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H11.S1.M4 · comision de exito con marca maxima previa NETA de comision: el ejemplo sintetico del
 * plan de extremo a extremo (no es una tarifa comercial), perdida, recuperacion, lotes y el
 * redondeo con el residuo asignado. PostgreSQL real; aliado y libro dobles.
 */
class CU125ComisionTest extends BaseDeLiquidacion {

    // ------------------------------------------------------------------ comision de exito (H11.M4)
    @Test
    @DisplayName(
            "Dado el ejemplo sintetico del plan: 100 cuotas, marca 110, valor 112 y 10 % · Cuando se rescata · Entonces la comision es Bs 20,00 y la cuota neta 111,80 (no es una tarifa comercial)")
    void ejemploDelPlanDeExtremoAExtremo() {
        conSaldo("20000.00");
        UUID posicion = fondo("11000.00", "110.000000"); // 100 cuotas, marca 110
        aliado.valorCuotaAplicado("112.000000").rescateFondoInmediato(true);

        VistaRescate r = cu124.solicitar(pedir(posicion, null), ctx);

        assertThat(r.estado()).isEqualTo("LIQUIDADO");
        var c = liquidacion(posicion).d();
        assertThat(c.principal()).isEqualByComparingTo("11000.00");
        assertThat(c.interes()).isEqualByComparingTo("200.00");
        assertThat(c.comision()).isEqualByComparingTo("20.00");
        assertThat(c.neto()).isEqualByComparingTo("11180.00");
        var fila = dslFixtura.fetchOne(
                "select marca_maxima_previa, base_elegible, comision, cuota_neta, marca_maxima_nueva, origen_datos"
                        + " from inversiones.comision_exito where posicion_inversion_id = ?",
                posicion);
        assertThat(fila.get(0, BigDecimal.class)).isEqualByComparingTo("110.000000");
        assertThat(fila.get(1, BigDecimal.class)).isEqualByComparingTo("200.00");
        assertThat(fila.get(2, BigDecimal.class)).isEqualByComparingTo("20.00");
        assertThat(fila.get(3, BigDecimal.class)).isEqualByComparingTo("111.800000");
        assertThat(fila.get(4, BigDecimal.class)).isEqualByComparingTo("111.800000");
        assertThat(fila.get(5, String.class)).isEqualTo("SINTETICO");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("20180.00"); // 20000 - 11000 + 11180
    }

    @Test
    @DisplayName(
            "Dado un rescate con perdida · Cuando se liquida · Entonces no hay comision porque se cobra solo beneficio elegible, y la diferencia es perdida realizada")
    void perdidaNoCobraComision() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        aliado.valorCuotaAplicado("98.500000").rescateFondoInmediato(true);

        cu124.solicitar(pedir(posicion, null), ctx);

        var c = liquidacion(posicion).d();
        assertThat(c.perdidaRealizada()).isEqualByComparingTo("15.00");
        assertThat(c.interes()).isEqualByComparingTo("0.00");
        assertThat(c.comision()).isEqualByComparingTo("0.00");
        assertThat(c.neto()).isEqualByComparingTo("985.00");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("19985.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.comision_exito where posicion_inversion_id = ? and comision > 0",
                        posicion))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un valor que cae y luego se recupera · Cuando se rescata en dos tandas · Entonces lo rescatado con perdida no paga exito y lo rescatado despues solo paga sobre lo que supera la marca del lote")
    void recuperacionPorTramos() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000"); // 10 cuotas, marca 100
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("90.000000");
        cu124.solicitar(pedir(posicion, "5.000000"), ctx); // pierde: 450 contra 500
        assertThat(liquidacion(posicion).d().perdidaRealizada()).isEqualByComparingTo("50.00");
        assertThat(liquidacion(posicion).d().comision()).isEqualByComparingTo("0.00");

        AHORA.set(AHORA.get().plusSeconds(60));
        aliado.valorCuotaAplicado("105.000000");
        cu124.solicitar(
                pedir(posicion, "5.000000"), ctx); // recupera: 525 contra 500, sobre la marca 100 -> (105-100) x 5 = 25
        var segunda = liquidacion(posicion).d();
        assertThat(segunda.interes()).isEqualByComparingTo("25.00");
        assertThat(segunda.comision()).isEqualByComparingTo("2.50");
        assertThat(segunda.neto()).isEqualByComparingTo("522.50");
        assertThat(costoBase(posicion))
                .as("la posicion quedo en cero desde el mayor")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName(
            "Dadas dos suscripciones con valores de entrada distintos · Cuando se rescata · Entonces cada lote paga exito solo sobre lo que supera SU valor de entrada")
    void lotes() {
        conSaldo("20000.00");
        UUID viejo = fondo("1000.00", "100.000000"); // 10 cuotas, marca 100
        UUID nuevo = fondo("1100.00", "110.000000"); // 10 cuotas, marca 110
        aliado.valorCuotaAplicado("112.000000").rescateFondoInmediato(true);

        cu124.solicitar(pedir(viejo, null), ctx);
        cu124.solicitar(pedir(nuevo, null), ctx);

        assertThat(liquidacion(viejo).d().comision()).isEqualByComparingTo("12.00"); // (112-100) x 10 x 10 %
        assertThat(liquidacion(nuevo).d().comision()).isEqualByComparingTo("2.00"); //  (112-110) x 10 x 10 %
    }

    @Test
    @DisplayName(
            "cuadre: redondeo declarado, tres rescates de una posicion de 100,00 en 3 cuotas liberan 33,33 + 33,34 + 33,33, el residuo se asigna y no se pierde")
    void residuoDeRedondeoAsignado() {
        conSaldo("20000.00");
        UUID posicion = fondo("100.00", "33.333333"); // 100,00 / 33,333333 = 3,0000003 -> 3,000000 cuotas
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("33.333333");

        for (int i = 0; i < 3; i++) {
            cu124.solicitar(pedir(posicion, "1.000000"), ctx);
            AHORA.set(AHORA.get()
                    .plusSeconds(60)); // un instante distinto por liquidacion: el orden del historial es el del tiempo
        }

        List<BigDecimal> liberados = cu123.comprobantes(posicion, ctx).stream()
                .filter(c -> "LIQUIDACION".equals(c.tipo()))
                .map(c -> c.d().costoBase())
                .toList();
        assertThat(liberados).hasSize(3);
        assertThat(liberados.stream().reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
        assertThat(liberados.get(1)).isEqualByComparingTo("33.34");
        assertThat(costoBase(posicion)).isEqualByComparingTo("0.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.posicion_inversion where id = ? and estado = 'CERRADA'",
                        posicion))
                .isEqualTo(1);
    }
}
