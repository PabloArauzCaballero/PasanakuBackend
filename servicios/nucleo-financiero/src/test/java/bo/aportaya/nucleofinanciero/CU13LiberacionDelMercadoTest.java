package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU13LiberarRetencionDelMercado;
import bo.aportaya.nucleofinanciero.infraestructura.RetencionRepositorio;
import bo.aportaya.plataforma.datos.Datos;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CU-13 · {@code entregas} devuelve el saldo de una puja superada: la retencion es del postor, pero la libera el
 * servicio (al postor lo supera otra persona, o la subasta cierra sin sesion).
 */
class CU13LiberacionDelMercadoTest extends BaseDeMercado {

    private CU13LiberarRetencionDelMercado liberacion() {
        return new CU13LiberarRetencionDelMercado(new Datos(dsl), new RetencionRepositorio(), retencionCU);
    }

    private String estadoDe(UUID retencionId) {
        return String.valueOf(dsl.fetchOne("SELECT estado FROM nucleo_financiero.retencion_saldo WHERE id = ?", retencionId)
                .get(0));
    }

    private BigDecimal disponibleDe(UUID cuenta) {
        return (BigDecimal) dsl.fetchOne(
                        "SELECT saldo_disponible FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta)
                .get(0);
    }

    private BigDecimal retenidoDe(UUID cuenta) {
        return (BigDecimal) dsl.fetchOne(
                        "SELECT saldo_retenido FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta)
                .get(0);
    }

    @Test
    @DisplayName("Dada la puja retenida de Bs 500 de un postor con Bs 1.000 · Cuando entregas la libera con su identidad de servicio · Entonces la retención queda LIBERADA y el postor vuelve a tener Bs 1.000 disponibles")
    void elServicioDevuelveElSaldoAlPostor() {
        assertThat(disponibleDe(cuentaComprador)).isEqualByComparingTo("500.00");
        assertThat(retenidoDe(cuentaComprador)).isEqualByComparingTo("500.00");

        var salida = transaccion.execute(t -> liberacion().liberar(retencion, servicio));

        assertThat(salida.estado()).isEqualTo("LIBERADA");
        assertThat(estadoDe(retencion)).isEqualTo("LIBERADA");
        assertThat(disponibleDe(cuentaComprador)).isEqualByComparingTo("1000.00");
        assertThat(retenidoDe(cuentaComprador)).isEqualByComparingTo("0.00");
        assertThat(sistemaFx.saldoDelLibro(cuentaVendedor))
                .as("liberar nunca le da la plata a otro")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Dada una retención ya liberada · Cuando entregas la vuelve a liberar · Entonces AP-CU13-03 con estado LIBERADA y el saldo no se duplica")
    void liberarDosVecesNoDuplica() {
        transaccion.execute(t -> liberacion().liberar(retencion, servicio));

        assertThatThrownBy(() -> transaccion.execute(t -> liberacion().liberar(retencion, servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-03"));
        assertThat(disponibleDe(cuentaComprador)).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Dada una sesión de usuario final · Cuando intenta liberar la retención de otro por esta vía · Entonces AP-CU13-09 y la retención sigue RETENIDA")
    void unUsuarioNoLaLibera() {
        var otro = contextoDe(vendedor);

        assertThatThrownBy(() -> transaccion.execute(t -> liberacion().liberar(retencion, otro)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-09"));
        assertThat(estadoDe(retencion)).isNotEqualTo("LIBERADA");
        assertThat(retenidoDe(cuentaComprador)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("Dada una retención que no es de una compra en el mercado · Cuando entregas la quiere liberar · Entonces AP-CU13-08 y no se toca")
    void soloRetencionesDelMercado() {
        UUID inversion = retenerInversion(comprador, cuentaComprador, "100.00", UUID.randomUUID());

        assertThatThrownBy(() -> transaccion.execute(t -> liberacion().liberar(inversion, servicio)))
                .satisfies(e -> assertThat(codigo(e)).isEqualTo("AP-CU13-08"));
        assertThat(estadoDe(inversion)).isNotEqualTo("LIBERADA");
    }
}
