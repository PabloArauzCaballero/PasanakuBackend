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
import bo.aportaya.plataforma.dominio.Traza;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Backoffice carril A · H2.S3 · la respuesta de la segunda aprobacion es idempotente de verdad.
 *
 * <p>Antes, la {@code Idempotency-Key} de {@code aprobarRetiro} llegaba al controlador y se
 * descartaba: el aprobador que perdia la respuesta y reintentaba recibia "ya no esta en revision"
 * por una orden que el mismo acababa de autorizar, y la UI no podia distinguir su propio exito del
 * de otro aprobador. Ahora la clave se guarda en {@code respuesta_idempotente} junto con el recibo,
 * dentro de la misma transaccion que la transicion (patron de {@code CU13RetenerSaldo}).
 */
class CU11AprobacionIdempotenciaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static ContextoSesion operador(UUID usuarioId) {
        return ContextoSesion.de(
                usuarioId, "BACKOFFICE", new Traza(UUID.randomUUID().toString()));
    }

    private SalidaRetiro ordenEnRevision(String clave) {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50000.00"));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        SalidaRetiro salida = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(clave, cuenta, bob("6000.00"), bob("5.00"), instrumento, true, true, true),
                contextoDe(usuario)));
        assertThat(salida.estado()).isEqualTo("EN_REVISION");
        return salida;
    }

    private int eventos(String tipo, UUID ordenId) {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.evento_dominio WHERE tipo = ? AND agregado_id = ?",
                tipo,
                ordenId);
    }

    private SalidaAprobacion autorizar(UUID ordenId, String clave, ContextoSesion ctx) {
        return transaccion.execute(t -> retiroCU.aprobar(ordenId, clave, ctx));
    }

    @Test
    @DisplayName("Dado un aprobador que autorizo una orden y perdio la respuesta · Cuando reintenta con la MISMA clave"
            + " · Entonces recibe el mismo recibo, y la orden, el evento y la metrica ocurrieron una sola vez")
    void reintentoConLaMismaClaveDevuelveElRecibo() {
        SalidaRetiro orden = ordenEnRevision("idem-aprob-1");
        ContextoSesion aprobador = operador(fixtura.usuario());
        double antes = metricas.counter("withdrawal_approved_total").count();

        SalidaAprobacion primera = autorizar(orden.ordenRetiroId(), "clave-aprob-1", aprobador);
        SalidaAprobacion repetida = autorizar(orden.ordenRetiroId(), "clave-aprob-1", aprobador);

        assertThat(repetida).isEqualTo(primera);
        assertThat(primera.estado()).isEqualTo("AUTORIZADA");
        assertThat(eventos("nucleo_financiero.retiro_autorizado", orden.ordenRetiroId()))
                .isEqualTo(1);
        assertThat(metricas.counter("withdrawal_approved_total").count() - antes)
                .isEqualTo(1.0);
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM nucleo_financiero.respuesta_idempotente
                        WHERE usuario_id = ? AND operacion = 'aprobarRetiro' AND clave_idempotencia = ?
                        """,
                        aprobador.usuarioId(),
                        "clave-aprob-1"))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un rechazo ya registrado · Cuando se repite con la misma clave · Entonces devuelve el mismo"
            + " recibo y la retencion se libero una sola vez")
    void rechazoRepetidoNoLiberaDosVeces() {
        SalidaRetiro orden = ordenEnRevision("idem-rech-1");
        ContextoSesion aprobador = operador(fixtura.usuario());

        SalidaAprobacion primera = transaccion.execute(t -> retiroCU.rechazarRevision(
                orden.ordenRetiroId(), "Documentacion insuficiente", "clave-rech-1", aprobador));
        SalidaAprobacion repetida = transaccion.execute(t -> retiroCU.rechazarRevision(
                orden.ordenRetiroId(), "Documentacion insuficiente", "clave-rech-1", aprobador));

        assertThat(repetida).isEqualTo(primera);
        assertThat(primera.estado()).isEqualTo("RECHAZADA");
        assertThat(eventos("nucleo_financiero.retiro_rechazado", orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza la misma clave con OTRO desenlace: AP-CU11-13, la clave no se recicla para otra decision")
    void mismaClaveOtroDesenlace() {
        SalidaRetiro orden = ordenEnRevision("idem-aprob-2");
        ContextoSesion aprobador = operador(fixtura.usuario());
        autorizar(orden.ordenRetiroId(), "clave-aprob-2", aprobador);

        assertThatThrownBy(() -> transaccion.execute(
                        t -> retiroCU.rechazarRevision(orden.ordenRetiroId(), null, "clave-aprob-2", aprobador)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU11-13"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado = 'AUTORIZADA'",
                        orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza OTRA clave sobre una orden ya resuelta: sigue siendo AP-CU11-11 (otra operacion, no un"
            + " reintento)")
    void otraClaveSobreOrdenResuelta() {
        SalidaRetiro orden = ordenEnRevision("idem-aprob-3");
        ContextoSesion aprobador = operador(fixtura.usuario());
        autorizar(orden.ordenRetiroId(), "clave-aprob-3", aprobador);

        assertThatThrownBy(() -> autorizar(orden.ordenRetiroId(), "clave-aprob-3-bis", aprobador))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU11-11"));
        assertThat(eventos("nucleo_financiero.retiro_autorizado", orden.ordenRetiroId()))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Dado un solicitante que intenta aprobar su propia orden con una clave · Cuando envia la aprobacion"
            + " · Entonces se rechaza por auto-aprobacion y la clave no queda reservada")
    void autoAprobacionConClave() {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuario = fixtura.usuario();
        UUID cuenta = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50000.00"));
        UUID instrumento = custodia.instrumentoDestino(usuario, true, true, null);
        SalidaRetiro orden = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro("idem-auto", cuenta, bob("6000.00"), bob("5.00"), instrumento, true, true, true),
                contextoDe(usuario)));

        assertThatThrownBy(() -> autorizar(orden.ordenRetiroId(), "clave-auto", operador(usuario)))
                .isInstanceOf(ErrorDeNegocio.class)
                .satisfies(
                        e -> assertThat(((ErrorDeNegocio) e).codigo().valor()).isEqualTo("AP-CU11-10"));
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.respuesta_idempotente WHERE clave_idempotencia = ?",
                        "clave-auto"))
                .isZero();
    }

    @Test
    @DisplayName("concurrencia: dos envios simultaneos con la misma clave producen UNA transicion y el mismo recibo")
    void dosEnviosSimultaneos() throws Exception {
        SalidaRetiro orden = ordenEnRevision("idem-conc");
        ContextoSesion aprobador = operador(fixtura.usuario());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<SalidaAprobacion>> envios = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                envios.add(pool.submit(() -> {
                    largada.await();
                    return autorizar(orden.ordenRetiroId(), "clave-conc", aprobador);
                }));
            }
            largada.countDown();
            SalidaAprobacion a = envios.get(0).get(30, TimeUnit.SECONDS);
            SalidaAprobacion b = envios.get(1).get(30, TimeUnit.SECONDS);
            assertThat(a).isEqualTo(b);
        } finally {
            pool.shutdownNow();
        }
        assertThat(eventos("nucleo_financiero.retiro_autorizado", orden.ordenRetiroId()))
                .isEqualTo(1);
    }
}
