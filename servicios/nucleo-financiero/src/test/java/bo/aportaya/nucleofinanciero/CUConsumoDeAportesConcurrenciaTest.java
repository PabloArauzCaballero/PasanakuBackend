package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.Resultado;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Carril I · las carreras del consumo de {@code aportes.aporte_cobrado}: dos replicas, un solo efecto; dos aportes, un solo gasto. */
class CUConsumoDeAportesConcurrenciaTest extends BaseDeAportes {

    @Test
    @DisplayName("concurrencia: dos replicas que entregan el MISMO evento a la vez mueven la plata una sola vez")
    void concurrenciaDelMismoEvento() throws Exception {
        var a = aporte("1000.00");
        var e = cobrado(a, "500.00");
        var resultados =
                enParalelo(() -> procesar(e).resultado(), () -> procesar(e).resultado());

        assertThat(resultados).containsExactlyInAnyOrder(Resultado.APLICADO, Resultado.DUPLICADO);
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(asientosDeAportes(a.grupo())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("500.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName(
            "concurrencia: dos aportes DISTINTOS de Bs 500 con Bs 600 en la billetera no la dejan en negativo: uno se"
                    + " aplica y el otro se rechaza por saldo (compensable)")
    void concurrenciaNoGastaDosVeces() throws Exception {
        var a = aporte("600.00");
        var uno = cobrado(a, "500.00");
        var otro = cobrado(a, "500.00");

        var resultados = enParalelo(() -> intentar(uno), () -> intentar(otro));

        assertThat(resultados).containsExactlyInAnyOrder("APLICADO", "RECHAZADO_POR_SALDO");
        assertThat(transacciones(a.obligacion())).isEqualTo(1);
        assertThat(saldoDelLibro(a.billetera())).isEqualByComparingTo("100.00");
        assertThat(saldoDelLibro(a.billeteraDelGrupo())).isEqualByComparingTo("500.00");
        // El rechazado no dejo efectos en el libro y publico exactamente UN aviso para compensar.
        assertThat(contar(
                        """
                        SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.aporte_no_aplicado' AND agregado_id = ?
                        """,
                        a.obligacion()))
                .isEqualTo(1);
    }

    private String intentar(bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAportes.AporteCobrado e) {
        try {
            return procesar(e).resultado().name();
        } catch (ErrorDeNegocio rechazo) {
            return rechazo.codigo().valor();
        }
    }

    @SafeVarargs
    private <T> List<T> enParalelo(java.util.concurrent.Callable<T>... tareas) throws Exception {
        var hilos = Executors.newFixedThreadPool(tareas.length);
        try {
            var largada = new CountDownLatch(1);
            var futuros = new java.util.ArrayList<java.util.concurrent.Future<T>>();
            for (var tarea : tareas) {
                futuros.add(hilos.submit(() -> {
                    largada.await();
                    return tarea.call();
                }));
            }
            largada.countDown();
            var resultados = new java.util.ArrayList<T>();
            for (var f : futuros) {
                resultados.add(f.get(60, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            hilos.shutdownNow();
        }
    }
}
