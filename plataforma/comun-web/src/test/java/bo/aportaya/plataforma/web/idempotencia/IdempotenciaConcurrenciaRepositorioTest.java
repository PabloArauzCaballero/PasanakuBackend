package bo.aportaya.plataforma.web.idempotencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H1.S1.M2 — 50 hilos en paralelo, y los tres escenarios de recuperacion tras una
 * falla (rollback, caida tras reservar, error transitorio). Vive separada de
 * {@link IdempotenciaRepositorioTest} por la regla de las 300 lineas (tamano-archivo),
 * igual que {@code CU10ConcurrenciaTest} vive separada de {@code CU10Test} en
 * nucleo-financiero: aquella fija el comportamiento caso por caso, esta prueba que
 * ninguna reserva se pierda ni se duplique cuando algo sale mal.
 */
class IdempotenciaConcurrenciaRepositorioTest extends BaseIdempotenciaRepositorioTest {

    @Test
    @DisplayName("50 hilos: misma identidad y mismo hash en paralelo, una sola reserva gana")
    void cincuentaHilosUnaReserva() throws InterruptedException {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-unico");
        int hilos = 50;

        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(hilos);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger conflictos = new AtomicInteger();
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                tareas.add(pool.submit(() -> {
                    try {
                        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
                        exitos.incrementAndGet();
                    } catch (RuntimeException e) {
                        conflictos.incrementAndGet();
                    } finally {
                        salida.countDown();
                    }
                }));
            }
            salida.await();
            for (Future<?> tarea : tareas) {
                tarea.get();
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }

        assertThat(exitos.get()).isEqualTo(1);
        assertThat(conflictos.get()).isEqualTo(hilos - 1);
        assertThat(filasCon(clave)).isEqualTo(1);
    }

    @Test
    @DisplayName("rollback: si la transaccion del llamador revierte, no queda reserva y se puede reintentar")
    void rollbackSinReserva() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");

        assertThatThrownBy(() -> dsl.transaction(config -> {
                    var tx = org.jooq.impl.DSL.using(config);
                    idempotencia.exigirNueva(tx, ctx, "CU10-recargar", clave, hash("cuerpo-a"));
                    throw new IllegalStateException("fallo simulado despues de reservar, antes de comitear");
                }))
                .isInstanceOf(IllegalStateException.class);

        assertThat(filasCon(clave)).as("la reserva se fue con el rollback").isEqualTo(0);

        // Como no quedo nada, un reintento tiene que poder reservar de nuevo sin lanzar.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, hash("cuerpo-a"));
        assertThat(filasCon(clave)).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "fallo tras reservar: la reserva sigue ahi; el reintento no duplica, y al completarse se puede leer la respuesta real")
    void falloTrasReservarReintentoPosible() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-a");

        // Reserva, y el caller "se cae" antes de llamar guardarRespuesta.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);

        // Reintento inmediato: no puede insertar una segunda fila ni tratarse como nueva.
        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h))
                .isInstanceOf(IdempotenciaEnProceso.class);
        assertThat(filasCon(clave)).isEqualTo(1);

        // El proceso original (o un worker de recuperacion) termina y guarda la respuesta real.
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 201, "{\"ordenId\": \"recuperada\"}");

        assertThatThrownBy(() -> idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h))
                .isInstanceOf(OperacionRepetida.class)
                .satisfies(e -> assertThat(((OperacionRepetida) e).cuerpo()).contains("recuperada"));
    }

    @Test
    @DisplayName(
            "reintento tras error transitorio: una respuesta 5xx guardada no se repite; se permite un nuevo intento")
    void reintentoTrasErrorTransitorio() {
        UUID usuario = usuarioReal();
        var ctx = ctxDe(usuario);
        var clave = claveNueva("recarga");
        String h = hash("cuerpo-a");

        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
        idempotencia.guardarRespuesta(dsl, ctx, "CU10-recargar", clave, 503, "{\"codigo\": \"AP-INT-01\"}");

        // Un 5xx no es una respuesta que se deba replayar: el reintento tiene que poder reservar de nuevo.
        idempotencia.exigirNueva(dsl, ctx, "CU10-recargar", clave, h);
        assertThat(filasCon(clave)).isEqualTo(1);
    }
}
