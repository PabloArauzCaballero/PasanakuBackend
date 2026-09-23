package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibroInvariantesTest extends BaseDeLibroInvariantes {
    // ------------------------------------------------------------ 1 · transferencia OK --
    @Test
    @DisplayName("escenario 1: transferencia normal — cuadra por transaccion, saldo total del sistema se preserva")
    void transferenciaOk() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();

        SalidaTransferencia salida = transferir(p, "500.00", "inv-1");

        assertThat(descuadreDe(salida.transaccionId())).isZero();
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(500);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.destino()))
                .isEqualTo(500);
    }

    // ------------------------------------------------------- 2 · saldo insuficiente --
    @Test
    @DisplayName("escenario 2: saldo insuficiente — se rechaza, cero efecto, cuadre intacto")
    void saldoInsuficiente() {
        Par p = par("100.00");
        int totalAntes = saldoTotalDelSistema();

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-2")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(100);
    }

    // -------------------------------------------------------- 3 · moneda distinta --
    @Test
    @DisplayName("escenario 3: moneda distinta entre origen y destino — se rechaza, cero efecto")
    void monedaDistinta() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destinoUsuario = fixtura.usuario();
        UUID destino = fixtura.billetera(destinoUsuario, ESTANDAR, BigDecimal.ZERO);
        dsl.execute("UPDATE nucleo_financiero.cuenta_billetera SET moneda = 'USD' WHERE id = ?", destino);
        int totalAntes = saldoTotalDelSistema();
        Par p = new Par(origen, destino, contextoDe(quienPaga));

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-3")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }

    // -------------------------------------------------------- 4 · cuenta bloqueada --
    @Test
    @DisplayName("escenario 4: cuenta de destino bloqueada — se rechaza, cero efecto")
    void cuentaBloqueada() {
        Par p = par("1000.00");
        // `BLOQUEADA` no es un valor del CHECK (ck_cuenta_billetera_estado): los
        // reales son ACTIVA/BLOQUEADA_AUTORIDAD/CERRADA/CONGELADA/EN_APERTURA/
        // EN_CIERRE/LIMITADA — encontrado corriendo esto contra PostgreSQL real.
        dsl.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'CONGELADA' WHERE id = ?", p.destino());
        int totalAntes = saldoTotalDelSistema();

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-4")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }

    // ------------------------------------------------------- 5 · P2P no permitido --
    @Test
    @DisplayName("escenario 5: la politica de la cuenta de origen no permite P2P — se rechaza, cero efecto")
    void p2pNoPermitido() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        // La politica de la fixture siempre permite P2P: se apaga a mano, sin
        // tocar la fixture compartida (no es de este carril).
        dsl.execute(
                """
                UPDATE nucleo_financiero.politica_billetera SET permite_transferencia_p2p = false
                 WHERE id = (SELECT politica_billetera_id FROM nucleo_financiero.cuenta_billetera WHERE id = ?)
                """,
                origen);
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        int totalAntes = saldoTotalDelSistema();
        Par p = new Par(origen, destino, contextoDe(quienPaga));

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-5"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no permite");

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }
}
