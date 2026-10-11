package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo.EntradaSolicitud;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.SalidaReverso;
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
 * H11.S1.M2 · Revertir los cargos de una operacion: una vez, con asiento NUEVO y sin tocar el original.
 *
 * <p>El costo de una recarga ya viene descontado del credito (se acredita el neto), asi que revertir la
 * recarga devuelve el neto con un contra-asiento y avisa a {@code tarifas} que el cargo se devuelve. La nota de
 * credito y las contrapartidas fiscales son de {@code tarifas}; aca se prueba el aviso, su unicidad y que el
 * libro quede cuadrado, no el documento fiscal.
 */
class CU14CargosTest extends BaseDeBilletera {

    private static final String MOTIVO = "Cobro duplicado por un reintento del proveedor de pagos";

    private UUID cuenta;
    private ContextoSesion titular;

    @BeforeEach
    void preparar() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private record Recarga(UUID orden, UUID transaccion, UUID cotizacion) {}

    private Recarga recargar(String monto, String costo) {
        UUID cotizacion = UUID.randomUUID();
        var orden = transaccion.execute(t -> recargaCU.solicitar(
                new EntradaSolicitud(
                        "rec-" + UUID.randomUUID(),
                        cuenta,
                        Dinero.de(monto, Moneda.BOB),
                        Dinero.de(costo, Moneda.BOB),
                        "QR",
                        Optional.empty(),
                        Optional.of(cotizacion)),
                titular));
        var acreditada = transaccion.execute(t -> recargaCU.acreditar(
                orden.ordenRecargaId(), ConfirmacionesDePrueba.para(dsl, orden.ordenRecargaId()), titular));
        return new Recarga(orden.ordenRecargaId(), acreditada.transaccionId(), cotizacion);
    }

    private SalidaReverso reversar(Recarga r, String clave) {
        return transaccion.execute(t -> reversoCU.ejecutar(
                new EntradaReverso(clave, r.transaccion(), "CONTRACARGO", MOTIVO, fixtura.usuario()), titular));
    }

    private int total() {
        return contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    private int avisos(UUID orden) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.evento_dominio WHERE agregado_id=? AND tipo='nucleo_financiero.cargo_a_revertir'",
                orden);
    }

    @Test
    @DisplayName(
            "Dada una recarga acreditada con un cargo cotizado · Cuando se reversa · Entonces existe un contra-asiento nuevo que cuadra, el original queda intacto y la orden queda REVERSADA · Y se avisa el cargo una sola vez citando su cotización")
    void reversaDelCargo() {
        var r = recargar("500.00", "5.00");
        assertThat(total()).isEqualTo(495);
        int movimientosDelOriginal = contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                r.transaccion());

        var salida = reversar(r, "rev-1");

        assertThat(salida.transaccionReversoId()).isNotNull().isNotEqualTo(r.transaccion());
        assertThat(total()).as("el neto acreditado se devuelve").isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                        r.transaccion()))
                .as("el original no se edita ni se le agregan patas")
                .isEqualTo(movimientosDelOriginal);
        assertThat(contar(
                        "SELECT COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0)::int FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id=?",
                        salida.transaccionReversoId()))
                .as("el contra-asiento cuadra")
                .isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='REVERSADA'",
                        r.orden()))
                .isEqualTo(1);
        assertThat(avisos(r.orden())).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.evento_dominio WHERE agregado_id=? AND tipo='nucleo_financiero.cargo_a_revertir' AND payload->>'cotizacionId'=?",
                        r.orden(),
                        r.cotizacion().toString()))
                .as("el aviso cita la cotizacion del cargo")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza reversar dos veces la misma recarga: el cargo no se avisa dos veces")
    void elCargoSeRevierteUnaSolaVez() {
        var r = recargar("500.00", "5.00");
        reversar(r, "rev-2");

        assertThatThrownBy(() -> reversar(r, "rev-2-otra")).isInstanceOf(ErrorDeNegocio.class);
        assertThatThrownBy(() -> reversar(r, "rev-2")).isInstanceOf(ErrorDeNegocio.class);

        assertThat(avisos(r.orden())).isEqualTo(1);
        assertThat(total()).isZero();
    }

    @Test
    @DisplayName(
            "Dada una recarga acreditada sin costo · Cuando se reversa · Entonces no hay cargo que avisar · Y la recarga igual queda REVERSADA")
    void sinCostoNoHayAviso() {
        var r = recargar("500.00", "0.00");
        reversar(r, "rev-3");
        assertThat(avisos(r.orden())).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='REVERSADA'",
                        r.orden()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una recarga con cargo cuyo saldo acreditado ya se gastó · Cuando se reversa · Entonces el reverso se convierte en obligación de restitución · Y el cargo no se avisa y la orden sigue ACREDITADA")
    void sinSaldoNoSeRevierteElCargo() {
        EscenarioDeRetiro.catalogo();
        var r = recargar("500.00", "5.00");
        UUID destino = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        transaccion.execute(t -> transferenciaCU.ejecutar(
                new bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia(
                        "gasto",
                        cuenta,
                        destino,
                        Dinero.de("495.00", Moneda.BOB),
                        "gasto",
                        Optional.empty(),
                        Optional.empty()),
                titular));

        var salida = reversar(r, "rev-4");

        assertThat(salida.generaObligacionDeRestitucion()).isTrue();
        assertThat(avisos(r.orden())).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='ACREDITADA'",
                        r.orden()))
                .isEqualTo(1);
    }
}
