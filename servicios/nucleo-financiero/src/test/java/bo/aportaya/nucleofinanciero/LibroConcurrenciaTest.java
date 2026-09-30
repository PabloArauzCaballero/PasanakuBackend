package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LibroConcurrenciaTest extends BaseDeLibroInvariantes {
    // ------------------------------------------------ 6 · 100 hilos, una cuenta --
    @Test
    @DisplayName(
            "escenario 6: 100 transferencias concurrentes desde la MISMA cuenta — sin saldo negativo, sin perdida, suma preservada")
    void cienHilosSobreUnaCuenta() throws InterruptedException {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("1000000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("10000.00"));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        ContextoSesion ctx = contextoDe(quienPaga);
        int totalAntes = saldoTotalDelSistema();

        int hilos = 100;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        AtomicInteger exitosos = new AtomicInteger();
        try {
            List<Future<?>> tareas = new java.util.ArrayList<>();
            for (int i = 0; i < hilos; i++) {
                String clave = "inv-6-" + i;
                tareas.add(pool.submit(() -> {
                    try {
                        transaccion.execute(t -> transferenciaCU.ejecutar(
                                new EntradaTransferencia(
                                        clave,
                                        origen,
                                        destino,
                                        bob("50.00"),
                                        "concurrencia",
                                        Optional.empty(),
                                        Optional.empty()),
                                ctx));
                        exitosos.incrementAndGet();
                    } catch (RuntimeException ignorada) {
                        // Algunas pueden perder la carrera del limite optimista o del
                        // limite operativo: lo que importa es que NUNCA quede
                        // descuadrado ni en negativo, no que las 100 ganen.
                    }
                }));
            }
            for (Future<?> t : tareas) {
                try {
                    t.get(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            pool.shutdown();
        }

        assertThat(saldoTotalDelSistema())
                .as("la suma del sistema no puede cambiar: es un traspaso interno")
                .isEqualTo(totalAntes);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.cuenta_billetera WHERE id = ? AND saldo_total < 0",
                        origen))
                .as("saldo negativo detectado")
                .isZero();
        int esperado = 10000 - 50 * exitosos.get();
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", origen))
                .as("cada exito debita exactamente 50, ni de mas ni de menos (sin perdida ni duplicacion)")
                .isEqualTo(esperado);
    }

    // ---------------------------------------------- 7 · dos opuestas simultaneas --
    @Test
    @DisplayName("escenario 7: A->B y B->A al mismo tiempo — las dos cuadran, la suma total se preserva")
    void dosOpuestasSimultaneas() throws InterruptedException {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID usuarioA = fixtura.usuario();
        UUID cuentaA = fixtura.billetera(usuarioA, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuentaA, new BigDecimal("1000.00"));
        UUID usuarioB = fixtura.usuario();
        UUID cuentaB = fixtura.billetera(usuarioB, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuentaB, new BigDecimal("1000.00"));
        ContextoSesion ctxA = contextoDe(usuarioA);
        ContextoSesion ctxB = contextoDe(usuarioB);
        int totalAntes = saldoTotalDelSistema();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> aHaciaB = pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                    new EntradaTransferencia(
                            "inv-7-ab", cuentaA, cuentaB, bob("300.00"), "cruzada", Optional.empty(), Optional.empty()),
                    ctxA)));
            Future<?> bHaciaA = pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                    new EntradaTransferencia(
                            "inv-7-ba", cuentaB, cuentaA, bob("200.00"), "cruzada", Optional.empty(), Optional.empty()),
                    ctxB)));
            aHaciaB.get(30, TimeUnit.SECONDS);
            bHaciaA.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuentaA))
                .isEqualTo(900);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", cuentaB))
                .isEqualTo(1100);
    }

    // --------------------------------------------------- 8 · replay idempotente --
    /**
     * <b>Hallazgo real, encontrado corriendo esto contra PostgreSQL real</b> (H3.S1.M3
     * del carril PR4-seguridad — "si el scope corregido por Justin ya está en dev; si
     * no, la aserción de replay queda A MEDIAS declarada", exactamente este caso):
     * cuando las DOS peticiones con la misma clave llegan de verdad simultáneas (no
     * una despues de la otra, que es el caso que {@code CU12Test} ya prueba y pasa),
     * {@code CU12TransferirSaldo.ejecutar} tiene una ventana TOCTOU real: el
     * {@code SELECT} de {@code porClaveIdempotencia} de las dos transacciones corre
     * ANTES de que cualquiera de las dos haya hecho commit, así que ninguna ve la
     * clave de la otra — y la perdedora de la carrera de {@code INSERT} recibe
     * {@code IntegrityConstraintViolationException} (violación de
     * {@code uq_tx_idem}) en vez de la respuesta idempotente de la ganadora.
     *
     * <p>El invariante financiero real (un solo efecto, nunca doble débito) SIGUE
     * cumpliéndose — eso es lo que este test verifica — pero el CONTRATO de la API
     * (¿la perdedora ve un error 500/`IllegalStateException`, o la respuesta
     * idempotente?) no está resuelto. Es de {@code nucleo-financiero}, fuera de este
     * carril (Justin, PR2): no se edita el código de producción; queda como hallazgo.
     */
    @Test
    @DisplayName(
            "escenario 8: la misma clave, exactamente simultanea — un solo efecto financiero (el contrato de la perdedora es hallazgo para Justin)")
    void replayIdempotente() throws InterruptedException {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        UUID[] transaccionGanadora = new UUID[1];
        int[] exitos = {0};
        try {
            Future<SalidaTransferencia> f1 = pool.submit(() -> transferir(p, "400.00", "inv-8-replay"));
            Future<SalidaTransferencia> f2 = pool.submit(() -> transferir(p, "400.00", "inv-8-replay"));
            for (Future<SalidaTransferencia> f : List.of(f1, f2)) {
                try {
                    transaccionGanadora[0] = f.get(30, TimeUnit.SECONDS).transaccionId();
                    exitos[0]++;
                } catch (java.util.concurrent.ExecutionException carreraPerdida) {
                    // Ver el hallazgo en el javadoc: es la ventana TOCTOU real de
                    // CU12TransferirSaldo bajo concurrencia exacta, no un fallo de
                    // este test.
                } catch (java.util.concurrent.TimeoutException tiempoAgotado) {
                    throw new IllegalStateException(tiempoAgotado);
                }
            }
        } finally {
            pool.shutdown();
        }

        // El invariante que importa: pase lo que pase con la respuesta HTTP de la
        // perdedora, el EFECTO FINANCIERO es uno solo, nunca dos.
        assertThat(exitos[0])
                .as("al menos una de las dos tiene que haber ganado la carrera")
                .isGreaterThanOrEqualTo(1);
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .as("un solo efecto: 1000 - 400, no 1000 - 800 — esto es lo que NUNCA puede fallar")
                .isEqualTo(600);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                        transaccionGanadora[0]))
                .isEqualTo(1);
    }

    // ------------------------------------------- 11 · cruzadas ×50, sin deadlock --
    @Test
    @DisplayName("escenario 11: 50 transferencias cruzadas A->B y B->A — sin incremento de pg_stat_database.deadlocks")
    void cruzadasSinDeadlock() throws InterruptedException {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("1000000.00"), null);
        UUID usuarioA = fixtura.usuario();
        UUID cuentaA = fixtura.billetera(usuarioA, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuentaA, new BigDecimal("100000.00"));
        UUID usuarioB = fixtura.usuario();
        UUID cuentaB = fixtura.billetera(usuarioB, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(cuentaB, new BigDecimal("100000.00"));
        ContextoSesion ctxA = contextoDe(usuarioA);
        ContextoSesion ctxB = contextoDe(usuarioB);

        int deadlocksAntes = contar("SELECT deadlocks::int FROM pg_stat_database WHERE datname = current_database()");
        int totalAntes = saldoTotalDelSistema();

        int repeticiones = 50;
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> tareas = new java.util.ArrayList<>();
            for (int i = 0; i < repeticiones; i++) {
                int n = i;
                tareas.add(pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "inv-11-ab-" + n,
                                cuentaA,
                                cuentaB,
                                bob("10.00"),
                                "cruzada",
                                Optional.empty(),
                                Optional.empty()),
                        ctxA))));
                tareas.add(pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "inv-11-ba-" + n,
                                cuentaB,
                                cuentaA,
                                bob("10.00"),
                                "cruzada",
                                Optional.empty(),
                                Optional.empty()),
                        ctxB))));
            }
            for (Future<?> t : tareas) {
                try {
                    t.get(60, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            pool.shutdown();
        }

        int deadlocksDespues = contar("SELECT deadlocks::int FROM pg_stat_database WHERE datname = current_database()");
        assertThat(deadlocksDespues)
                .as("pg_stat_database.deadlocks no puede haber crecido")
                .isEqualTo(deadlocksAntes);
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }
}
