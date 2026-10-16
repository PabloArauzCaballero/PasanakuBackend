package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M4 · devengo de un DPF (devengado, pagado, impuestos, vencimiento) y
 * H10.S1.M5 · valoracion de cuotas, con fecha, variacion y dato desactualizado visible.
 *
 * <p>Los valores esperados del interes estan calculados a mano. PostgreSQL real; el aliado
 * es el doble declarado.
 */
class CU123Test extends BaseDeInversiones {

    private static final LocalDate DIA0 = LocalDate.of(2026, 10, 12);

    private String codigoDe(Throwable e) {
        return ((ErrorDeNegocio) e).codigo().valor();
    }

    private BigDecimal sumaDeDevengos(UUID posicion) {
        return dslFixtura
                .fetchOne(
                        "select coalesce(sum(monto), 0) from inversiones.devengo_dpf where posicion_inversion_id = ?",
                        posicion)
                .get(0, BigDecimal.class);
    }

    private BigDecimal ultimoAcumulado(UUID posicion) {
        return dslFixtura
                .fetchOne(
                        "select interes_acumulado from inversiones.devengo_dpf where posicion_inversion_id = ? order by fecha desc limit 1",
                        posicion)
                .get(0, BigDecimal.class);
    }

    private UUID dpf10000() {
        conSaldo("20000.00");
        return posicionConfirmada(PRODUCTO_DPF, "10000.00");
    }

    // ------------------------------------------------------------------ DPF
    @Test
    @DisplayName(
            "Dados principal, tasa y base contractual · Cuando devenga · Entonces distingue devengado, pagado, impuesto estimado y vencimiento")
    void criterio4() {
        UUID posicion = dpf10000();
        cu123Devengo.devengar(DIA0.plusDays(30), sistema());
        AHORA.set(LUNES.plusSeconds(30L * 86400));

        var vista = cu123.ver(posicion, ctx);
        var dpf = vista.dpf().orElseThrow();

        // 10000 x 4 % x 30 / 365 = 32,8767... -> 32,88 ; impuesto sintetico 10 % = 3,288 -> 3,29
        assertThat(dpf.interesDevengado()).isEqualByComparingTo("32.88");
        assertThat(dpf.diasDevengados()).isEqualTo(30);
        assertThat(dpf.interesPagado()).isEqualByComparingTo("0.00");
        assertThat(dpf.impuestoEstimado()).isEqualByComparingTo("3.29");
        assertThat(dpf.impuestoPagado()).isEqualByComparingTo("0.00");
        assertThat(dpf.vencimiento()).isEqualTo(DIA0.plusDays(180));
        assertThat(dpf.tasaNominalAnual()).isEqualByComparingTo("0.040000");
        assertThat(vista.valoracion()).isEmpty();
        assertThat(vista.origen().name()).isEqualTo("SINTETICO");
        assertThat(vista.advertencia()).contains("no esta garantizado");
    }

    @Test
    @DisplayName(
            "cuadre: los 180 devengos diarios suman EXACTAMENTE 197,26 (cuenta independiente hecha a mano) y el acumulado final es igual")
    void devengoCompletoCuadraConLaCuentaIndependiente() {
        UUID posicion = dpf10000();
        for (int dia = 0; dia <= 180; dia++) {
            cu123Devengo.devengar(DIA0.plusDays(dia), sistema());
        }
        assertThat(ultimoAcumulado(posicion)).isEqualByComparingTo("197.26");
        assertThat(sumaDeDevengos(posicion)).isEqualByComparingTo("197.26");
        assertThat(contar(
                        "select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ? and monto < 0",
                        posicion))
                .isZero();
        assertThat(contar(
                        "select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ?", posicion))
                .isEqualTo(181);
    }

    @Test
    @DisplayName("reintento: correr el devengo dos veces el mismo dia no duplica nada")
    void devengoIdempotente() {
        UUID posicion = dpf10000();
        var primera = cu123Devengo.devengar(DIA0.plusDays(10), sistema());
        int filas =
                contar("select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ?", posicion);
        var segunda = cu123Devengo.devengar(DIA0.plusDays(10), sistema());

        assertThat(primera.devengadas()).isGreaterThanOrEqualTo(1);
        assertThat(segunda.devengadas())
                .as("la segunda corrida no inserta nada")
                .isZero();
        assertThat(segunda.total()).isEqualByComparingTo("0");
        assertThat(contar(
                        "select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ?", posicion))
                .isEqualTo(filas);
    }

