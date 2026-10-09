package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.aplicacion.VistaOrden;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H10.S1.M2 · concurrencia REAL sobre la orden de inversion: doce hilos con la misma clave y
 * dos ordenes distintas que juntas superan el saldo. PostgreSQL real; libro y aliado dobles.
 */
class CU121ConcurrenciaTest extends BaseDeInversiones {

    private int ordenesDelTitular() {
        return contar("select count(*)::int from inversiones.orden_inversion where usuario_id = ?", usuario);
    }

    @Test
    @DisplayName(
            "concurrencia: doce pedidos simultaneos con la misma clave dejan UNA orden, UNA posicion y el libro se debita UNA vez")
    void concurrenciaRealMismaClave() throws Exception {
        conSaldo("5000.00");
        var entrada = entrada(PRODUCTO_DPF, "1000.00", UUID.randomUUID());
        int hilos = 12;
        var pool = Executors.newFixedThreadPool(hilos);
        var salida = new CountDownLatch(1);
        List<Future<VistaOrden>> futuros = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                return cu121.ordenar(entrada, contextoDe(usuario));
            }));
        }
        salida.countDown();
        List<VistaOrden> vistas = new ArrayList<>();
        for (Future<VistaOrden> f : futuros) {
            vistas.add(f.get());
        }
        pool.shutdown();

        assertThat(vistas.stream().map(VistaOrden::ordenId).distinct()).hasSize(1);
        assertThat(ordenesDelTitular()).isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.posicion_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        assertThat(contar("select count(*)::int from inversiones.comprobante_inversion where usuario_id = ?", usuario))
                .isEqualTo(1);
        UUID orden = vistas.get(0).ordenId();
        assertThat(libro.asientos()).hasSize(1);
        assertThat(libro.total(cuenta)).isEqualByComparingTo("4000.00");
        assertThat(libro.clavesAplicadas()).contains("RETENER:" + orden, "DEBITAR:" + orden);
    }

    @Test
    @DisplayName(
            "concurrencia: dos ordenes DISTINTAS que juntas superan el saldo, una se concreta y la otra se rechaza por saldo, y el libro no queda en negativo")
    void concurrenciaRealSaldoJusto() throws Exception {
        conSaldo("5000.00");
        var uno = entrada(PRODUCTO_DPF, "3000.00", UUID.randomUUID());
        var otra = entrada(PRODUCTO_DPF, "3000.00", UUID.randomUUID());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        var salida = new CountDownLatch(1);
        Future<VistaOrden> a = pool.submit(() -> {
            salida.await();
            return cu121.ordenar(uno, contextoDe(usuario));
        });
        Future<VistaOrden> b = pool.submit(() -> {
            salida.await();
            return cu121.ordenar(otra, contextoDe(usuario));
        });
        salida.countDown();
        List<String> estados = List.of(a.get().estado(), b.get().estado());
        pool.shutdown();

        assertThat(estados).containsExactlyInAnyOrder("CONFIRMADA", "RECHAZADA");
        assertThat(libro.total(cuenta)).isEqualByComparingTo("2000.00");
        assertThat(libro.disponible(cuenta)).isEqualByComparingTo("2000.00");
        assertThat(libro.retenido(cuenta)).isEqualByComparingTo("0.00");
        assertThat(contar(
                        "select count(*)::int from inversiones.orden_inversion where usuario_id = ? and motivo_rechazo = 'SALDO_INSUFICIENTE'",
                        usuario))
                .isEqualTo(1);
    }
}
