package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro;
import bo.aportaya.nucleofinanciero.dominio.CondicionesDeRetiro.Situacion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Condiciones de autenticación y encaje antes de crear un retiro. */
class CU11CondicionesTest extends BaseDeBilletera {

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private record Escenario(UUID cuenta, UUID instrumento, ContextoSesion ctx) {}

    private Escenario escenario() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", "ESTANDAR", "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("1000.00"));
        return new Escenario(cuenta, custodia.instrumentoDestino(usuario, true, true, null), contextoDe(usuario));
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("rechaza sin segundo factor: el retiro no sale sin MFA")
    void rechazaSinMfa() {
        Escenario e = escenario();

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(
                        new EntradaRetiro(
                                "ret-sinmfa",
                                e.cuenta(),
                                bob("100.00"),
                                bob("5.00"),
                                e.instrumento(),
                                false,
                                false,
                                false),
                        e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("segundo factor");
    }

    @Test
    @DisplayName(
            "rechaza con MFA_INVALIDO (no MFA_REQUERIDO, H2.S2.M5) cuando SI se mando evidencia pero no paso la verificacion")
    void rechazaMfaInvalido() {
        Escenario e = escenario();

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.solicitar(
                        new EntradaRetiro(
                                "ret-mfa-invalido",
                                e.cuenta(),
                                bob("100.00"),
                                bob("5.00"),
                                e.instrumento(),
                                false,
                                true,
                                false),
                        e.ctx())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no es valida");
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.orden_retiro"))
                .isZero();
    }

    @Test
    @DisplayName("rechaza con el encaje roto: no salen retiros nuevos")
    void rechazaEncajeRoto() {
        var conEncaje =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), false, true);
        var sinEncaje =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), false, false);
        OffsetDateTime ahora = OffsetDateTime.of(2026, 8, 27, 12, 0, 0, 0, ZoneOffset.UTC);

        assertThat(CondicionesDeRetiro.evaluar(conEncaje, ahora).permitido()).isTrue();
        assertThat(CondicionesDeRetiro.evaluar(sinEncaje, ahora).codigo()).isEqualTo("ENCAJE_INCUMPLIDO");
        var conOficio =
                new Situacion(true, true, bob("1000.00"), bob("100.00"), true, true, Optional.empty(), true, true);
        assertThat(CondicionesDeRetiro.evaluar(conOficio, ahora).codigo()).isEqualTo("BLOQUEO_DE_AUTORIDAD");
    }
}