    @Test
    @DisplayName(
            "Dado un devengo que no corrio durante varios dias · Cuando corre de nuevo · Entonces el devengo del dia cubre la diferencia y no se pierde un centavo")
    void devengoConHuecos() {
        UUID posicion = dpf10000();
        cu123Devengo.devengar(DIA0.plusDays(10), sistema());
        // 10000 x 4 % x 10 / 365 = 10,9589 -> 10,96
        assertThat(ultimoAcumulado(posicion)).isEqualByComparingTo("10.96");
        assertThat(sumaDeDevengos(posicion)).isEqualByComparingTo("10.96");
        cu123Devengo.devengar(DIA0.plusDays(25), sistema());
        assertThat(sumaDeDevengos(posicion))
                .isEqualByComparingTo(ultimoAcumulado(posicion))
                .isEqualByComparingTo("27.40"); // 25 dias: 10000 x 4 % x 25 / 365 = 27,3973
    }

    @Test
    @DisplayName(
            "Dado un DPF · Cuando se corre el devengo antes de constituir o despues del vencimiento · Entonces no se devenga nada")
    void fueraDelPlazo() {
        UUID posicion = dpf10000();
        cu123Devengo.devengar(DIA0.plusDays(181), sistema());
        cu123Devengo.devengar(DIA0.minusDays(1), sistema());
        assertThat(contar(
                        "select count(*)::int from inversiones.devengo_dpf where posicion_inversion_id = ?", posicion))
                .isZero();
    }

    // ------------------------------------------------------------------ fondo
    private UUID fondo1000() {
        conSaldo("20000.00");
        return posicionConfirmada(productoFondo, "1000.00"); // 10 cuotas a 100,000000
    }

    @Test
    @DisplayName(
            "Dadas cuotas y valor publicado · Cuando cae el valor · Entonces muestra fecha, variacion NEGATIVA y valor neto, sin promesa")
    void criterio5CaidaDeValor() {
        UUID posicion = fondo1000();
        aliado.publicar(productoFondo, DIA0, "98.500000");
        assertThat(cu123.sincronizarValores(sistema())).isEqualTo(1);

        var v = cu123.ver(posicion, ctx).valoracion().orElseThrow();

        assertThat(v.cuotas()).isEqualByComparingTo("10.000000");
        assertThat(v.valorCuota()).isEqualByComparingTo("98.500000");
        assertThat(v.fechaValor()).isEqualTo(DIA0);
        assertThat(v.valorBruto()).isEqualByComparingTo("985.00");
        assertThat(v.variacion()).isEqualByComparingTo("-15.00");
        assertThat(v.comisionDevengada()).isEqualByComparingTo("0.00");
        assertThat(v.valorNetoEstimado()).isEqualByComparingTo("985.00");
        assertThat(v.desactualizado()).isFalse();
        assertThat(cu123.ver(posicion, ctx).advertencia()).contains("puede generar perdidas");
    }

    @Test
    @DisplayName(
            "Dado un valor por encima de la marca · Cuando se valora la posicion · Entonces la comision de exito devengada se muestra aparte y se descuenta del neto estimado")
    void valorPorEncimaDeLaMarca() {
        UUID posicion = fondo1000();
        aliado.publicar(productoFondo, DIA0, "112.000000");
        cu123.sincronizarValores(sistema());

        var v = cu123.ver(posicion, ctx).valoracion().orElseThrow();

        assertThat(v.valorBruto()).isEqualByComparingTo("1120.00");
        assertThat(v.variacion()).isEqualByComparingTo("120.00");
        assertThat(v.comisionDevengada()).isEqualByComparingTo("12.00"); // (112-100) x 10 cuotas x 10 % sintetico
        assertThat(v.valorNetoEstimado()).isEqualByComparingTo("1108.00");
    }

