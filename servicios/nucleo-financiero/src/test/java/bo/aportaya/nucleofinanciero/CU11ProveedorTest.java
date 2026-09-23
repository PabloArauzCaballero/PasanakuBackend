package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El rechazo firme del proveedor libera la retención y devuelve el saldo. */
class CU11ProveedorTest extends BaseDeBilletera {

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    @Test
    @DisplayName(
            "rechaza por fallo del proveedor tras autorizar (H4.S1): la orden pasa a RECHAZADA y libera la retencion")
    void elProveedorRechazaTrasAutorizar() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", "ESTANDAR", "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("2000.00"));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        var ctx = contextoDe(usuario);

        // Neto 666.66: monto 671.66 menos costo 5.00, según el perfil test.
        var solicitud = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        "r-proveedor-rechaza",
                        cuenta,
                        Dinero.de("671.66", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        instrumento,
                        true,
                        true,
                        false),
                ctx));
        assertThat(solicitud.estado()).isEqualTo("AUTORIZADA");

        var instruccion = transaccion.execute(t -> retiroCU.instruirPago(solicitud.ordenRetiroId(), ctx));

        assertThat(instruccion.estado()).isEqualTo("RECHAZADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado = 'RECHAZADA'",
                        solicitud.ordenRetiroId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado = 'LIBERADA'",
                        solicitud.retencionId()))
                .isEqualTo(1);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta))
                .isEqualTo(2000);
    }
}
