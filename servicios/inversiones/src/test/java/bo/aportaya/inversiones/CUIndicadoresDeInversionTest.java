package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.aplicacion.IndicadoresDeInversion;
import bo.aportaya.inversiones.infraestructura.IndicadoresDeInversionRepositorio;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tableros · H4.S1.M6 — saldo invertido al cierre, reconstruible por fecha. Se mide el DELTA de cada prueba. */
class CUIndicadoresDeInversionTest extends BaseDeInversiones {

    private final IndicadoresDeInversionRepositorio repositorio = new IndicadoresDeInversionRepositorio();

    private IndicadoresDeInversionRepositorio.Suma invertidoAl(LocalDate dia) {
        return tx.en(() -> datos.conContexto(sistema(), d -> repositorio.invertido(d, dia, "BOB")));
    }

    private LocalDate constitucion(UUID posicion) {
        return dsl.fetchOne("select fecha_constitucion from inversiones.posicion_inversion where id = ?", posicion)
                .get(0, LocalDate.class);
    }

    @Test
    @DisplayName("cuadre: una posición abierta de Bs 1000 suma 1000 al saldo invertido desde su constitución")
    void abiertaSuma() {
        conSaldo("5000.00");
        LocalDate hoyLibro = LocalDate.now().plusYears(50);
        var antes = invertidoAl(hoyLibro);

        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "1000.00");

        assertThat(invertidoAl(hoyLibro).total().subtract(antes.total())).isEqualByComparingTo("1000.00");
        assertThat(invertidoAl(constitucion(posicion).minusDays(1)).total())
                .isLessThanOrEqualTo(invertidoAl(constitucion(posicion)).total());
    }

    @Test
    @DisplayName("límite: una posición cerrada deja de contar desde el día siguiente a su cierre")
    void cerradaNoCuenta() {
        conSaldo("5000.00");
        UUID posicion = posicionConfirmada(PRODUCTO_DPF, "700.00");
        LocalDate dia = constitucion(posicion);
        var abierta = invertidoAl(dia.plusDays(10));

        dslFixtura.execute(
                """
                UPDATE inversiones.posicion_inversion
                   SET estado = 'CERRADA', cerrada_en = (?::date + time '12:00') AT TIME ZONE 'America/La_Paz'
                 WHERE id = ?
                """,
                dia.plusDays(5).toString(),
                posicion);

        assertThat(abierta.total().subtract(invertidoAl(dia.plusDays(10)).total()))
                .isEqualByComparingTo("700.00");
    }

    @Test
    @DisplayName("límite: antes de toda posición no hay saldo invertido (SIN_DATOS), nunca un cero")
    void sinDatos() {
        var c = tx.en(() -> datos.conContexto(
                sistema(), d -> new IndicadoresDeInversion(repositorio).calcular(d, PeriodoDeTablero.de("1990"))));

        assertThat(c.get(0).indicador().codigo()).isEqualTo("SALDO_INVERTIDO");
        assertThat(c.get(0).indicador().valor()).isNull();
    }
}
