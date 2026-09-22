package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.EntradaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaAprobacion;
import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
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
 * H3.S3 · dos aprobadores DISTINTOS, la MISMA orden EN_REVISION, al mismo tiempo.
 *
 * <p>{@code pasarAAutorizadaPorAprobacion}/{@code pasarARechazadaPorAprobacion} son un
 * {@code UPDATE ... WHERE estado = 'EN_REVISION'}: el motor de la base solo deja pasar
 * a un UPDATE concurrente sobre la misma fila, el resto espera y despues ve
 * {@code estado <> 'EN_REVISION'} y no actualiza nada. No hace falta un lock explicito
 * — la fila misma es el lock. Lo que este test comprueba es que ESO es verdad tambien
 * bajo carga real, no solo en la lectura del codigo.
 */
class CU11AprobacionConcurrenciaTest extends BaseDeBilletera {

    private static final int APROBADORES = 20;
    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private UUID ordenEnRevision(String clave) {
        fixtura.tipoDeCambioDeHoy();
        custodia.cumpleEncaje();
        fixtura.limite("RETIRO", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID solicitante = fixtura.usuario();
        UUID cuenta = fixtura.billetera(solicitante, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuenta, new BigDecimal("50000.00"));
        UUID instrumento = custodia.instrumentoDestino(solicitante, true, true, null);
        SalidaRetiro salida = transaccion.execute(t -> retiroCU.solicitar(
                new EntradaRetiro(
                        clave,
                        cuenta,
                        Dinero.de("6000.00", Moneda.BOB),
                        Dinero.de("5.00", Moneda.BOB),
                        instrumento,
                        true,
                        true,
                        true),
                contextoDe(solicitante)));
        assertThat(salida.estado()).isEqualTo("EN_REVISION");
        return salida.ordenRetiroId();
    }

    @Test
    @DisplayName("concurrencia: 20 aprobadores DISTINTOS pelean por la MISMA orden, gana exactamente uno")
    void veinteAprobadoresSimultaneosGanaUnoSolo() throws Exception {
        UUID ordenId = ordenEnRevision("ret-aprob-carrera-1");
        List<ContextoSesion> aprobadores = new ArrayList<>();
        for (int i = 0; i < APROBADORES; i++) {
            aprobadores.add(contextoDe(fixtura.usuario()));
        }

        CountDownLatch preparados = new CountDownLatch(APROBADORES);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(APROBADORES);
        try {
            List<Future<SalidaAprobacion>> resultados = new ArrayList<>();
            for (ContextoSesion aprobador : aprobadores) {
                resultados.add(pool.submit(() -> {
                    preparados.countDown();
                    if (!salida.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Los aprobadores no arrancaron juntos.");
                    }
                    return transaccion.execute(t -> retiroCU.aprobar(ordenId, aprobador));
                }));
            }

            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();

            int ganadores = 0;
            int perdedores = 0;
            for (Future<SalidaAprobacion> resultado : resultados) {
                try {
                    resultado.get(30, TimeUnit.SECONDS);
                    ganadores++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause())
                            .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeNegocio.class)
                            .hasMessageContaining("ya no esta en revision");
                    perdedores++;
                }
            }

            // Exactamente un aprobador se queda con la orden; los otros 19 chocan contra
            // el WHERE estado = 'EN_REVISION' y reciben el error de negocio — nunca una
            // segunda fila AUTORIZADA, nunca un aprobada_por pisado por otro hilo.
            assertThat(ganadores).isEqualTo(1);
            assertThat(perdedores).isEqualTo(APROBADORES - 1);
            assertThat(contar(
                            "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado ="
                                    + " 'AUTORIZADA'",
                            ordenId))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("concurrencia: un aprobador aprueba y otro rechaza a la vez, gana el primero que llega y nunca los dos")
    void aprobarYRechazarALaVezGanaUnoSolo() throws Exception {
        UUID ordenId = ordenEnRevision("ret-aprob-carrera-2");
        ContextoSesion queAprueba = contextoDe(fixtura.usuario());
        ContextoSesion queRechaza = contextoDe(fixtura.usuario());

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<SalidaAprobacion> aprobar = pool.submit(() -> {
                preparados.countDown();
                salida.await(10, TimeUnit.SECONDS);
                return transaccion.execute(t -> retiroCU.aprobar(ordenId, queAprueba));
            });
            Future<SalidaAprobacion> rechazar = pool.submit(() -> {
                preparados.countDown();
                salida.await(10, TimeUnit.SECONDS);
                return transaccion.execute(t -> retiroCU.rechazarRevision(ordenId, queRechaza));
            });

            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();

            int exitos = 0;
            for (Future<SalidaAprobacion> f : List.of(aprobar, rechazar)) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    exitos++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause())
                            .isInstanceOf(bo.aportaya.plataforma.dominio.ErrorDeNegocio.class)
                            .hasMessageContaining("ya no esta en revision");
                }
            }

            // La orden termina en UN estado terminal, no en dos: o AUTORIZADA o
            // RECHAZADA, nunca "las dos cosas a la vez" ni un estado intermedio raro.
            assertThat(exitos).isEqualTo(1);
            assertThat(contar(
                            "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE id = ? AND estado IN"
                                    + " ('AUTORIZADA','RECHAZADA')",
                            ordenId))
                    .isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
