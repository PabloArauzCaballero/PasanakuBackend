package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.ConsumoDeAsientos.Resultado;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H2.S1.M4 · reinicio y duplicado concurrente del consumo de asientos (el productor es un doble de contrato). */
class CU24EventosReinicioTest extends BaseDeEventosDeAsientos {

    @Test
    @DisplayName("reintento: la caida antes del commit no deja marca ni asiento, y el reproceso asienta una sola vez")
    void caidaAntesDelCommit() {
        UUID origen = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "garantia.respaldo_reservado",
                carga("AJUSTE", origen, "CAJA_EMPRESA", "RESERVA_RESPALDO", "300.00", "BOB"));
        // El proceso muere despues de asentar y antes de confirmar: la transaccion entera se pierde.
        transaccion.execute(t -> {
            consumo.procesar(e);
            t.setRollbackOnly();
            return null;
        });
        assertThat(asientos(origen)).isZero();
        assertThat(marcas(e.eventoId())).isZero();

        assertThat(procesar(e).resultado()).isEqualTo(Resultado.REGISTRADO);
        // Y la caida DESPUES del commit: el broker reentrega y no pasa nada mas.
        assertThat(procesar(e).resultado()).isEqualTo(Resultado.DUPLICADO);
        assertThat(asientos(origen)).isEqualTo(1);
        assertThat(((Number) dsl.fetchOne(
                                        "SELECT count(*) FROM nucleo_financiero.evento_dominio WHERE tipo = 'nucleo_financiero.asiento_registrado' AND payload ->> 'origenId' = ?",
                                        origen.toString())
                                .get(0))
                        .intValue())
                .as("el outbox emitio un solo asiento_registrado")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("evento duplicado: ocho consumidores a la vez del mismo evento asientan una sola vez")
    void concurrencia() throws Exception {
        UUID origen = UUID.randomUUID();
        var e = evento(
                UUID.randomUUID(),
                "garantia.respaldo_reservado",
                carga("AJUSTE", origen, "CAJA_EMPRESA", "RESERVA_RESPALDO", "75.00", "BOB"));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        long registrados = 0;
        try {
            var tareas = new java.util.ArrayList<Future<Resultado>>();
            for (int i = 0; i < 8; i++) {
                tareas.add(pool.submit(() -> procesar(e).resultado()));
            }
            for (var t : tareas) {
                registrados += t.get() == Resultado.REGISTRADO ? 1 : 0;
            }
        } finally {
            pool.shutdown();
        }
        assertThat(registrados).isEqualTo(1);
        assertThat(asientos(origen)).isEqualTo(1);
    }
}
