package bo.aportaya.entregas;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.entregas.aplicacion.CU22EntregarPozoCompleto.Entrada;
import bo.aportaya.entregas.aplicacion.RegistroDelFondeo;
import bo.aportaya.entregas.dominio.puertos.RecaudoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.time.LocalDate;
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

/** Concurrencia REAL del fondeo: varios hilos, conexiones distintas, PostgreSQL real. */
class CU133PozoCompletoConcurrenciaTest extends BaseDeEntregas {

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName(
            "Dados ocho pedidos simultaneos del mismo turno con la misma clave · Cuando se fondea · Entonces hay UNA entrega, UN fondeo, UNA cobertura y exactamente un pedido es el nuevo")
    void ochoPedidosDelMismoTurno() throws Exception {
        UUID usuario = fixtura.usuario();
        var e = fixtura.escenario(usuario);
        var ctx = contextoDe(usuario);
        aportesDoble.responde(new RecaudoDelPozo.Recaudo(
                e.periodoId(), e.grupoId(), OffsetDateTime.now(), bob("6000.00"), bob("5000.00"), bob("0.00")));
        garantiaDoble.conDisponible(bob("8000.00"));
        var pedido = new Entrada(
                e.grupoId(),
                e.periodoId(),
                e.turnoId(),
                e.cupoId(),
                e.participanteId(),
                "BILLETERA_MOVIL",
                LocalDate.now(),
                "fondeo-concurrente-" + e.turnoId());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch listos = new CountDownLatch(8);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Callable<Object> tarea = () -> {
                listos.countDown();
                largada.await();
                try {
                    return pozoCU.fondear(pedido, ctx);
                } catch (Exception ex) {
                    return ex;
                }
            };
            futuros.add(pool.submit(tarea));
        }
        listos.await();
        largada.countDown();
        List<Object> resultados = new ArrayList<>();
        for (var f : futuros) {
            resultados.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(resultados).noneMatch(r -> r instanceof Exception);
        var salidas = resultados.stream().map(r -> (RegistroDelFondeo.Salida) r).toList();
        assertThat(salidas.stream().map(RegistroDelFondeo.Salida::entregaId).distinct())
                .hasSize(1);
        assertThat(salidas.stream().filter(RegistroDelFondeo.Salida::esNuevo)).hasSize(1);
        assertThat(contar("SELECT count(*)::int FROM entregas.entrega_fondo WHERE turno_id = ?", e.turnoId()))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM entregas.fondeo_entrega WHERE turno_id = ?", e.turnoId()))
                .isEqualTo(1);
        assertThat(garantiaDoble.coberturasNuevas.get()).isEqualTo(1);
    }
}
