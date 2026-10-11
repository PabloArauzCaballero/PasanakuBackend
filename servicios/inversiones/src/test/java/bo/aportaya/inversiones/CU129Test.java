package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.aplicacion.CU129SimularCaja.Entrada;
import bo.aportaya.inversiones.aplicacion.CU129SimularCaja.Salida;
import bo.aportaya.inversiones.dominio.SimulacionDeCaja.Alternativa;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M7 · la inversion de dinero de grupos sigue desactivada y una simulacion SEPARADA permite
 * comparar caja sin tocarla. Los numeros estan hechos a mano con los productos sinteticos de prueba
 * (DPF-T: 180 dias, 4 %, base 365, retencion 10 %; DPF-T-ANT: 360 dias, 4,5 %, base 360, penalizacion
 * 50 %; fondo: comision de exito 10 %). Nada de esto es una promesa de rendimiento.
 */
class CU129Test extends BaseDeAnalisis {

    private Salida simular(int horizonte, Set<String> productos, List<BigDecimal> variaciones) {
        return cu129.simular(new Entrada(bob("1000.00"), horizonte, productos, variaciones), ctx);
    }

    private Alternativa de(Salida s, String codigo, String tipo) {
        return s.alternativas().stream()
                .filter(a -> tipo.equals(a.tipo()) && (codigo == null || codigo.equals(a.productoCodigo())))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName(
            "Dado un importe hipotetico y un horizonte · Cuando se simula · Entonces compara billetera, DPF y fondo sin consultar ni mover ningun saldo")
    void comparaSinTocarSaldos() {
        conSaldo("5000.00");

        Salida s = simular(365, Set.of(), List.of());

        assertThat(s.usaSaldoReal()).isFalse();
        var billetera = de(s, null, "BILLETERA");
        assertThat(billetera.netoCuandoDisponible()).isEqualByComparingTo("1000.00");
        assertThat(billetera.diasParaDisponer()).isZero();
        // DPF-T: 1000 x 4 % x 180 / 365 = 19,73 ; retencion sintetica 10 % = 1,97
        var dpf = de(s, "DPF-T", "DPF_AL_VENCIMIENTO");
        assertThat(dpf.interes()).isEqualByComparingTo("19.73");
        assertThat(dpf.retencion()).isEqualByComparingTo("1.97");
        assertThat(dpf.netoCuandoDisponible()).isEqualByComparingTo("1017.76");
        assertThat(dpf.diferenciaContraBilletera()).isEqualByComparingTo("17.76");
        assertThat(dpf.diasParaDisponer()).isEqualTo(180);
        // DPF-T-ANT al vencimiento: 1000 x 4,5 % x 360 / 360 = 45,00 ; retencion 4,50
        assertThat(de(s, "DPF-T-ANT", "DPF_AL_VENCIMIENTO").netoCuandoDisponible())
                .isEqualByComparingTo("1040.50");
        // Fondo, escenarios por omision: -10 %, 0 % y +10 % (la comision solo grava el beneficio)
        var fondos = s.alternativas().stream()
                .filter(a -> productoFondo.equals(a.productoCodigo()))
                .toList();
        assertThat(fondos)
                .extracting(a -> a.netoCuandoDisponible().toPlainString())
                .containsExactly("900.00", "1000.00", "1090.00");
        assertThat(fondos).extracting(Alternativa::tipo).containsOnly("FONDO_ESCENARIO");
        // Ni el libro ni la base se enteraron: la simulacion no tiene nada que escribir.
        assertThat(libro.asientos()).isEmpty();
        assertThat(libro.total(cuenta)).isEqualByComparingTo("5000.00");
        assertThat(contar("select count(*)::int from inversiones.instruccion_libro where usuario_id = ?", usuario))
                .isZero();
        assertThat(contar("select count(*)::int from inversiones.orden_inversion where usuario_id = ?", usuario))
                .isZero();
    }

    @Test
    @DisplayName(
            "Dado un DPF que no admite cancelacion anticipada y un horizonte menor al plazo · Cuando se simula · Entonces el DPF figura como no disponible en el horizonte")
    void dpfSinCancelacionNoEstaDisponible() {
        Salida s = simular(90, Set.of("DPF-T"), List.of());

        var dpf = de(s, "DPF-T", "DPF_NO_DISPONIBLE");
        assertThat(dpf.disponibleEnElHorizonte()).isFalse();
        assertThat(dpf.diasParaDisponer()).isEqualTo(180);
        assertThat(dpf.netoCuandoDisponible()).isEqualByComparingTo("1017.76");
        assertThat(s.alternativas()).hasSize(2); // billetera y ese DPF, nada mas
        assertThat(de(s, null, "BILLETERA").disponibleEnElHorizonte()).isTrue();
    }

    @Test
    @DisplayName(
            "Dado un DPF que si admite la cancelacion y un horizonte menor al plazo · Cuando se simula · Entonces se calcula la cancelacion con la penalizacion del contrato")
    void dpfConCancelacionAnticipada() {
        Salida s = simular(90, Set.of("DPF-T-ANT"), List.of());

        // 1000 x 4,5 % x 90 / 360 = 11,25 ; penalizacion 50 % = 5,625 -> 5,62 (half-even) ; retencion 0,562 -> 0,56
        var dpf = de(s, "DPF-T-ANT", "DPF_ANTICIPADO");
        assertThat(dpf.disponibleEnElHorizonte()).isTrue();
        assertThat(dpf.interes()).isEqualByComparingTo("5.62");
        assertThat(dpf.retencion()).isEqualByComparingTo("0.56");
        assertThat(dpf.netoCuandoDisponible()).isEqualByComparingTo("1005.06");
        assertThat(dpf.diasParaDisponer()).isEqualTo(90);
    }

    @Test
    @DisplayName(
            "Dado un fondo y escenarios de caida y de subida · Cuando se simula · Entonces la caida muestra perdida sin comision y la subida cobra exito solo sobre el beneficio")
    void fondoConCaidaYSubida() {
        Salida s = simular(30, Set.of(productoFondo), List.of(new BigDecimal("-0.20"), new BigDecimal("0.30")));

        var fondos = s.alternativas().stream()
                .filter(a -> a.productoCodigo() != null)
                .toList();
        assertThat(fondos).hasSize(2);
        var caida = fondos.get(0);
        assertThat(caida.netoCuandoDisponible()).isEqualByComparingTo("800.00");
        assertThat(caida.diferenciaContraBilletera()).isEqualByComparingTo("-200.00");
        assertThat(caida.comision()).isEqualByComparingTo("0.00");
        var subida = fondos.get(1);
        assertThat(subida.interes()).isEqualByComparingTo("300.00");
        assertThat(subida.comision()).isEqualByComparingTo("30.00");
        assertThat(subida.netoCuandoDisponible()).isEqualByComparingTo("1270.00");
        assertThat(caida.descripcion()).contains("no una prediccion");
    }

    @Test
    @DisplayName(
            "Dado un saldo afectado a un pozo · Cuando se simula la inversion de ese importe · Entonces la simulacion no lo lee ni reactiva la inversion de grupos, y la orden real sigue rechazada con AP-CU121-05")
    void inversionDeGruposSigueDesactivada() {
        conSaldo("5000.00");
        libro.comprometerEnPozos(cuenta, "4500.00");

        var simulacion = cu129.simular(new Entrada(bob("4500.00"), 365, Set.of("DPF-T"), List.of()), ctx);

        assertThat(simulacion.usaSaldoReal()).isFalse();
        assertThat(de(simulacion, "DPF-T", "DPF_AL_VENCIMIENTO").netoCuandoDisponible())
                .isGreaterThan(bob("4500.00").monto());
        assertThatThrownBy(() -> cu121.ordenar(entrada(PRODUCTO_DPF, "1000.00", java.util.UUID.randomUUID()), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU121-05"));
        assertThat(contar("select count(*)::int from inversiones.orden_inversion where usuario_id = ?", usuario))
                .isZero();
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("cuadre: en cada alternativa el neto es el importe mas el interes menos la retencion y la comision")
    void cadaAlternativaCuadra() {
        Salida s = simular(365, Set.of(), List.of(new BigDecimal("-0.05"), new BigDecimal("0.07")));

        for (Alternativa a : s.alternativas()) {
            BigDecimal base = new BigDecimal("1000.00");
            BigDecimal esperado = a.tipo().equals("FONDO_ESCENARIO")
                    ? base.add(a.interes()).subtract(a.comision()).subtract(perdidaDe(a))
                    : base.add(a.interes()).subtract(a.retencion()).subtract(a.comision());
            assertThat(a.netoCuandoDisponible()).as(a.descripcion()).isEqualByComparingTo(esperado);
            assertThat(a.diferenciaContraBilletera())
                    .isEqualByComparingTo(a.netoCuandoDisponible().subtract(base));
        }
    }

    /** En una caida de fondo no hay interes: lo que se pierde es el valor, no una retencion. */
    private BigDecimal perdidaDe(Alternativa a) {
        return a.interes().signum() == 0 && a.netoCuandoDisponible().compareTo(new BigDecimal("1000.00")) < 0
                ? new BigDecimal("1000.00").subtract(a.netoCuandoDisponible())
                : BigDecimal.ZERO;
    }

    @Test
    @DisplayName(
            "rechaza: un importe cero o en otra moneda, un horizonte fuera de rango, un producto que no existe y escenarios imposibles")
    void entradasInvalidas() {
        assertThatThrownBy(() -> cu129.simular(new Entrada(Dinero.cero(Moneda.BOB), 30, Set.of(), List.of()), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-01"));
        assertThatThrownBy(
                        () -> cu129.simular(new Entrada(Dinero.de("10.00", Moneda.USD), 30, Set.of(), List.of()), ctx))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-01"));
        assertThatThrownBy(() -> simular(0, Set.of(), List.of()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-02"));
        assertThatThrownBy(() -> simular(3651, Set.of(), List.of()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-02"));
        assertThat(simular(3650, Set.of("DPF-T"), List.of()).alternativas())
                .as("el borde de 3650 dias entra")
                .hasSize(2);
        assertThatThrownBy(() -> simular(30, Set.of("NO-EXISTE"), List.of()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-03"));
        assertThatThrownBy(() -> simular(30, Set.of(), List.of(new BigDecimal("-1.00"))))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-04"));
        assertThatThrownBy(() -> simular(30, Set.of(), java.util.Collections.nCopies(10, BigDecimal.ZERO)))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU129-04"));
    }
}
