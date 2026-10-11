package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H11.S1.M4 · la comision por resultado, estimada ANTES de rescatar y cobrada despues, con la marca
 * maxima previa neta de comision. Casos: perdida, recuperacion, suscripcion por lotes y rescate
 * parcial (la marca que sube y no vuelve a cobrar el mismo tramo). La tasa es SINTETICA: el ejemplo
 * del plan (100 cuotas, marca 110, valor 112, 10 %) es un calculo, no una tarifa (DR-INV-04).
 */
class CU127Test extends BaseDeAnalisis {

    private void publicar(String valor) {
        aliado.publicar(productoFondo, LocalDate.of(2026, 10, 12), valor);
        cu123.sincronizarValores(sistema());
    }

    @Test
    @DisplayName("Dado un DPF que no tiene comision por resultado · Cuando se estima · Entonces AP-CU127-01")
    void productoSinComision() {
        conSaldo("20000.00");
        UUID dpf = posicionConfirmada(PRODUCTO_DPF, "1000.00");

        assertThatThrownBy(() -> cu127.estimar(dpf, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU127-01"));
    }

    @Test
    @DisplayName(
            "Dado el ejemplo sintetico del plan: 100 cuotas, marca 110, valor 112 y 10 % · Cuando se estima · Entonces la base elegible es Bs 200,00, la comision Bs 20,00 y la cuota neta 111,80")
    void ejemploDelPlan() {
        conSaldo("20000.00");
        UUID posicion = fondo("11000.00", "110.000000");
        publicar("112.000000");

        var v = cu127.estimar(posicion, ctx);

        assertThat(v.cuotas()).isEqualByComparingTo("100.000000");
        assertThat(v.marcaMaximaPrevia()).isEqualByComparingTo("110.000000");
        assertThat(v.baseElegible()).isEqualByComparingTo("200.00");
        assertThat(v.comisionEstimada()).isEqualByComparingTo("20.00");
        assertThat(v.cuotaNetaEstimada()).isEqualByComparingTo("111.800000");
        assertThat(v.marcaMaximaNueva()).isEqualByComparingTo("111.800000");
        assertThat(v.comisionCobradaAntes()).isEqualByComparingTo("0.00");
        assertThat(v.origen().name()).isEqualTo("SINTETICO");
        assertThat(v.advertencia()).contains("no esta garantizado");
    }

    @Test
    @DisplayName(
            "Dado un valor de cuota por debajo de la marca previa · Cuando se estima · Entonces no hay beneficio elegible ni comision")
    void valorPorDebajoDeLaMarca() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        publicar("93.000000");

        var v = cu127.estimar(posicion, ctx);

        assertThat(v.baseElegible()).isEqualByComparingTo("0.00");
        assertThat(v.comisionEstimada()).isEqualByComparingTo("0.00");
        assertThat(v.marcaMaximaNueva()).isEqualByComparingTo("100.000000");
    }

    @Test
    @DisplayName(
            "Dado un fondo sin ningun valor de cuota publicado · Cuando se estima · Entonces AP-CU127-02 y no se inventa una estimacion")
    void sinValorPublicado() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");

        assertThatThrownBy(() -> cu127.estimar(posicion, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU127-02"));
    }

    @Test
    @DisplayName(
            "Dada una posicion de fondo ya rescatada por completo · Cuando se estima · Entonces AP-CU127-03: no quedan cuotas")
    void sinCuotasAbiertas() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");
        publicar("100.000000");
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("100.000000");
        cu124.solicitar(pedir(posicion, null), ctx);

        assertThatThrownBy(() -> cu127.estimar(posicion, ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU127-03"));
    }

    @Test
    @DisplayName(
            "Dado un rescate parcial con beneficio · Cuando se rescata el resto al mismo valor · Entonces la marca ya subio y no se cobra dos veces el mismo tramo")
    void rescateParcialNoCobraDosVecesElMismoTramo() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000"); // 10 cuotas, marca 100
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("120.000000");
        publicar("120.000000");

        cu124.solicitar(pedir(posicion, "5.000000"), ctx); // (120 - 100) x 5 = 100 de base, 10 % = 10,00
        assertThat(liquidacion(posicion).d().comision()).isEqualByComparingTo("10.00");
        // La marca de la posicion quedo en la cuota neta (120 x 5 - 10) / 5 = 118,00, no en la de entrada.
        assertThat(dslFixtura
                        .fetchOne("select marca_maxima from inversiones.posicion_inversion where id = ?", posicion)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("118.000000");
        var estimacion = cu127.estimar(posicion, ctx);
        assertThat(estimacion.marcaMaximaPrevia()).isEqualByComparingTo("118.000000");
        assertThat(estimacion.baseElegible()).isEqualByComparingTo("10.00"); // (120 - 118) x 5
        assertThat(estimacion.comisionCobradaAntes()).isEqualByComparingTo("10.00");

        AHORA.set(AHORA.get().plusSeconds(60));
        cu124.solicitar(pedir(posicion, "5.000000"), ctx);

        var segunda = liquidacion(posicion).d();
        assertThat(segunda.comision())
                .as("solo el tramo entre 118 y 120, no el de 100 a 120 otra vez")
                .isEqualByComparingTo("1.00");
    }

    @Test
    @DisplayName(
            "Dadas una perdida previa en un lote y una suscripcion nueva mas alta · Cuando se estima cada una · Entonces cada lote paga solo sobre lo que supera su propia marca")
    void perdidaPreviaYSuscripcionNueva() {
        conSaldo("40000.00");
        UUID viejo = fondo("1000.00", "100.000000"); // 10 cuotas, marca 100
        aliado.rescateFondoInmediato(true).valorCuotaAplicado("90.000000");
        cu124.solicitar(pedir(viejo, "5.000000"), ctx); // perdida realizada de 50,00 en el lote viejo
        UUID nuevo = fondo("1200.00", "120.000000"); // 10 cuotas, marca 120: la perdida de otro lote no lo toca
        publicar("125.000000");

        var delViejo = cu127.estimar(viejo, ctx);
        var delNuevo = cu127.estimar(nuevo, ctx);

        assertThat(delViejo.baseElegible()).isEqualByComparingTo("125.00"); // (125 - 100) x 5
        assertThat(delViejo.comisionEstimada()).isEqualByComparingTo("12.50");
        assertThat(delNuevo.baseElegible()).isEqualByComparingTo("50.00"); // (125 - 120) x 10
        assertThat(delNuevo.comisionEstimada()).isEqualByComparingTo("5.00");
        assertThat(delViejo.comisionCobradaAntes()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("rechaza: estimar la comision de la posicion de OTRA persona es AP-CU123-01, como si no existiera")
    void posicionAjena() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");

        assertThatThrownBy(() -> cu127.estimar(posicion, contextoDe(UUID.randomUUID())))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-01"));
    }

    @Test
    @DisplayName("rechaza por R-INV-07: bajar la marca maxima de una posicion, y cobrar una comision sin base elegible")
    void baseProtegeLaMarca() {
        conSaldo("20000.00");
        UUID posicion = fondo("1000.00", "100.000000");

        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set marca_maxima = marca_maxima - 1 where id = ?",
                        posicion))
                .contains("R-INV-07");
        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set marca_maxima = marca_maxima where id = ?", posicion))
                .as("control negativo: dejarla igual no es bajarla")
                .isEmpty();
        assertThat(rechazaLaBase(
                        "update inversiones.posicion_inversion set marca_maxima = marca_maxima + 1 where id = ?",
                        posicion))
                .as("subirla si se puede: es lo que hace un rescate con beneficio")
                .isEmpty();
    }
}
