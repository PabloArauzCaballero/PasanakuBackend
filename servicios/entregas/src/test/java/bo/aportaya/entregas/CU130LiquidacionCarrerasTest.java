package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.dominio.ReglasDeOferta;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H9.S1.M6 · Vencimiento, cancelacion y compra COMPITEN en el PUNTO DE LIQUIDACION (titulo dado, pago pendiente).
 *
 * <p>La barrera no es un {@code if}: cada transicion lleva su precondicion y su version en la escritura
 * ({@code CesionRepositorio.avanzar}, {@code OfertaRepositorio.mover}) y lo irreversible (pagarle al vendedor) solo
 * ocurre si la retencion sigue viva, mientras que liberar solo ocurre si el vendedor NO cobro. Resultado: UNA
 * transicion vence, la otra recibe una respuesta coherente, y el dinero queda en un solo lugar.
 *
 * <p>Primero los entrelazados DETERMINISTAS (lo que se puede demostrar sin suerte) y despues la carrera real con
 * hilos, repetida: una carrera que depende de quien llego primero tiene que dar un resultado coherente sea quien
 * sea el que gane. nucleo es un DOBLE ({@link ContratoDeFondosDelComprador}); queda pendiente de la prueba contra
 * el productor real.
 */
class CU130LiquidacionCarrerasTest extends BaseDeCompraDeTurno {

    private static final int RONDAS = 12;

    private int eventos(String tipo, UUID turno) {
        return contar(
                "SELECT count(*)::int FROM entregas.evento_dominio WHERE tipo = ? AND payload->>'turnoId' = ?",
                tipo,
                turno.toString());
    }

    /** Una compra detenida justo en el punto de liquidacion: titulo dado, saldo retenido, vendedor sin cobrar. */
    private record EnElPunto(Caso c, UUID ofertaId, UUID cesionId) {}

    private EnElPunto enElPuntoDeLiquidacion() {
        var c = caso();
        var oferta = publicar(c);
        fondosDoble.fallaAlPagar(1);
        var r = compraCU.comprar(oferta.ofertaId(), "compra-" + c.e().turnoId(), c.ctxComprador());
        assertThat(r.estado()).isEqualTo("TITULO_ASIGNADO");
        assertThat(estadoDeOferta(oferta.ofertaId())).isEqualTo("LIQUIDANDO");
        return new EnElPunto(c, oferta.ofertaId(), r.cesionId());
    }

    private void rechazaCon(Runnable accion, String codigo) {
        rechaza(accion, codigo);
    }

    @Test
    @DisplayName(
            "DETERMINISTA 1 · Dado el punto de liquidacion · Cuando la cancelacion gana (se aborta y se libera) y despues llega el pago · Entonces FALLIDA, el pago no se hace, el comprador recupera todo y la oferta reabre")
    void gana_la_cancelacion() {
        var p = enElPuntoDeLiquidacion();

        var abortada = compraCU.abortar(p.cesionId(), "Se arrepintio", p.c().ctxComprador());
        // El proceso que iba a pagar llega tarde: la retencion ya no existe.
        var tarde = compraCU.reanudar(p.cesionId(), p.c().ctxComprador());

        assertThat(abortada.estado()).isEqualTo("FALLIDA");
        assertThat(tarde.estado()).isEqualTo("FALLIDA");
        assertThat(tarde.liquidada()).isFalse();
        assertThat(fondosDoble.pagosNuevos.get()).isZero();
        assertThat(fondosDoble.disponible(p.c().comprador().usuarioId())).isEqualTo(bob("10000.00"));
        assertThat(fondosDoble.disponible(p.c().vendedor())).isEqualTo(bob("0.00"));
        assertThat(fondosDoble.retenidoVivo().esCero()).isTrue();
        assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("PUBLICADA");
        assertThat(eventos("entregas.cesion_fallida", p.c().e().turnoId())).isEqualTo(1);
        assertThat(eventos("entregas.derecho_cedido", p.c().e().turnoId())).isZero();
    }

    @Test
    @DisplayName(
            "DETERMINISTA 2 · Dado el punto de liquidacion · Cuando nucleo YA pago al vendedor y llega la cancelacion · Entonces la cancelacion se rechaza (no se libera lo ejecutado), la cesion sigue en curso y al reanudar se liquida con ese mismo pago")
    void gana_el_pago() {
        var p = enElPuntoDeLiquidacion();
        var vista = pasosDeCesion.leer(p.cesionId(), p.c().ctxComprador());
        var pago = fondosDoble.pagarAlVendedor(
                vista.cesion().retencionRef(),
                vista.oferta().vendedorUsuarioId(),
                ReglasDeOferta.paraElVendedor(
                        vista.oferta().precio(), vista.oferta().cargos()),
                vista.oferta().cargos(),
                "cesion:" + p.cesionId() + ":pagar");

        rechazaCon(() -> compraCU.abortar(p.cesionId(), "tarde", p.c().ctxComprador()), "AP-CU130-08");
        assertThat(estadoDeCesion(p.cesionId())).isEqualTo("TITULO_ASIGNADO");
        var fin = compraCU.reanudar(p.cesionId(), p.c().ctxComprador());

        assertThat(fin.liquidada()).isTrue();
        assertThat(fondosDoble.pagosNuevos.get()).isEqualTo(1);
        assertThat(fondosDoble.liberacionesNuevas.get()).isZero();
        assertThat(fondosDoble.disponible(p.c().vendedor())).isEqualTo(bob("5450.00"));
        assertThat(dsl.fetchOne("SELECT liquidacion_ref FROM entregas.cesion_derecho WHERE id = ?", p.cesionId())
                        .get(0, UUID.class))
                .isEqualTo(pago.referencia());
        assertThat(eventos("entregas.derecho_cedido", p.c().e().turnoId())).isEqualTo(1);
        assertThat(eventos("entregas.cesion_fallida", p.c().e().turnoId())).isZero();
        // Y una vez liquidada, abortar es AP-CU130-09: no se deshace a ciegas.
        rechazaCon(() -> compraCU.abortar(p.cesionId(), "mas tarde", p.c().ctxComprador()), "AP-CU130-09");
    }

