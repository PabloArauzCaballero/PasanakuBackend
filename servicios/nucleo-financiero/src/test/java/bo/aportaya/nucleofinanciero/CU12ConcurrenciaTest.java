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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** H1.S3 · Reintentos simultáneos de una misma transferencia. */
class CU12ConcurrenciaTest extends BaseDeBilletera {

    private static final int REINTENTOS = 50;
    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    @Test
    @DisplayName(
            "Dada una transferencia con 50 reintentos simultáneos · Cuando se procesan · Entonces se registra una sola transferencia")
    void cincuentaReintentosCompartenElResultadoOriginal() throws Exception {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID titular = fixtura.usuario();
        UUID origen = fixtura.billetera(titular, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        ContextoSesion ctx = contextoDe(titular);

        CountDownLatch preparadas = new CountDownLatch(REINTENTOS);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(REINTENTOS);
        try {
            List<Future<SalidaTransferencia>> resultados = new ArrayList<>();
            for (int i = 0; i < REINTENTOS; i++) {
                resultados.add(pool.submit(() -> {
                    preparadas.countDown();
                    if (!salida.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Los reintentos no comenzaron juntos.");
                    }
                    return transaccion.execute(t -> transferenciaCU.ejecutar(
                            new EntradaTransferencia(
                                    "tr-idem-cincuenta",
                                    origen,
                                    destino,
                                    Dinero.de("200.00", Moneda.BOB),
                                    "aporte del mes",
                                    Optional.empty(),
                                    Optional.empty()),
                            ctx));
                }));
            }

            assertThat(preparadas.await(10, TimeUnit.SECONDS)).isTrue();
            salida.countDown();

            List<UUID> transacciones = new ArrayList<>();
            for (Future<SalidaTransferencia> resultado : resultados) {
                transacciones.add(resultado.get(30, TimeUnit.SECONDS).transaccionId());
            }

            assertThat(transacciones).hasSize(REINTENTOS).containsOnly(transacciones.getFirst());
            assertThat(contar(
                            "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE iniciada_por = ? AND origen_tipo = ? AND clave_idempotencia = ?",
                            titular,
                            "TRANSFERENCIA_P2P",
                            "tr-idem-cincuenta"))
                    .isEqualTo(1);
            assertThat(contar(
                            "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                            origen))
                    .isEqualTo(800);
        } finally {
            pool.shutdownNow();
        }
    }
}
