package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.EntradaSolicitud;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M3 / H4.S1.M1 · Repetir la clave es repetir la MISMA solicitud.
 *
 * <p>Cada campo que la define se cambia por separado: si alguno pudiera cambiar sin que la
 * clave lo note, el reintento le devolveria a la persona una orden que no pidio.
 */
class CU10IdempotenciaTest extends BaseDeBilletera {

    private UUID usuario;
    private UUID cuenta;
    private ContextoSesion titular;

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private EntradaSolicitud pedido(String clave, String monto, String medio, Optional<UUID> instrumento) {
        return new EntradaSolicitud(
                clave, cuenta, Dinero.de(monto, Moneda.BOB), Dinero.de("5.00", Moneda.BOB), medio, instrumento);
    }

    private <T> T enTransaccion(java.util.function.Supplier<T> trabajo) {
        return transaccion.execute(e -> trabajo.get());
    }

    private int ordenes() {
        return contar("SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?", cuenta);
    }

    @Test
    @DisplayName("reintento: la misma solicitud con la misma clave devuelve la misma orden, dos veces, sin duplicar")
    void mismaSolicitudMismaOrden() {
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        var una = enTransaccion(
                () -> recargaCU.solicitar(pedido("k1", "300.00", "QR", Optional.of(instrumento)), titular));
        var otra = enTransaccion(
                () -> recargaCU.solicitar(pedido("k1", "300.00", "QR", Optional.of(instrumento)), titular));
        assertThat(otra).isEqualTo(una);
        assertThat(ordenes()).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza cambiar el medio de fondeo con la misma clave: no crea otra orden")
    void cambiarElMedioSeRechaza() {
        enTransaccion(() -> recargaCU.solicitar(pedido("k2", "300.00", "QR", Optional.empty()), titular));
        assertThatThrownBy(() -> enTransaccion(
                        () -> recargaCU.solicitar(pedido("k2", "300.00", "TARJETA", Optional.empty()), titular)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        assertThat(ordenes()).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza cambiar el instrumento de fondeo con la misma clave")
    void cambiarElInstrumentoSeRechaza() {
        UUID uno = custodia.instrumentoDestino(usuario, true, true, null);
        // Solo puede haber un instrumento principal por titular: el segundo es otro identificador, y
        // la comparacion de la repeticion ocurre antes de validarlo.
        UUID otro = UUID.randomUUID();
        enTransaccion(() -> recargaCU.solicitar(pedido("k3", "300.00", "QR", Optional.of(uno)), titular));
        assertThatThrownBy(() -> enTransaccion(
                        () -> recargaCU.solicitar(pedido("k3", "300.00", "QR", Optional.of(otro)), titular)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("idempotencia");
        assertThatThrownBy(() -> enTransaccion(
                        () -> recargaCU.solicitar(pedido("k3", "300.00", "QR", Optional.empty()), titular)))
                .as("quitar el instrumento tambien es otra solicitud")
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza agregar o cambiar la cotizacion aceptada con la misma clave")
    void cambiarLaCotizacionSeRechaza() {
        UUID cotizacion = UUID.randomUUID();
        var con = new EntradaSolicitud(
                "k4",
                cuenta,
                Dinero.de("300.00", Moneda.BOB),
                Dinero.de("5.00", Moneda.BOB),
                "QR",
                Optional.empty(),
                Optional.of(cotizacion));
        var primera = enTransaccion(() -> recargaCU.solicitar(con, titular));
        assertThat(enTransaccion(() -> recargaCU.solicitar(con, titular))).isEqualTo(primera);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cotizacion_id=?", cotizacion))
                .isEqualTo(1);
        assertThatThrownBy(() -> enTransaccion(
                        () -> recargaCU.solicitar(pedido("k4", "300.00", "QR", Optional.empty()), titular)))
                .isInstanceOf(ErrorDeNegocio.class);
    }

    @Test
    @DisplayName("rechaza cambiar importe o costo con la misma clave")
    void cambiarImporteOCostoSeRechaza() {
        enTransaccion(() -> recargaCU.solicitar(pedido("k5", "300.00", "QR", Optional.empty()), titular));
        assertThatThrownBy(() -> enTransaccion(
                        () -> recargaCU.solicitar(pedido("k5", "301.00", "QR", Optional.empty()), titular)))
                .isInstanceOf(ErrorDeNegocio.class);
        var otroCosto = new EntradaSolicitud(
                "k5", cuenta, Dinero.de("300.00", Moneda.BOB), Dinero.de("6.00", Moneda.BOB), "QR", Optional.empty());
        assertThatThrownBy(() -> enTransaccion(() -> recargaCU.solicitar(otroCosto, titular)))
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(ordenes()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dados dos titulares que usan la misma clave de idempotencia · Cuando cada uno solicita su recarga · Entonces cada uno tiene su propia orden: la clave se ampara en la billetera · Y repetir la clave de uno devuelve su orden, no la del otro")
    void laClaveEsDeCadaBilletera() {
        UUID otroUsuario = fixtura.usuario();
        UUID otraCuenta = fixtura.billetera(otroUsuario, "ESTANDAR", BigDecimal.ZERO);
        var mia = enTransaccion(
                () -> recargaCU.solicitar(pedido("compartida", "300.00", "QR", Optional.empty()), titular));
        var suya = enTransaccion(() -> recargaCU.solicitar(
                new EntradaSolicitud(
                        "compartida",
                        otraCuenta,
                        Dinero.de("300.00", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        "QR",
                        Optional.empty()),
                contextoDe(otroUsuario)));
        assertThat(suya.ordenRecargaId()).isNotEqualTo(mia.ordenRecargaId());
        assertThat(enTransaccion(
                        () -> recargaCU.solicitar(pedido("compartida", "300.00", "QR", Optional.empty()), titular)))
                .isEqualTo(mia);
    }
}
