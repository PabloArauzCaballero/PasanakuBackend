package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU14ReversarTransaccion.EntradaReverso;
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

/**
 * F1.L1.M03 · CU-14 con cuatro ojos reales (P-13, R-BIL-27), contra la base: pedir no mueve saldo, la
 * misma persona no autoriza, otra persona ejecuta una sola vez y la base guarda a las dos.
 */
class CU14CuatroOjosTest extends BaseDeBilletera {

    private static final String MOTIVO = "Cobro duplicado por un reintento del proveedor de pagos";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private record Movida(UUID transaccion, UUID origen, ContextoSesion pide) {}

    private Movida movida() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", "ESTANDAR", "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, "ESTANDAR", BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destino = fixtura.billetera(fixtura.usuario(), "ESTANDAR", BigDecimal.ZERO);
        ContextoSesion ctx = contextoDe(quienPaga);
        var t = transaccion.execute(x -> transferenciaCU.ejecutar(
                new EntradaTransferencia(
                        "mov-" + UUID.randomUUID(),
                        origen,
                        destino,
                        Dinero.de("300.00", Moneda.BOB),
                        "cobro",
                        Optional.empty(),
                        Optional.empty()),
                ctx));
        return new Movida(t.transaccionId(), origen, contextoDe(fixtura.usuario()));
    }

    private UUID pedir(Movida m, String clave) {
        return transaccion
                .execute(x -> reversoCU.solicitar(
                        new EntradaReverso(clave, m.transaccion(), "ERROR_OPERATIVO", MOTIVO), m.pide()))
                .solicitudId();
    }

    @Test
    @DisplayName(
            "Dada una solicitud de reverso creada por la persona A · Cuando la persona B, con su propio token, la autoriza · Entonces el reverso se ejecuta · Y la base guarda a A como solicitante y a B como autorizador")
    void otraPersonaAutoriza() {
        Movida m = movida();
        UUID solicitud = pedir(m, "rev-ojos-1");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.reverso_transaccion WHERE transaccion_original_id = ?",
                        m.transaccion()))
                .isZero();

        UUID autoriza = fixtura.usuario();
        var salida = transaccion.execute(x -> reversoCU.autorizar(solicitud, Reversos.otraPersona(m.pide(), autoriza)));

        assertThat(salida.transaccionReversoId()).isNotNull();
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM nucleo_financiero.solicitud_reverso
                         WHERE id = ? AND estado = 'EJECUTADA' AND solicitada_por = ?
                           AND autorizada_por = ? AND reverso_transaccion_id = ?
                        """,
                        solicitud,
                        m.pide().usuarioId(),
                        autoriza,
                        salida.reversoId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una solicitud de reverso creada por la persona A · Cuando A intenta autorizarla · Entonces se rechaza con AP-CU14-02 · Y no se mueve saldo")
    void mismaPersonaNoAutoriza() {
        Movida m = movida();
        UUID solicitud = pedir(m, "rev-ojos-2");

        assertThatThrownBy(() -> transaccion.execute(x -> reversoCU.autorizar(solicitud, m.pide())))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no puede ser quien lo solicita");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.reverso_transaccion WHERE transaccion_original_id = ?",
                        m.transaccion()))
                .isZero();
    }

    @Test
    @DisplayName(
            "reintento: autorizar dos veces ejecuta una sola vez (AP-CU14-05); pedir dos veces con la misma clave da la misma solicitud")
    void unaSolaVez() {
        Movida m = movida();
        UUID solicitud = pedir(m, "rev-ojos-3");
        assertThat(pedir(m, "rev-ojos-3")).isEqualTo(solicitud);

        var autoriza = Reversos.otraPersona(m.pide(), fixtura.usuario());
        transaccion.execute(x -> reversoCU.autorizar(solicitud, autoriza));
        assertThatThrownBy(() -> transaccion.execute(x -> reversoCU.autorizar(solicitud, autoriza)))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya fue resuelta");
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE tipo = 'REVERSO' AND origen_id = ?",
                        m.transaccion()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza una segunda solicitud viva para la misma transaccion (R-BIL-27)")
    void unaSolicitudVivaPorTransaccion() {
        Movida m = movida();
        pedir(m, "rev-ojos-4");
        assertThatThrownBy(() -> pedir(m, "rev-ojos-5"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("reverso pendiente");
    }

    @Test
    @DisplayName("rechaza la base una solicitud autorizada por quien la pidio (R-BIL-27)")
    void laBaseExigeOtraPersona() {
        Movida m = movida();
        UUID solicitud = pedir(m, "rev-ojos-6");
        // La solicitud recien creada es la unica pendiente de esta prueba (limpiar() borra las demas).
        String error = rechazaLaBase(
                "UPDATE nucleo_financiero.solicitud_reverso SET autorizada_por = solicitada_por WHERE estado = 'SOLICITADA'");
        assertThat(solicitud).isNotNull();
        assertThat(error).contains("ck_solicitud_reverso_cuatro_ojos");
    }
}
