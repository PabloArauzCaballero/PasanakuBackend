package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-23 · la cobertura del faltante del corte.
 *
 * <p>La cobertura manual del fondo mutual (monto, mora y aprobador del pedido) se retiró (F1.L2.M02): su ruta responde
 * 410 y lo prueba {@code web/CU23CoberturaManualRetiradaWebTest}. El camino vigente es el respaldo, transitorio hasta
 * la cobertura con el fondo del grupo (ADR-055 E7, F3.L2.M05). Los bordes del respaldo viven en {@code CU131*Test}.
 */
class CU23Test extends BaseDeRespaldo {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    @Test
    @DisplayName(
            "Dado un pozo de Bs 6.000 con Bs 5.000 confirmados y una reserva de Bs 8.000 · Cuando se cubre el faltante del corte · Entonces se aplica una sola cobertura de Bs 1.000 con una línea por obligación faltante")
    void criterio1() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");

        var salida = cubrir(c, pedidoDelEjemplo(c, "cu23-1-" + c.turno()));

        assertThat(salida.resultado()).isEqualTo(Resultado.APLICADA);
        assertThat(salida.cubierto()).isEqualTo(bob("1000.00"));
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.cobertura_respaldo_linea WHERE cobertura_respaldo_id = ?",
                        salida.coberturaId()))
                .isEqualTo(2);
    }

    @Test
    @DisplayName(
            "Dada una reserva de Bs 800 y un faltante de Bs 1.000 · Cuando se cubre el faltante del corte · Entonces no se cubre nada y la reserva queda intacta")
    void criterio2() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "800.00");

        var salida = cubrir(c, pedidoDelEjemplo(c, "cu23-2-" + c.turno()));

        // Todo o nada: cubrir una parte dejaria al grupo creyendo que el pozo esta completo.
        assertThat(salida.resultado()).isEqualTo(Resultado.INSUFICIENTE);
        assertThat(salida.coberturaId()).isNull();
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("0");
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isZero();
    }

    @Test
    @DisplayName("reintento: la misma clave de idempotencia dos veces devuelve la misma respuesta y un solo efecto")
    void reintento() {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        reservar(c, "8000.00");
        var pedido = pedidoDelEjemplo(c, "cu23-r-" + c.turno());

        var a = cubrir(c, pedido);
        var b = cubrir(c, pedido);

        assertThat(b.coberturaId()).isEqualTo(a.coberturaId());
        assertThat(contar("SELECT count(*)::int FROM garantia.cobertura_respaldo WHERE turno_id = ?", c.turno()))
                .isEqualTo(1);
    }
}
