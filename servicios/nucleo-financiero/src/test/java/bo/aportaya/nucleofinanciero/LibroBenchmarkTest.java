package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * H3.S2.M2 del carril PR4-seguridad (Marcelo, nombre de test reservado — Q-05):
 * benchmark del advisory lock global de la cadena de hash
 * ({@code fn_aud_encadenar_transaccion}, {@code sql/40_reglas/restricciones.sql:56}).
 * Mide, no verifica — por eso {@code @Tag("benchmark")} y excluido de la ejecución
 * automática de {@code integrationTest} (ver
 * {@code buildSrc/aportaya.base.gradle.kts}); se corre a mano con:
 *
 * <pre>{@code
 * ./gradlew :servicios:nucleo-financiero:integrationTest \
 *   --tests '*LibroBenchmarkTest*' -PcorrerBenchmarks
 * }</pre>
 *
 * <p>200 transferencias concurrentes, 3 corridas. Las 6 métricas del encargo:
 * throughput, p50/p95/p99, espera del advisory lock (muestreada de
 * {@code pg_stat_activity.wait_event} durante la corrida), conexiones activas y
 * deadlocks. Resultado completo en {@code evidencia/H3-benchmark-hashchain.txt}
 * (este test lo escribe él mismo, para que la evidencia sea exactamente la salida
 * real y no una transcripción a mano).
 */
class LibroBenchmarkTest extends BaseDeBilletera {

    private static final int TRANSFERENCIAS_POR_CORRIDA = 200;
    private static final int CORRIDAS = 3;
    private static final String ESTANDAR = "ESTANDAR";

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Par(UUID origen, UUID destino, ContextoSesion ctx) {}

    @Test
    @Tag("benchmark")
    // 200 transferencias x 3 corridas, TODAS serializadas por el mismo advisory
    // lock global — eso es justamente lo que este benchmark mide. El timeout por
    // omision del corredor (120s) alcanza para un caso de uso, no para medir un
    // cuello de botella a proposito: 10 minutos de margen.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void doscientasTransferenciasConcurrentesTresCorridas() throws IOException {
        StringBuilder informe = new StringBuilder();
        informe.append("Benchmark del advisory lock global — cadena de hash de transaccion_billetera\n");
        informe.append("sql/40_reglas/restricciones.sql:56 — pg_advisory_xact_lock(hashtext('cadena_transaccion_billetera'))\n");
        informe.append("Generado por LibroBenchmarkTest, %s\n\n".formatted(Instant.now()));
        informe.append(
                "%d transferencias concurrentes x %d corridas, cada una entre un par de cuentas DISTINTO\n\n"
                        .formatted(TRANSFERENCIAS_POR_CORRIDA, CORRIDAS));

        // Una sola vez para las 3 corridas: `ex_limite_vigencia` es una EXCLUSION
        // CONSTRAINT sobre (concepto, nivel, ventana, rango de vigencia) — pedirla
        // de nuevo en cada corrida choca contra la que ya quedo vigente.
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000000.00"), null);

        for (int corrida = 1; corrida <= CORRIDAS; corrida++) {
            informe.append(correr(corrida));
        }

        Path destino = raizDelRepositorio().resolve("docs/auditoria-produccion/evidencia/H3-benchmark-hashchain.txt");
        Files.createDirectories(destino.getParent());
        Files.writeString(destino, informe.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        assertThat(Files.exists(destino)).isTrue();
    }

