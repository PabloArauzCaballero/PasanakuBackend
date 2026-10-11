package bo.aportaya.inversiones.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.CalendarioHabil;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comision de exito, valoracion, liquidacion de cuotas y corte. Los numeros del ejemplo de
 * la comision son los del PLAN §H11 (ejemplo sintetico, NO una tarifa comercial).
 */
class CalculosDeFondoTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    // ------------------------------------------------------------------ comision de exito
    @Test
    @DisplayName(
            "PLAN H11: 100 cuotas, marca 110, valor 112 y 10 % dan base 200,00, comision 20,00 y cuota neta 111,80")
    void ejemploDelPlan() {
        var c = ComisionDeExito.calcular(d("100.000000"), d("112.000000"), d("110.000000"), d("0.100000"));
        assertThat(c.baseElegible()).isEqualByComparingTo("200.00");
        assertThat(c.comision()).isEqualByComparingTo("20.00");
        assertThat(c.cuotaNeta()).isEqualByComparingTo("111.800000");
        assertThat(c.marcaNueva()).isEqualByComparingTo("111.800000");
    }

    @Test
    @DisplayName("si el valor cae y se recupera sin pasar la marca previa, NO hay comision")
    void perdidaYRecuperacion() {
        // marca 110 -> cae a 100 -> recupera a 108: todavia debajo de la marca.
        var c = ComisionDeExito.calcular(d("100.000000"), d("108.000000"), d("110.000000"), d("0.100000"));
        assertThat(c.comision()).isEqualByComparingTo("0.00");
        assertThat(c.baseElegible()).isEqualByComparingTo("0.00");
        assertThat(c.marcaNueva()).isEqualByComparingTo("110.000000");
    }

    @Test
    @DisplayName("valor exactamente igual a la marca: nada que cobrar")
    void igualALaMarca() {
        assertThat(ComisionDeExito.calcular(d("5.000000"), d("110.000000"), d("110.000000"), d("0.100000"))
                        .comision())
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("lotes: cada lote parte de SU valor de entrada, asi que lo ganado antes de entrar no se cobra")
    void suscripcionPorLotes() {
        // Valor de hoy 112. Lote viejo entro a 100; lote nuevo entro a 112.
        var viejo = ComisionDeExito.calcular(d("10.000000"), d("112.000000"), d("100.000000"), d("0.100000"));
        var nuevo = ComisionDeExito.calcular(d("10.000000"), d("112.000000"), d("112.000000"), d("0.100000"));
        assertThat(viejo.comision()).isEqualByComparingTo("12.00"); // (112-100) x 10 x 10 %
        assertThat(nuevo.comision()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("la comision nunca supera la ganancia sobre la marca y la marca nueva nunca baja")
    void invariantes() {
        for (String tasa : new String[] {"0.000000", "0.050000", "0.333333", "1.000000"}) {
            for (String valor : new String[] {"90.000000", "110.000000", "110.000001", "150.000000"}) {
                var c = ComisionDeExito.calcular(d("7.123457"), d(valor), d("110.000000"), d(tasa));
                assertThat(c.comision()).isLessThanOrEqualTo(c.baseElegible());
                assertThat(c.marcaNueva()).isGreaterThanOrEqualTo(d("110.000000"));
            }
        }
    }

    @Test
    @DisplayName("entradas invalidas se rechazan")
    void entradasInvalidas() {
        assertThatThrownBy(() -> ComisionDeExito.calcular(d("0"), d("1"), d("1"), d("0.1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ComisionDeExito.calcular(d("1"), d("1"), d("0"), d("0.1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ComisionDeExito.calcular(d("1"), d("1"), d("1"), d("-0.1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ valoracion
    @Test
    @DisplayName("la valoracion muestra variacion NEGATIVA sin maquillarla")
    void caidaDeValor() {
        var v = ValoracionDeCuotas.de(
                d("10.000000"),
                d("98.500000"),
                d("1000.00"),
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2026, 10, 12),
                3);
        assertThat(v.valorBruto()).isEqualByComparingTo("985.00");
        assertThat(v.variacion()).isEqualByComparingTo("-15.00");
        assertThat(v.desactualizado()).isFalse();
    }

    @Test
    @DisplayName("un valor de hace mas dias que la tolerancia se marca desactualizado; en el borde no")
    void datoDesactualizado() {
        LocalDate hoy = LocalDate.of(2026, 10, 15);
        assertThat(ValoracionDeCuotas.de(
                                d("1.000000"), d("100.000000"), d("100.00"), LocalDate.of(2026, 10, 12), hoy, 3)
                        .desactualizado())
                .isFalse();
        var viejo =
                ValoracionDeCuotas.de(d("1.000000"), d("100.000000"), d("100.00"), LocalDate.of(2026, 10, 11), hoy, 3);
        assertThat(viejo.antiguedadDias()).isEqualTo(4);
        assertThat(viejo.desactualizado()).isTrue();
    }

    @Test
    @DisplayName("rescatar mas cuotas de las que quedan, o cero, es un error")
    void costoBaseConCuotasInvalidas() {
        assertThatThrownBy(() -> ValoracionDeCuotas.costoBaseLiberado(d("100.00"), d("4"), d("3")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValoracionDeCuotas.costoBaseLiberado(d("100.00"), d("0"), d("3")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ descomposicion
    @Test
    @DisplayName("fondo con ganancia: principal = costo liberado, interes = ganancia, la comision sale del neto")
    void liquidacionConGanancia() {
        var d = Descomposicion.fondo(d("11000.00"), d("11200.00"), d("20.00"));
        assertThat(d.principal()).isEqualByComparingTo("11000.00");
        assertThat(d.interes()).isEqualByComparingTo("200.00");
        assertThat(d.comision()).isEqualByComparingTo("20.00");
        assertThat(d.neto()).isEqualByComparingTo("11180.00");
    }

    @Test
    @DisplayName("fondo con perdida: la diferencia es perdida realizada, no interes negativo, y no hay comision")
    void liquidacionConPerdida() {
        var d = Descomposicion.fondo(d("1000.00"), d("985.00"), d("0.00"));
        assertThat(d.perdidaRealizada()).isEqualByComparingTo("15.00");
        assertThat(d.interes()).isEqualByComparingTo("0.00");
        assertThat(d.neto()).isEqualByComparingTo("985.00");
        assertThat(d.costoBase()).isEqualByComparingTo("1000.00");
        assertThatThrownBy(() -> Descomposicion.fondo(d("1000.00"), d("985.00"), d("1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("una descomposicion que no cuadra no se puede construir")
    void identidades() {
        assertThatThrownBy(() -> new Descomposicion(d("10"), d("1"), d("0"), d("0"), d("0"), d("12"), d("10")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Descomposicion(d("10"), d("1"), d("0"), d("0"), d("0"), d("11"), d("9")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Descomposicion(d("10"), d("1"), d("0"), d("0"), d("1"), d("11"), d("11")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Descomposicion(d("-1"), d("0"), d("0"), d("0"), d("0"), d("-1"), d("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ------------------------------------------------------------------ corte
    private static final CalendarioHabil SEMANA =
            f -> f.getDayOfWeek() == DayOfWeek.SATURDAY || f.getDayOfWeek() == DayOfWeek.SUNDAY;

    @Test
    @DisplayName("antes del corte toma el valor del dia; despues del corte, el del proximo dia habil")
    void corteDelDia() {
        LocalTime corte = LocalTime.of(15, 0);
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-12T12:00:00Z"), corte, SEMANA))
                .isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-12T20:00:00Z"), corte, SEMANA))
                .isEqualTo(LocalDate.of(2026, 10, 13));
        // Justo a las 15:00 en La Paz ya paso el corte (el instante del corte no admite ordenes).
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-12T19:00:00Z"), corte, SEMANA))
                .isEqualTo(LocalDate.of(2026, 10, 13));
    }

    @Test
    @DisplayName("viernes tarde, sabado y feriado saltan al proximo dia habil")
    void finDeSemanaYFeriado() {
        LocalTime corte = LocalTime.of(15, 0);
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-16T20:00:00Z"), corte, SEMANA))
                .isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-17T14:00:00Z"), corte, SEMANA))
                .isEqualTo(LocalDate.of(2026, 10, 19));
        Set<LocalDate> feriados = Set.of(LocalDate.of(2026, 10, 19));
        CalendarioHabil conFeriado = f -> SEMANA.esNoHabil(f) || feriados.contains(f);
        assertThat(CorteDeOrden.fechaValor(Instant.parse("2026-10-16T20:00:00Z"), corte, conFeriado))
                .isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(CorteDeOrden.habilPosterior(LocalDate.of(2026, 10, 16), 2, conFeriado))
                .isEqualTo(LocalDate.of(2026, 10, 21));
    }

    // ------------------------------------------------------------------ texto de condiciones
    private Condiciones condiciones(boolean sintetico) {
        return new Condiciones(
                UUID.randomUUID(),
                1,
                TipoProducto.DPF,
                Optional.of(180),
                Optional.of(365),
                Optional.of(d("0.040000")),
                false,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                d("500.00"),
                Optional.of(d("0.100000")),
                Optional.empty(),
                "costos de prueba",
                "",
                "",
                "fuente de prueba",
                Instant.parse("2026-10-12T12:00:00Z"),
                sintetico ? OrigenDatos.SINTETICO : OrigenDatos.VERIFICADO,
                !sintetico);
    }

    @Test
    @DisplayName("el texto aceptado es deterministico, avisa que los datos son sinteticos y no promete rentabilidad")
    void textoDeCondiciones() {
        String t1 = TextoDeCondiciones.redactar("DPF de prueba", "Aliado", condiciones(true), "a nombre del titular");
        String t2 = TextoDeCondiciones.redactar("DPF de prueba", "Aliado", condiciones(true), "a nombre del titular");
        assertThat(t1).isEqualTo(t2);
        assertThat(TextoDeCondiciones.hash(t1))
                .isEqualTo(TextoDeCondiciones.hash(t2))
                .hasSize(64);
        assertThat(t1).startsWith("DATOS SINTETICOS DE DEMOSTRACION");
        assertThat(t1).contains("no esta garantizado").doesNotContainIgnoringCase("rentabilidad segura");
        assertThat(TextoDeCondiciones.redactar("DPF de prueba", "Aliado", condiciones(false), "x"))
                .doesNotContain("SINTETICOS");
        assertThat(TextoDeCondiciones.hash(t1)).isNotEqualTo(TextoDeCondiciones.hash(t1 + " "));
    }

    @Test
    @DisplayName("un dato sintetico no puede ser apto para produccion, ni siquiera construyendolo")
    void sinteticoNoEsAptoParaProduccion() {
        assertThatThrownBy(() -> new Condiciones(
                        UUID.randomUUID(),
                        1,
                        TipoProducto.DPF,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        false,
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        d("1.00"),
                        Optional.empty(),
                        Optional.empty(),
                        "",
                        "",
                        "",
                        "",
                        Instant.now(),
                        OrigenDatos.SINTETICO,
                        true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