    @Test
    @DisplayName(
            "DETERMINISTA 3 · Dada una compra en LIQUIDANDO · Cuando el vendedor intenta cancelar la oferta y corre el vencimiento aunque su vigencia ya paso · Entonces ninguno la toca y la compra se liquida")
    void ni_cancelar_ni_vencer_tocan_una_compra_en_curso() {
        var p = enElPuntoDeLiquidacion();
        dslFixtura.execute(
                "UPDATE entregas.oferta_turno SET vigente_hasta = now() - interval '1 hour' WHERE id = ?",
                p.ofertaId());

        rechazaCon(() -> ofertasCU.cancelar(p.ofertaId(), p.c().ctxVendedor()), "AP-CU130-05");
        int vencidas = ofertasCU.vencer(p.c().ctxVendedor());

        assertThat(vencidas).isZero();
        assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("LIQUIDANDO");
        var fin = compraCU.reanudar(p.cesionId(), p.c().ctxComprador());
        assertThat(fin.liquidada()).isTrue();
        assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("VENDIDA");
    }

    @Test
    @DisplayName(
            "CARRERA REAL · Dada una compra en el punto de liquidacion · Cuando reanudar (pagar) y abortar (cancelar) corren a la vez, muchas veces · Entonces gana UNA: o el vendedor cobra una vez o el comprador recupera todo, nunca las dos, y el dinero total no cambia")
    void carreraRealPagarContraCancelar() throws Exception {
        int liquidadas = 0;
        int falladas = 0;
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            fondosDoble.reiniciar();
            var p = enElPuntoDeLiquidacion();
            fondosDoble.fallaAlPagar(0);

            enParalelo(List.of(
                    () -> compraCU.reanudar(p.cesionId(), p.c().ctxComprador()),
                    () -> compraCU.abortar(p.cesionId(), "carrera", p.c().ctxComprador())));
            // Quien perdio pudo quedar a mitad (nucleo cobro y la cancelacion se rechazo): un reintento converge.
            if (!List.of("LIQUIDADA", "FALLIDA").contains(estadoDeCesion(p.cesionId()))) {
                compraCU.reanudar(p.cesionId(), p.c().ctxComprador());
            }

            String final_ = estadoDeCesion(p.cesionId());
            UUID turno = p.c().e().turnoId();
            assertThat(fondosDoble.totalDelSistema()).as("ronda %d", ronda).isEqualTo(bob("10000.00"));
            assertThat(fondosDoble.retenidoVivo().esCero())
                    .as("ronda %d", ronda)
                    .isTrue();
            if ("LIQUIDADA".equals(final_)) {
                liquidadas++;
                assertThat(fondosDoble.pagosNuevos.get()).as("ronda %d", ronda).isEqualTo(1);
                assertThat(fondosDoble.liberacionesNuevas.get())
                        .as("ronda %d", ronda)
                        .isZero();
                assertThat(fondosDoble.disponible(p.c().vendedor())).isEqualTo(bob("5450.00"));
                assertThat(fondosDoble.disponible(p.c().comprador().usuarioId()))
                        .isEqualTo(bob("4500.00"));
                assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("VENDIDA");
                assertThat(eventos("entregas.derecho_cedido", turno)).isEqualTo(1);
                assertThat(eventos("entregas.cesion_fallida", turno)).isZero();
            } else {
                falladas++;
                assertThat(final_).as("ronda %d", ronda).isEqualTo("FALLIDA");
                assertThat(fondosDoble.pagosNuevos.get()).as("ronda %d", ronda).isZero();
                assertThat(fondosDoble.liberacionesNuevas.get())
                        .as("ronda %d", ronda)
                        .isEqualTo(1);
                assertThat(fondosDoble.disponible(p.c().vendedor())).isEqualTo(bob("0.00"));
                assertThat(fondosDoble.disponible(p.c().comprador().usuarioId()))
                        .isEqualTo(bob("10000.00"));
                assertThat(estadoDeOferta(p.ofertaId())).isEqualTo("PUBLICADA");
                assertThat(eventos("entregas.derecho_cedido", turno)).isZero();
                assertThat(eventos("entregas.cesion_fallida", turno)).isEqualTo(1);
            }
        }
        assertThat(liquidadas + falladas).isEqualTo(RONDAS);
    }

    private void enParalelo(List<Callable<Object>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch listos = new CountDownLatch(tareas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        for (Callable<Object> t : tareas) {
            futuros.add(pool.submit(() -> {
                listos.countDown();
                largada.await();
                try {
                    return t.call();
                } catch (ErrorDeNegocio e) {
                    // La perdedora recibe una respuesta de negocio coherente (AP-CU130-08/09), no un 500.
                    return e;
                }
            }));
        }
        listos.await();
        largada.countDown();
        for (var f : futuros) {
            f.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
    }
}
