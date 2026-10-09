package bo.aportaya.garantia;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.EntradaCobertura;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Linea;
import bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.Resultado;
import bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.EntradaRecuperacion;
import bo.aportaya.plataforma.dominio.ClaveIdempotencia;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Concurrencia REAL del respaldo: hilos distintos, conexiones distintas, PostgreSQL real.
 *
 * <p>Los hilos arrancan juntos con un cerrojo para que las transacciones se pisen de verdad;
 * un resultado que dependa de quien llego primero se repite en varias rondas.
 */
class CU131ConcurrenciaTest extends BaseDeRespaldo {

    private static final int RONDAS = 5;

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private <T> List<Object> enParalelo(List<Callable<T>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch listos = new CountDownLatch(tareas.size());
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        for (Callable<T> tarea : tareas) {
            futuros.add(pool.submit(() -> {
                listos.countDown();
                largada.await();
                try {
                    return (Object) tarea.call();
                } catch (Exception e) {
                    return e;
                }
            }));
        }
        listos.await();
        largada.countDown();
        List<Object> resultados = new ArrayList<>();
        for (Future<Object> f : futuros) {
            resultados.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        return resultados;
    }

    @Test
    @DisplayName(
            "Dados dos grupos que activan a la vez con capacidad para uno solo · Cuando reservan Bs 4.000 cada uno · Entonces uno obtiene la reserva y el otro recibe AP-CU23-05, sin sobreasignar (cinco rondas)")
    void dosGruposNoSobreasignan() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            fixturaDeRespaldo.capacidad("6000.00");
            BigDecimal antes = fixturaDeRespaldo.comprometido();
            var a = caso();
            var b = caso();

            var resultados =
                    enParalelo(List.<Callable<Object>>of(() -> reservar(a, "4000.00"), () -> reservar(b, "4000.00")));

            long exitos =
                    resultados.stream().filter(r -> !(r instanceof Exception)).count();
            long rechazos = resultados.stream()
                    .filter(r -> r instanceof ErrorDeNegocio e
                            && "AP-CU23-05".equals(e.codigo().valor()))
                    .count();
            assertThat(exitos).as("ronda %d", ronda).isEqualTo(1);
            assertThat(rechazos).as("ronda %d", ronda).isEqualTo(1);
            assertThat(fixturaDeRespaldo.comprometido()).isEqualByComparingTo(antes.add(new BigDecimal("4000.00")));
            assertThat(contar(
                            "SELECT count(*)::int FROM garantia.reserva_respaldo WHERE grupo_id IN (?, ?)",
                            a.escenario().grupoId(),
                            b.escenario().grupoId()))
                    .isEqualTo(1);
        }
    }

    @Test
    @DisplayName(
            "Dados ocho pedidos simultaneos del mismo turno con la misma clave · Cuando se cubre · Entonces hay una sola cobertura, un solo movimiento y todos reciben la misma respuesta")
    void mismoPedidoEnParalelo() throws Exception {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        var pedido = pedidoDelEjemplo(c, "cobertura-" + c.turno());

        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            tareas.add(() -> cubrir(c, pedido));
        }
        var resultados = enParalelo(tareas);

        assertThat(resultados).noneMatch(r -> r instanceof Exception);
        var coberturas = resultados.stream()
                .map(r -> ((bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.SalidaCobertura) r).coberturaId())
                .distinct()
                .toList();
        assertThat(coberturas).hasSize(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM garantia.movimiento_reserva WHERE reserva_respaldo_id = ? AND tipo = 'APLICACION'",
                        reserva.reservaId()))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName(
            "Dos turnos de Bs 800 que pelean por una reserva de Bs 1.000 · Cuando cubren a la vez · Entonces uno se aplica y el otro queda INSUFICIENTE: nunca se gasta mas de lo reservado")
    void dosTurnosPorLaMismaReserva() throws Exception {
        for (int ronda = 0; ronda < RONDAS; ronda++) {
            fixturaDeRespaldo.capacidad("20000.00");
            var c = caso();
            var reserva = reservar(c, "1000.00");
            UUID turno2 = fixturaDeRespaldo.turno(c.escenario());
            var pedido1 = new EntradaCobertura(
                    c.escenario().grupoId(),
                    c.escenario().periodoId(),
                    c.turno(),
                    bob("6000.00"),
                    bob("5200.00"),
                    bob("0.00"),
                    List.of(new Linea(c.obligacionA(), bob("800.00"))),
                    OffsetDateTime.now(),
                    new ClaveIdempotencia("c1-" + c.turno()));
            var pedido2 = new EntradaCobertura(
                    c.escenario().grupoId(),
                    periodoDe(turno2),
                    turno2,
                    bob("6000.00"),
                    bob("5200.00"),
                    bob("0.00"),
                    List.of(new Linea(c.obligacionB(), bob("800.00"))),
                    OffsetDateTime.now(),
                    new ClaveIdempotencia("c2-" + turno2));

            var resultados = enParalelo(List.<Callable<Object>>of(() -> cubrir(c, pedido1), () -> cubrir(c, pedido2)));

            var salidas = resultados.stream()
                    .map(r -> (bo.aportaya.garantia.aplicacion.CU23CubrirConRespaldo.SalidaCobertura) r)
                    .toList();
            assertThat(salidas.stream().filter(s -> s.resultado() == Resultado.APLICADA))
                    .hasSize(1);
            assertThat(salidas.stream().filter(s -> s.resultado() == Resultado.INSUFICIENTE))
                    .hasSize(1);
            assertThat(numero("SELECT monto_aplicado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                    .as("ronda %d", ronda)
                    .isEqualByComparingTo("800.00");
        }
    }

    @Test
    @DisplayName(
            "Dado el mismo pago tardio llegando seis veces a la vez · Cuando se recupera · Entonces se recupera una sola vez")
    void mismoPagoEnParalelo() throws Exception {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        UUID pago = fixturaDeRespaldo.pago(c.obligacionA(), "500.00");

        List<Callable<Object>> tareas = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            tareas.add(() -> transaccion.execute(t ->
                    recuperacionCU.recuperar(new EntradaRecuperacion(pago, c.obligacionA(), bob("500.00")), c.ctx())));
        }
        var resultados = enParalelo(tareas);

        assertThat(resultados).noneMatch(r -> r instanceof Exception);
        assertThat(contar("SELECT count(*)::int FROM garantia.recuperacion_respaldo WHERE pago_id = ?", pago))
                .isEqualTo(1);
        assertThat(numero("SELECT monto_recuperado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "Dos pagos distintos de Bs 500 para la misma obligacion de Bs 500 · Cuando llegan a la vez · Entonces uno recupera y el otro queda AGOTADA: no se cobra dos veces")
    void dosPagosMismaLinea() throws Exception {
        fixturaDeRespaldo.capacidad("20000.00");
        var c = caso();
        var reserva = reservar(c, "8000.00");
        cubrir(c, pedidoDelEjemplo(c, "cobertura-" + c.turno()));
        UUID pago1 = fixturaDeRespaldo.pago(c.obligacionA(), "500.00");
        UUID pago2 = fixturaDeRespaldo.pago(c.obligacionA(), "500.00");

        var resultados = enParalelo(List.<Callable<Object>>of(
                () -> transaccion.execute(t -> recuperacionCU.recuperar(
                        new EntradaRecuperacion(pago1, c.obligacionA(), bob("500.00")), c.ctx())),
                () -> transaccion.execute(t -> recuperacionCU.recuperar(
                        new EntradaRecuperacion(pago2, c.obligacionA(), bob("500.00")), c.ctx()))));

        assertThat(resultados).noneMatch(r -> r instanceof Exception);
        assertThat(numero("SELECT monto_recuperado FROM garantia.reserva_respaldo WHERE id = ?", reserva.reservaId()))
                .isEqualByComparingTo("500.00");
        assertThat(resultados.stream()
                        .map(r -> ((bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.SalidaRecuperacion) r)
                                .resultado()))
                .containsExactlyInAnyOrder(
                        bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.Resultado.RECUPERADA,
                        bo.aportaya.garantia.aplicacion.CU23RecuperarRespaldo.Resultado.AGOTADA);
    }
}
