package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.dominio.puertos.ProveedorDeRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S2.M3 · {@code ReconciliacionDeRetiros} resuelve lo que {@code instruirPago} dejo
 * {@code EN_PROCESO} sin saber que paso de verdad (un {@code TIMEOUT} del proveedor).
 *
 * <p>El monto {@code 111.11} es la marca de {@code ProveedorDeRetiroLocal} para forzar
 * un TIMEOUT en {@code instruir}; {@code resolverComoSiElProveedorHubieraContestado}
 * simula que, en una corrida posterior, el proveedor SI contesta.
 */
class ReconciliacionDeRetirosTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private record Escenario(UUID usuario, UUID cuenta, UUID instrumento, ContextoSesion ctx) {}

    private Escenario escenario(String saldo) {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal(saldo));
        return new Escenario(
                usuario, cuenta, custodia.instrumentoDestino(usuario, true, true, null), contextoDe(usuario));
    }

    /** Neto exactamente 111.11: monto 116.11 menos costo 5.00. */
    private SalidaRetiro pedirQueDemora(Escenario e, String clave) {
        return transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        clave,
                        e.cuenta(),
                        Dinero.de("116.11", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        e.instrumento(),
                        true,
                        true,
                        false),
                e.ctx()));
    }

    @Test
    @DisplayName(
            "una orden EN_PROCESO cuyo proveedor sigue en TIMEOUT queda EN_PROCESO tras reconciliar, sin tocar el saldo")
    void siSigueEnTimeoutNoTocaNada() {
        Escenario e = escenario("1000.00");
        SalidaRetiro salida = pedirQueDemora(e, "ret-reconc-timeout");
        var instruccion = transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));
        assertThat(instruccion.estado()).isEqualTo("EN_PROCESO");

        // BaseDeBilletera arma retiroCU/reconciliacionCU a mano, sin proxy de Spring:
        // @Transactional en CU11.ordenesEnProceso no tiene efecto aca (SI lo tiene en
        // produccion, donde @Scheduled dispara correr() sin transaccion ambiente y
        // ESE metodo la abre). En la prueba, la transaccion ambiente la pone este
        // wrapper — mismo patron que el resto de la suite usa para cualquier llamada
        // a un caso de uso.
        transaccion.execute(t -> {
            reconciliacionCU.correr();
            return null;
        });

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado ="
                                + " 'EN_PROCESO'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "el proveedor contesta ACEPTADO despues del TIMEOUT: reconciliar confirma el pago (EN_PROCESO -> PAGADA)")
    void aceptaDespuesDelTimeout() {
        Escenario e = escenario("1000.00");
        SalidaRetiro salida = pedirQueDemora(e, "ret-reconc-acepta");
        var instruccion = transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));
        assertThat(instruccion.estado()).isEqualTo("EN_PROCESO");

        proveedorDeRetiro.resolverComoSiElProveedorHubieraContestado(
                instruccion.referenciaProveedor(), ProveedorDeRetiro.Estado.ACEPTADO);
        // BaseDeBilletera arma retiroCU/reconciliacionCU a mano, sin proxy de Spring:
        // @Transactional en CU11.ordenesEnProceso no tiene efecto aca (SI lo tiene en
        // produccion, donde @Scheduled dispara correr() sin transaccion ambiente y
        // ESE metodo la abre). En la prueba, la transaccion ambiente la pone este
        // wrapper — mismo patron que el resto de la suite usa para cualquier llamada
        // a un caso de uso.
        transaccion.execute(t -> {
            reconciliacionCU.correr();
            return null;
        });

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado = 'PAGADA'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
        // La reconciliacion es un trabajo del SISTEMA: la transaccion que confirma el
        // pago no tiene un autor humano (H4.S2.M3, mismo patron que CU24RegistrarAsiento).
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE origen_id = ? AND"
                                + " iniciada_por IS NULL AND canal = 'BATCH'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "el proveedor contesta RECHAZADO despues del TIMEOUT: reconciliar rechaza y libera la retencion (EN_PROCESO -> RECHAZADA)")
    void rechazaDespuesDelTimeout() {
        Escenario e = escenario("1000.00");
        SalidaRetiro salida = pedirQueDemora(e, "ret-reconc-rechaza");
        var instruccion = transaccion.execute(t -> retiroCU.instruirPago(salida.ordenRetiroId(), e.ctx()));
        assertThat(instruccion.estado()).isEqualTo("EN_PROCESO");

        proveedorDeRetiro.resolverComoSiElProveedorHubieraContestado(
                instruccion.referenciaProveedor(), ProveedorDeRetiro.Estado.RECHAZADO);
        // BaseDeBilletera arma retiroCU/reconciliacionCU a mano, sin proxy de Spring:
        // @Transactional en CU11.ordenesEnProceso no tiene efecto aca (SI lo tiene en
        // produccion, donde @Scheduled dispara correr() sin transaccion ambiente y
        // ESE metodo la abre). En la prueba, la transaccion ambiente la pone este
        // wrapper — mismo patron que el resto de la suite usa para cualquier llamada
        // a un caso de uso.
        transaccion.execute(t -> {
            reconciliacionCU.correr();
            return null;
        });

        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado ="
                                + " 'RECHAZADA'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado ="
                                + " 'LIBERADA' AND liberada_por IS NULL",
                        salida.retencionId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        e.cuenta()))
                .isEqualTo(1000);
    }
}
