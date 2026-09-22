package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaAprobacion;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H3.S2 · la aprobacion la hace cumplir la aplicacion, no solo el CHECK de la base.
 *
 * <p>Todas las pruebas aca piden un monto POR ENCIMA de
 * {@code aportaya.retiro.doble-aprobacion-desde} (5000.00 en {@code application.yml}),
 * asi que la orden nace {@code EN_REVISION} — es el escenario que {@code aprobar}/
 * {@code rechazarRevision} necesitan para tener algo que resolver.
 */
class CU11AprobacionTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Escenario(UUID solicitante, UUID cuenta, UUID instrumento, ContextoSesion ctxSolicitante) {}

    private Escenario escenarioGrande() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50000.00"));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        return new Escenario(usuario, cuenta, instrumento, contextoDe(usuario));
    }

    private SalidaRetiro pedirGrande(Escenario e, String clave) {
        return transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        clave, e.cuenta(), bob("6000.00"), bob("5.00"), e.instrumento(), true, true, true),
                e.ctxSolicitante()));
    }

    @Test
    @DisplayName(
            "Dado un retiro por encima del umbral de doble aprobacion · Cuando se solicita · Entonces la orden"
                    + " nace EN_REVISION, no AUTORIZADA")
    void solicitudGrandeQuedaEnRevision() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-1");
        assertThat(salida.estado()).isEqualTo("EN_REVISION");
    }

    @Test
    @DisplayName(
            "Dado un retiro EN_REVISION · Cuando un aprobador DISTINTO del solicitante lo aprueba · Entonces la"
                    + " orden pasa a AUTORIZADA con aprobada_por igual al aprobador")
    void aprueba() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-2");
        ContextoSesion aprobador = contextoDe(fixtura.usuario());

        SalidaAprobacion resultado =
                transaccion.execute(t -> retiroCU.aprobar(salida.ordenRetiroId(), aprobador));

        assertThat(resultado.estado()).isEqualTo("AUTORIZADA");
        assertThat(resultado.aprobadaPor()).isEqualTo(aprobador.usuarioId());
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado ="
                                + " 'AUTORIZADA' AND aprobada_por = ?",
                        salida.ordenRetiroId(),
                        aprobador.usuarioId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza: un aprobador DISTINTO del solicitante la rechaza y libera la retencion")
    void rechaza() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-3");
        ContextoSesion aprobador = contextoDe(fixtura.usuario());

        SalidaAprobacion resultado =
                transaccion.execute(t -> retiroCU.rechazarRevision(salida.ordenRetiroId(), aprobador));

        assertThat(resultado.estado()).isEqualTo("RECHAZADA");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.retencion_saldo WHERE id = ? AND estado ="
                                + " 'LIBERADA'",
                        salida.retencionId()))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                        e.cuenta()))
                .isEqualTo(50000);
    }

    @Test
    @DisplayName("rechaza la auto-aprobacion: el solicitante no puede aprobar su propia orden")
    void autoAprobacionProhibida() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-4");

        assertThatThrownBy(() -> transaccion.execute(
                        t -> retiroCU.aprobar(salida.ordenRetiroId(), e.ctxSolicitante())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("propia solicitud");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado ="
                                + " 'EN_REVISION'",
                        salida.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza por orden inexistente: 404 de negocio")
    void ordenInexistente() {
        assertThatThrownBy(() -> transaccion.execute(
                        t -> retiroCU.aprobar(UUID.randomUUID(), contextoDe(fixtura.usuario()))))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no existe");
    }

    @Test
    @DisplayName("rechaza por ya resuelta: aprobar una orden que otro aprobador YA aprobo, falla (no doble efecto)")
    void yaResuelta() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-5");
        ContextoSesion primerAprobador = contextoDe(fixtura.usuario());
        ContextoSesion segundoAprobador = contextoDe(fixtura.usuario());

        transaccion.execute(t -> retiroCU.aprobar(salida.ordenRetiroId(), primerAprobador));

        assertThatThrownBy(() -> transaccion.execute(
                        t -> retiroCU.aprobar(salida.ordenRetiroId(), segundoAprobador)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no esta en revision");
        // Una sola orden AUTORIZADA, con el PRIMER aprobador — el segundo no la piso.
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND aprobada_por = ?",
                        salida.ordenRetiroId(),
                        primerAprobador.usuarioId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("reintento idempotente: aprobar dos veces la MISMA orden con el MISMO aprobador falla la segunda")
    void reintentoNoDuplica() {
        Escenario e = escenarioGrande();
        SalidaRetiro salida = pedirGrande(e, "ret-grande-6");
        ContextoSesion aprobador = contextoDe(fixtura.usuario());

        transaccion.execute(t -> retiroCU.aprobar(salida.ordenRetiroId(), aprobador));

        assertThatThrownBy(() -> transaccion.execute(t -> retiroCU.aprobar(salida.ordenRetiroId(), aprobador)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no esta en revision");
    }
}