    private String correr(int numero) {
        List<Par> pares = new ArrayList<>();
        for (int i = 0; i < TRANSFERENCIAS_POR_CORRIDA; i++) {
            UUID quienPaga = fixtura.usuario();
            UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
            fixtura.acreditar(origen, new BigDecimal("10000.00"));
            UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
            pares.add(new Par(origen, destino, contextoDe(quienPaga)));
        }

        int deadlocksAntes = deadlocksActuales();
        long[] latenciasNanos = new long[TRANSFERENCIAS_POR_CORRIDA];
        ConcurrentHashMap<String, Integer> muestrasDeEspera = new ConcurrentHashMap<>();
        AtomicBoolean corriendo = new AtomicBoolean(true);
        int[] conexionesMaximas = {0};

        ScheduledExecutorService muestreador = Executors.newSingleThreadScheduledExecutor();
        muestreador.scheduleAtFixedRate(
                () -> {
                    if (!corriendo.get()) {
                        return;
                    }
                    try (var conexion = BaseDePrueba.conexion();
                            var sentencia = conexion.createStatement()) {
                        var filas = sentencia.executeQuery(
                                """
                                SELECT count(*)::int AS conexiones,
                                       count(*) FILTER (WHERE wait_event = 'advisory')::int AS esperando_lock
                                  FROM pg_stat_activity WHERE datname = current_database()
                                """);
                        if (filas.next()) {
                            conexionesMaximas[0] = Math.max(conexionesMaximas[0], filas.getInt("conexiones"));
                            muestrasDeEspera.merge("esperando_lock_max", filas.getInt("esperando_lock"), Math::max);
                        }
                    } catch (SQLException ignorada) {
                        // Una muestra perdida no invalida el benchmark: es una
                        // observacion de mejor esfuerzo, no una medicion exacta.
                    }
                },
                0,
                50,
                TimeUnit.MILLISECONDS);

        long inicio = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(50);
        long duracionTotalNanos;
        int deadlocksDespues;
        try {
            List<Future<?>> tareas = new ArrayList<>();
            for (int i = 0; i < TRANSFERENCIAS_POR_CORRIDA; i++) {
                Par p = pares.get(i);
                int indice = i;
                tareas.add(pool.submit(() -> {
                    long antes = System.nanoTime();
                    transaccion.execute(t -> transferenciaCU.ejecutar(
                            new EntradaTransferencia(
                                    "bench-" + numero + "-" + indice,
                                    p.origen(),
                                    p.destino(),
                                    bob("100.00"),
                                    "benchmark",
                                    Optional.empty(),
                                    Optional.empty()),
                            p.ctx()));
                    latenciasNanos[indice] = System.nanoTime() - antes;
                }));
            }
            for (Future<?> t : tareas) {
                try {
                    t.get(120, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            long finLatencia = System.nanoTime();
            corriendo.set(false);
            muestreador.shutdown();
            pool.shutdown();
            duracionTotalNanos = finLatencia - inicio;
            deadlocksDespues = deadlocksActuales();
        }

        long[] ordenadas = latenciasNanos.clone();
        Arrays.sort(ordenadas);
        double throughputPorSegundo = TRANSFERENCIAS_POR_CORRIDA / (duracionTotalNanos / 1_000_000_000.0);

        return """
                == Corrida %d ==
                Transferencias:      %d
                Duracion total:      %.3f s
                Throughput:          %.2f transferencias/s
                Latencia p50:        %.2f ms
                Latencia p95:        %.2f ms
                Latencia p99:        %.2f ms
                Conexiones activas (maximo muestreado): %d
                Sesiones esperando el advisory lock (maximo muestreado): %d
                Deadlocks (pg_stat_database) antes/despues: %d / %d
                """
                .formatted(
                        numero,
                        TRANSFERENCIAS_POR_CORRIDA,
                        duracionTotalNanos / 1_000_000_000.0,
                        throughputPorSegundo,
                        percentilMs(ordenadas, 0.50),
                        percentilMs(ordenadas, 0.95),
                        percentilMs(ordenadas, 0.99),
                        conexionesMaximas[0],
                        muestrasDeEspera.getOrDefault("esperando_lock_max", 0),
                        deadlocksAntes,
                        deadlocksDespues)
                + "\n";
    }

    private double percentilMs(long[] ordenadasNanos, double percentil) {
        int indice = (int) Math.ceil(percentil * ordenadasNanos.length) - 1;
        indice = Math.max(0, Math.min(indice, ordenadasNanos.length - 1));
        return ordenadasNanos[indice] / 1_000_000.0;
    }

    private int deadlocksActuales() {
        return contar("SELECT deadlocks::int FROM pg_stat_database WHERE datname = current_database()");
    }

    private Path raizDelRepositorio() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null && !Files.isDirectory(actual.resolve("sql"))) {
            actual = actual.getParent();
        }
        if (actual == null) {
            throw new UncheckedIOException(new IOException("No se encontro la raiz del repositorio"));
        }
        return actual;
    }
}
