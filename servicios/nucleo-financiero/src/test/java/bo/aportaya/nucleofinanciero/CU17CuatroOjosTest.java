package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.EntradaBloqueo;
import bo.aportaya.nucleofinanciero.aplicacion.CU17BloquearPorAutoridad.SalidaBloqueo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** F1.L1.M10 (ANF-SEG-006) · R-BIL-32: el bloqueo por autoridad lo levanta otra persona y el saldo vuelve. */
class CU17CuatroOjosTest extends BaseDeBilletera {

    private static final String HASH = "b".repeat(64);

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private UUID cuentaCon(UUID usuario, String saldo) {
        fixtura.tipoDeCambioDeHoy();
        UUID cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal(saldo));
        return cuenta;
    }

    private SalidaBloqueo bloquear(UUID cuenta, String monto, ContextoSesion quien) {
        return transaccion.execute(t -> bloqueoCU.ejecutar(
                new EntradaBloqueo(
                        cuenta,
                        "JUZGADO",
                        "INMOVILIZACION",
                        "OF-" + UUID.randomUUID().toString().substring(0, 8),
                        Optional.of(Dinero.de(monto, Moneda.BOB)),
                        "PARCIAL",
                        "https://oficios/sintetico.pdf",
                        HASH),
                quien));
    }

    private int retenido(UUID cuenta) {
        return contar("SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuenta);
    }

    @Test
    @DisplayName("rechaza que quien registro el bloqueo lo levante (AP-CU17-04) y el saldo sigue retenido")
    void rechazaLaMismaPersona() {
        UUID oficial = fixtura.usuario();
        UUID cuenta = cuentaCon(fixtura.usuario(), "5000.00");
        ContextoSesion registra = contextoDe(oficial);
        SalidaBloqueo b = bloquear(cuenta, "2000.00", registra);

        assertThatThrownBy(() -> transaccion.execute(t -> bloqueoCU.levantar(b.bloqueoId(), registra)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("otra persona");
        assertThat(retenido(cuenta)).isEqualTo(2000);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.bloqueo_saldo WHERE id = ? AND estado = 'VIGENTE'",
                        b.bloqueoId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "cuadre: cuando otra persona levanta el bloqueo, la retencion se libera y el saldo vuelve a estar disponible")
    void cuadreAlLevantar() {
        UUID cuenta = cuentaCon(fixtura.usuario(), "5000.00");
        ContextoSesion registra = contextoDe(fixtura.usuario());
        ContextoSesion levanta = contextoDe(fixtura.usuario());
        SalidaBloqueo b = bloquear(cuenta, "2000.00", registra);
        assertThat(retenido(cuenta)).isEqualTo(2000);

        transaccion.execute(t -> bloqueoCU.levantar(b.bloqueoId(), levanta));

        assertThat(retenido(cuenta)).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado = 'LIBERADA'",
                        b.retencionId()))
                .isEqualTo(1);
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM nucleo_financiero.bloqueo_saldo
                         WHERE id = ? AND estado = 'LEVANTADO' AND registrado_por = ? AND levantada_por = ?
                        """,
                        b.bloqueoId(),
                        registra.usuarioId(),
                        levanta.usuarioId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "rechaza la base que levante el bloqueo la misma persona que lo registro (ck_bloqueo_saldo_cuatro_ojos)")
    void rechazaLaBase() {
        UUID cuenta = cuentaCon(fixtura.usuario(), "5000.00");
        ContextoSesion registra = contextoDe(fixtura.usuario());
        SalidaBloqueo b = bloquear(cuenta, "1000.00", registra);

        assertThatThrownBy(() -> transaccion.execute(t -> dsl.execute(
                        """
                        UPDATE nucleo_financiero.bloqueo_saldo
                           SET estado = 'LEVANTADO', levantada_por = registrado_por, levantado_en = now()
                         WHERE id = ?
                        """,
                        b.bloqueoId())))
                .hasMessageContaining("ck_bloqueo_saldo_cuatro_ojos");
    }
}
