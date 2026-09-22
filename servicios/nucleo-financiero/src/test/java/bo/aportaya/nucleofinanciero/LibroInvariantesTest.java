package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.SalidaTransferencia;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H3.S1/H3.S2.M4 del carril PR4-seguridad (Marcelo, nombre de test reservado —
 * Q-05): los 11 escenarios del plan madre §H8.S2, demostrados contra PostgreSQL
 * real. Cada escenario cuadra por transacción: {@code SUM(CASE sentido...) = 0}
 * en {@code movimiento_billetera}, y la suma de {@code saldo_total} del sistema
 * se preserva.
 *
 * <p>El código de producción de {@code nucleo-financiero} NO se toca: este
 * archivo es 100% de prueba.
 */
class LibroInvariantesTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Par(UUID origen, UUID destino, ContextoSesion ctx) {}

    private Par par(String saldoOrigen) {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal(saldoOrigen));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        return new Par(origen, destino, contextoDe(quienPaga));
    }

    private SalidaTransferencia transferir(Par p, String monto, String clave) {
        return transaccion.execute(t -> transferenciaCU.ejecutar(
                new EntradaTransferencia(clave, p.origen(), p.destino(), bob(monto), "prueba", Optional.empty(), Optional.empty()),
                p.ctx()));
    }

    /** El cuadre que TODO escenario tiene que cumplir: nunca positivo, nunca negativo. */
    private int descuadreDe(UUID transaccionId) {
        return contar(
                """
                SELECT COALESCE(SUM(CASE WHEN sentido = 'CREDITO' THEN monto ELSE -monto END), 0)::int
                  FROM nucleo_financiero.movimiento_billetera WHERE transaccion_id = ?
                """,
                transaccionId);
    }

    private int saldoTotalDelSistema() {
        return contar("SELECT COALESCE(SUM(saldo_total),0)::int FROM nucleo_financiero.cuenta_billetera");
    }

    // ------------------------------------------------------------ 1 · transferencia OK --
    @Test
    @DisplayName("escenario 1: transferencia normal — cuadra por transaccion, saldo total del sistema se preserva")
    void transferenciaOk() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();

        SalidaTransferencia salida = transferir(p, "500.00", "inv-1");

        assertThat(descuadreDe(salida.transaccionId())).isZero();
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(500);
        assertThat(
                        contar(
                                "SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?",
                                p.destino()))
                .isEqualTo(500);
    }

    // ------------------------------------------------------- 2 · saldo insuficiente --
    @Test
    @DisplayName("escenario 2: saldo insuficiente — se rechaza, cero efecto, cuadre intacto")
    void saldoInsuficiente() {
        Par p = par("100.00");
        int totalAntes = saldoTotalDelSistema();

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-2")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(100);
    }

    // -------------------------------------------------------- 3 · moneda distinta --
    @Test
    @DisplayName("escenario 3: moneda distinta entre origen y destino — se rechaza, cero efecto")
    void monedaDistinta() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        UUID destinoUsuario = fixtura.usuario();
        UUID destino = fixtura.billetera(destinoUsuario, ESTANDAR, BigDecimal.ZERO);
        dsl.execute("UPDATE nucleo_financiero.cuenta_billetera SET moneda = 'USD' WHERE id = ?", destino);
        int totalAntes = saldoTotalDelSistema();
        Par p = new Par(origen, destino, contextoDe(quienPaga));

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-3")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }

    // -------------------------------------------------------- 4 · cuenta bloqueada --
    @Test
    @DisplayName("escenario 4: cuenta de destino bloqueada — se rechaza, cero efecto")
    void cuentaBloqueada() {
        Par p = par("1000.00");
        // `BLOQUEADA` no es un valor del CHECK (ck_cuenta_billetera_estado): los
        // reales son ACTIVA/BLOQUEADA_AUTORIDAD/CERRADA/CONGELADA/EN_APERTURA/
        // EN_CIERRE/LIMITADA — encontrado corriendo esto contra PostgreSQL real.
        dsl.execute("UPDATE nucleo_financiero.cuenta_billetera SET estado = 'CONGELADA' WHERE id = ?", p.destino());
        int totalAntes = saldoTotalDelSistema();

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-4")).isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }

    // ------------------------------------------------------- 5 · P2P no permitido --
    @Test
    @DisplayName("escenario 5: la politica de la cuenta de origen no permite P2P — se rechaza, cero efecto")
    void p2pNoPermitido() {
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("TRANSFERENCIA", ESTANDAR, "MES", new BigDecimal("100000.00"), null);
        UUID quienPaga = fixtura.usuario();
        UUID origen = fixtura.billetera(quienPaga, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal("1000.00"));
        // La politica de la fixture siempre permite P2P: se apaga a mano, sin
        // tocar la fixture compartida (no es de este carril).
        dsl.execute(
                """
                UPDATE nucleo_financiero.politica_billetera SET permite_transferencia_p2p = false
                 WHERE id = (SELECT politica_billetera_id FROM nucleo_financiero.cuenta_billetera WHERE id = ?)
                """,
                origen);
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        int totalAntes = saldoTotalDelSistema();
        Par p = new Par(origen, destino, contextoDe(quienPaga));

        assertThatThrownBy(() -> transferir(p, "500.00", "inv-5"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("no permite");

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }

    // ------------------------------------------------ 6 · 100 hilos, una cuenta --
    @Test
    @DisplayName("escenario 6: 100 transferencias concurrentes desde la MISMA cuenta — sin saldo negativo, sin perdida, suma preservada")
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
                                        clave, origen, destino, bob("50.00"), "concurrencia", Optional.empty(), Optional.empty()),
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

        assertThat(saldoTotalDelSistema()).as("la suma del sistema no puede cambiar: es un traspaso interno").isEqualTo(totalAntes);
        assertThat(contar("SELECT count(*)::int FROM nucleo_financiero.cuenta_billetera WHERE id = ? AND saldo_total < 0", origen))
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
    @DisplayName("escenario 8: la misma clave, exactamente simultanea — un solo efecto financiero (el contrato de la perdedora es hallazgo para Justin)")
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
        assertThat(exitos[0]).as("al menos una de las dos tiene que haber ganado la carrera").isGreaterThanOrEqualTo(1);
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .as("un solo efecto: 1000 - 400, no 1000 - 800 — esto es lo que NUNCA puede fallar")
                .isEqualTo(600);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                        transaccionGanadora[0]))
                .isEqualTo(1);
    }

    // ------------------------------- 10 · excepcion tras el debito, en contexto --
    /**
     * Escenario 10 del plan madre §H8.S2: una excepción DESPUÉS de que el débito ya
     * ocurrió, dentro del mismo {@code datos.conContexto(...)} de
     * {@code CU12TransferirSaldo.ejecutar}, tiene que revertir TODO — el débito
     * incluido — porque {@code conContexto} corre dentro de la transacción de Spring
     * ({@code @Transactional}) del método, no en una transacción propia.
     *
     * <p>No se fuerza la excepción con un doble ni tocando código de producción: se
     * usa una violación real de {@code fk_transferencia_p2p_grupo_id}
     * (`sql/20_claves/10_billetera_custodia.sql:302-305`) pasando un {@code grupoId}
     * que no existe. Esa comprobación ocurre en
     * {@code TransferenciaRepositorio.registrar}, que corre DESPUÉS de
     * {@code libro.registrar} (el que hace el débito y el crédito) — exactamente el
     * orden que el escenario pide: excepción tras el débito, misma transacción.
     */
    @Test
    @DisplayName("escenario 10: excepcion despues del debito (FK invalida de grupoId) dentro de Datos.conContexto — revierte TODO, ni el debito sobrevive")
    void excepcionTrasElDebitoRevierteTodoDentroDeConContexto() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();
        UUID grupoInexistente = UUID.randomUUID();

        assertThatThrownBy(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "inv-10",
                                p.origen(),
                                p.destino(),
                                bob("500.00"),
                                "prueba",
                                Optional.of(grupoInexistente),
                                Optional.empty()),
                        p.ctx())))
                .as("un grupoId inexistente tiene que violar fk_transferencia_p2p_grupo_id DESPUES del debito")
                .isInstanceOf(RuntimeException.class);

        assertThat(saldoTotalDelSistema()).as("el debito hecho antes de la excepcion tiene que revertirse entero").isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .as("la cuenta de origen queda exactamente como antes: el debito no sobrevive")
                .isEqualTo(1000);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE clave_idempotencia = 'inv-10'"))
                .as("ni la cabecera de la transaccion (escrita por libro.registrar, antes de la FK invalida) sobrevive")
                .isZero();
    }

    // ------------------------------------------------------------- 9 · rollback --
    @Test
    @DisplayName("escenario 9: una transferencia que revierte (rollback explicito) no deja rastro")
    void rollbackNoDejaRastro() {
        Par p = par("1000.00");
        int totalAntes = saldoTotalDelSistema();

        UUID[] transaccionId = new UUID[1];
        transaccion.execute(t -> {
            SalidaTransferencia salida = transferenciaCU.ejecutar(
                    new EntradaTransferencia(
                            "inv-9", p.origen(), p.destino(), bob("500.00"), "revertida", Optional.empty(), Optional.empty()),
                    p.ctx());
            transaccionId[0] = salida.transaccionId();
            t.setRollbackOnly();
            return null;
        });

        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
        assertThat(contar("SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id = ?", p.origen()))
                .isEqualTo(1000);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.transaccion_billetera WHERE id = ?",
                        transaccionId[0]))
                .as("el rollback no deja ni la cabecera de la transaccion")
                .isZero();
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
                                "inv-11-ab-" + n, cuentaA, cuentaB, bob("10.00"), "cruzada", Optional.empty(), Optional.empty()),
                        ctxA))));
                tareas.add(pool.submit(() -> transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "inv-11-ba-" + n, cuentaB, cuentaA, bob("10.00"), "cruzada", Optional.empty(), Optional.empty()),
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

        int deadlocksDespues =
                contar("SELECT deadlocks::int FROM pg_stat_database WHERE datname = current_database()");
        assertThat(deadlocksDespues).as("pg_stat_database.deadlocks no puede haber crecido").isEqualTo(deadlocksAntes);
        assertThat(saldoTotalDelSistema()).isEqualTo(totalAntes);
    }
}