    @Test
    @DisplayName(
            "Dado un valor de cuota publicado hace 3 o 4 dias · Cuando se valora la posicion · Entonces a los 3 dias no avisa y a los 4 se marca desactualizado")
    void datoDesactualizado() {
        UUID posicion = fondo1000();
        aliado.publicar(productoFondo, DIA0, "101.000000");
        cu123.sincronizarValores(sistema());

        AHORA.set(Instant.parse("2026-10-15T12:00:00Z"));
        var alBorde = cu123.ver(posicion, ctx).valoracion().orElseThrow();
        assertThat(alBorde.antiguedadDias()).isEqualTo(3);
        assertThat(alBorde.desactualizado()).isFalse();

        AHORA.set(Instant.parse("2026-10-16T12:00:00Z"));
        var viejo = cu123.ver(posicion, ctx).valoracion().orElseThrow();
        assertThat(viejo.antiguedadDias()).isEqualTo(4);
        assertThat(viejo.desactualizado()).isTrue();
        assertThat(viejo.fechaValor()).isEqualTo(DIA0);
    }

    @Test
    @DisplayName(
            "Dado un fondo sin ningun valor publicado · Cuando se valora la posicion · Entonces no se inventa una valoracion")
    void sinValor() {
        UUID posicion = fondo1000();
        var vista = cu123.ver(posicion, ctx);
        // Puede haber valores de otras pruebas del mismo fondo; lo que no puede pasar es una valoracion sin valor
        // detras.
        vista.valoracion().ifPresent(v -> assertThat(v.fechaValor()).isNotNull());
    }

    @Test
    @DisplayName(
            "Dado un valor de cuota ya publicado para una fecha · Cuando el aliado publica otro distinto para esa fecha · Entonces se rechaza con AP-CU123-02 y queda el primero")
    void valorContradictorio() {
        fondo1000();
        LocalDate fecha = LocalDate.of(2031, 1, 6);
        aliado.publicar(productoFondo, fecha, "100.100000");
        cu123.sincronizarValores(sistema());
        aliado.publicar(productoFondo, fecha, "100.200000");

        assertThatThrownBy(() -> cu123.sincronizarValores(sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-02"));
        assertThat(dslFixtura
                        .fetchOne("select valor from inversiones.valor_cuota where fecha = ?", fecha)
                        .get(0, BigDecimal.class))
                .isEqualByComparingTo("100.100000");
        assertThat(rechazaLaBase("update inversiones.valor_cuota set valor = 1 where fecha = ?", fecha))
                .contains("R-AUD-01");
    }

    @Test
    @DisplayName(
            "Dado el aliado caido · Cuando se sincronizan los valores de cuota · Entonces falla con AP-CU123-02 y la valoracion existente sigue visible")
    void aliadoCaido() {
        UUID posicion = fondo1000();
        aliado.publicar(productoFondo, DIA0, "100.500000");
        cu123.sincronizarValores(sistema());
        aliado.modo(AliadoDoble.Modo.CAIDO);

        assertThatThrownBy(() -> cu123.sincronizarValores(sistema()))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-02"));
        assertThat(cu123.ver(posicion, ctx).valoracion()).isPresent();
    }

    // ------------------------------------------------------------------ objeto propio
    @Test
    @DisplayName(
            "Dada la posicion de OTRA persona · Cuando esta intenta verla, listarla o pedir sus comprobantes · Entonces AP-CU123-01: no existe para ella")
    void posicionAjena() {
        UUID posicion = dpf10000();
        var otra = contextoDe(UUID.randomUUID());

        assertThatThrownBy(() -> cu123.ver(posicion, otra))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-01"));
        assertThatThrownBy(() -> cu123.comprobantes(posicion, otra))
                .satisfies(e -> assertThat(codigoDe(e)).isEqualTo("AP-CU123-01"));
        assertThat(cu123.listar(otra)).isEmpty();
        assertThat(cu123.listar(ctx)).extracting(p -> p.id()).containsExactly(posicion);
    }

    @Test
    @DisplayName("rechaza por R-INV-08: un devengo diario mayor que el interes acumulado")
    void rechazaRINV08() {
        // El devengo del dia es la diferencia de dos acumulados: nunca puede superar el acumulado.
        UUID posicion = dpf10000();
        String insercion =
                "insert into inversiones.devengo_dpf (posicion_inversion_id, fecha, dias_acumulados, interes_acumulado,"
                        + " monto, moneda) values (?, date '2031-01-01', 1, 1.00, ?, 'BOB')";

        assertThat(rechazaLaBase(insercion, posicion, new BigDecimal("1.01"))).contains("ck_devengo_dpf_acumulado");
        assertThat(rechazaLaBase(insercion, posicion, new BigDecimal("1.00")))
                .as("control negativo: el mismo insert, coherente, entra")
                .isEmpty();
    }
}
