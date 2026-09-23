package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S2.M6 · metricas de negocio de CU-11: contadores reales, no solo declarados.
 *
 * <p>{@code withdrawal_requested_total}/{@code _approved_total}/{@code _failed_total}
 * (Micrometer {@code Counter}, ver {@code CU11RetirarSaldo}) — lo que un dashboard de
 * operacion necesita para saber, sin leer un log, cuantos retiros se piden, cuantos se
 * autorizan (automatico o por aprobacion) y cuantos se rechazan (MFA, limites,
 * revision, proveedor).
 */
class CU11MetricasTest extends BaseDeBilletera {

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

    private double contador(String nombre) {
        var c = metricas.find(nombre).counter();
        return c == null ? 0.0 : c.count();
    }

    @Test
    @DisplayName("Dado un retiro bajo el umbral · Cuando se solicita y queda AUTORIZADA · Entonces las metricas"
            + " withdrawal_requested_total y withdrawal_approved_total suman, y withdrawal_failed_total no")
    void retiroAutorizadoSumaRequestedYApproved() {
        double antesRequested = contador("withdrawal_requested_total");
        double antesApproved = contador("withdrawal_approved_total");
        double antesFailed = contador("withdrawal_failed_total");

        Escenario e = escenario("1000.00");
        SalidaRetiro salida = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        "ret-metrica-1",
                        e.cuenta(),
                        Dinero.de("100.00", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        e.instrumento(),
                        true,
                        true,
                        false),
                e.ctx()));

        assertThat(salida.estado()).isEqualTo("AUTORIZADA");
        assertThat(contador("withdrawal_requested_total")).isEqualTo(antesRequested + 1);
        assertThat(contador("withdrawal_approved_total")).isEqualTo(antesApproved + 1);
        assertThat(contador("withdrawal_failed_total")).isEqualTo(antesFailed);
    }

    @Test
    @DisplayName("Dado un retiro que no pasa una condicion dura · Cuando se rechaza antes de crear la orden · Entonces"
            + " solo suma la metrica withdrawal_failed_total, nunca requested ni approved")
    void retiroRechazadoSumaSoloFailed() {
        double antesRequested = contador("withdrawal_requested_total");
        double antesApproved = contador("withdrawal_approved_total");
        double antesFailed = contador("withdrawal_failed_total");

        Escenario e = escenario("1000.00");
        try {
            transaccion.execute(t -> retiroCU.solicitar(
                    new EntradaRetiro(
                            "ret-metrica-2",
                            e.cuenta(),
                            Dinero.de("100.00", Moneda.BOB),
                            Dinero.de("5.00", Moneda.BOB),
                            e.instrumento(),
                            false,
                            false,
                            false),
                    e.ctx()));
        } catch (bo.aportaya.plataforma.dominio.ErrorDeNegocio esperado) {
            // El rechazo es el punto de la prueba: la condicion dura (MFA_REQUERIDO)
            // corta ANTES de crear la orden.
        }

        // La orden nunca se creo: "requested" cuenta ordenes CREADAS, no intentos.
        assertThat(contador("withdrawal_requested_total")).isEqualTo(antesRequested);
        assertThat(contador("withdrawal_approved_total")).isEqualTo(antesApproved);
        assertThat(contador("withdrawal_failed_total")).isEqualTo(antesFailed + 1);
    }
}
