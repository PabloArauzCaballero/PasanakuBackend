package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H1.S3 · 50 reintentos simultaneos de CU-12 con la MISMA clave de idempotencia sobre
 * la MISMA cuenta: el kill-test de concurrencia. Vive aparte de {@code CU12Test} por la
 * misma razon que separa {@code CU10ConcurrenciaTest} de {@code CU10Test}: aquella fija
 * el comportamiento de una transferencia, esta fija que cincuenta peticiones a la vez
 * nunca produzcan mas de un efecto.
 *
 * <p>El scope del lookup (H1.S1) y el indice unico {@code uq_tx_idem} son la MISMA
 * barrera vista desde dos lados: el lookup evita la mayoria de los duplicados sin
 * pelear por el candado de la cuenta, y el indice atrapa la ventana de carrera que
 * queda entre el SELECT y el INSERT de los hilos que pasaron el lookup a la vez. Por
 * eso cada hilo termina con la MISMA salida que el ganador o con una excepcion —el
 * equivalente de negocio a un 409— y ninguno de los dos casos es un fallo de la prueba.
 */
class CU12ConcurrenciaTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";
    private static final int HILOS = 50;

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName(
            "kill-test H1.S3: 50 hilos, misma clave, misma cuenta · Cuando terminan · Entonces hay una sola transaccion nueva y la suma de saldos del sistema se preserva")
    void cincuentaHilosMismaClave() throws InterruptedException {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        ContextoSesion ctx = contextoDe(quienPaga);
        String clave = "tr-carrera-50";
        int totalAntes = contar("SELECT COALESCE(SUM(saldo_total),0)::int FROM nucleo_financiero.cuenta_billetera");

        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        List<Future<SalidaTransferencia>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < HILOS; i++) {
                futuros.add(pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                clave, origen, destino, bob("100.00"), "carrera", Optional.empty(), Optional.empty()),
                        ctx))));
            }
            int exitos = 0;
            int rechazos = 0;
            for (Future<SalidaTransferencia> f : futuros) {
                try {
                    f.get();
                    exitos++;
                } catch (ExecutionException fallo) {
                    rechazos++;
                }
            }
            // Cada hilo obtuvo la misma salida que el ganador de la carrera, o fue
            // rechazado por el indice unico: nunca las dos cosas produjeron un
            // segundo efecto en el libro.
            assertThat(exitos + rechazos).isEqualTo(HILOS);
            assertThat(exitos).isGreaterThanOrEqualTo(1);
        } finally {
            pool.shutdown();
        }

        // Una sola transaccion nueva con esa clave, sin importar cuantos hilos la
        // pidieron a la vez.
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera
                         WHERE clave_idempotencia = ? AND origen_tipo = 'TRANSFERENCIA_P2P'
                        """,
                        clave))
                .isEqualTo(1);
        // La plata no entra ni sale: cambia de bolsillo, una sola vez.
        assertThat(contar("SELECT COALESCE(SUM(saldo_total),0)::int FROM nucleo_financiero.cuenta_billetera"))
                .isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", origen))
                .isEqualTo(900);
    }
}
