package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU12TransferirSaldo.EntradaTransferencia;
import bo.aportaya.nucleofinanciero.aplicacion.CU13RetenerSaldo.EntradaRetencion;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H2.S1.M2 y H4.S1.M3 · Disponible, retenido y liquidado; y retiro contra transferencia.
 *
 * <p>Todas las pruebas de concurrencia usan conexiones reales distintas contra PostgreSQL: un
 * doble gasto que solo se rechaza con un solo hilo no esta rechazado. Despues de cada carrera se
 * hace el cuadre DESDE EL MAYOR: el saldo total sale de los movimientos y lo retenido de las
 * retenciones vigentes, no de la columna que la propia operacion acaba de escribir.
 */
class CU12DobleGastoTest extends BaseDeBilletera {

    private static final String ESTANDAR = "ESTANDAR";

    @BeforeEach
    void catalogo() {
        EscenarioDeRetiro.catalogo();
    }

    @AfterEach
    void limpiar() {
        fixtura.limpiarBilleteras();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private record Par(UUID usuario, UUID origen, UUID destino, ContextoSesion ctx) {}

    private Par par(String saldo) {
        UUID usuario = fixtura.usuario();
        UUID origen = fixtura.billetera(usuario, ESTANDAR, BigDecimal.ZERO);
        fixtura.acreditar(origen, new BigDecimal(saldo));
        UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
        return new Par(usuario, origen, destino, contextoDe(usuario));
    }

    private void transferir(Par p, String monto, String clave) {
        transaccion.execute(t -> transferenciaCU.ejecutar(
                new EntradaTransferencia(
                        clave, p.origen(), p.destino(), bob(monto), "prueba", Optional.empty(), Optional.empty()),
                p.ctx()));
    }

    private void retener(Par p, String monto) {
        transaccion.execute(t ->
                retencionCU.retener(EntradaRetencion.simple(p.origen(), bob(monto), "COMISION_PENDIENTE"), p.ctx()));
    }

    private int columna(UUID cuenta, String columna) {
        // El nombre de la columna no es un parametro SQL: se elige entre consultas fijas, no se concatena.
        String consulta =
                switch (columna) {
                    case "saldo_disponible" ->
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?";
                    case "saldo_retenido" ->
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id=?";
                    case "saldo_total" -> "SELECT saldo_total::int FROM nucleo_financiero.cuenta_billetera WHERE id=?";
                    default -> throw new IllegalArgumentException("columna no prevista: " + columna);
                };
        return contar(consulta, cuenta);
    }

    /** Cuadre desde el mayor: total = SUM(movimientos); retenido = SUM(retenciones vigentes). */
    private void cuadraDesdeElMayor(UUID cuenta) {
        int delMayor = contar(
                "SELECT COALESCE(SUM(CASE WHEN sentido='CREDITO' THEN monto ELSE -monto END),0)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=?",
                cuenta);
        int retenidoDelMayor = contar(
                "SELECT COALESCE(SUM(monto),0)::int FROM nucleo_financiero.retencion_saldo WHERE cuenta_billetera_id=? AND estado='VIGENTE'",
                cuenta);
        assertThat(columna(cuenta, "saldo_total")).as("total vs movimientos").isEqualTo(delMayor);
        assertThat(columna(cuenta, "saldo_retenido"))
                .as("retenido vs retenciones")
                .isEqualTo(retenidoDelMayor);
        assertThat(columna(cuenta, "saldo_disponible"))
                .isEqualTo(delMayor - retenidoDelMayor)
                .isNotNegative();
    }

    /** Lanza las tareas a la vez, todas desde el mismo disparo, y devuelve cuantas terminaron bien. */
    private int enParalelo(List<Callable<Object>> tareas) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tareas.size());
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Object>> futuros = new ArrayList<>();
        try {
            for (var tarea : tareas) {
                futuros.add(pool.submit(() -> {
                    salida.await();
                    return tarea.call();
                }));
            }
            salida.countDown();
            int exitos = 0;
            for (var futuro : futuros) {
                try {
                    futuro.get();
                    exitos++;
                } catch (java.util.concurrent.ExecutionException rechazada) {
                    assertThat(rechazada.getCause()).isInstanceOf(ErrorDeNegocio.class);
                }
            }
            return exitos;
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName(
            "Dada una billetera con Bs 500 de los cuales Bs 300 están retenidos · Cuando se intenta transferir Bs 250 y después Bs 200 · Entonces solo los Bs 200 disponibles pueden gastarse y los Bs 250 se rechazan · Y los Bs 300 retenidos quedan intactos y todo cuadra desde el mayor")
    void soloSePuedeGastarLoDisponible() {
        Par p = par("500.00");
        retener(p, "300.00");
        assertThat(columna(p.origen(), "saldo_disponible")).isEqualTo(200);
        assertThat(columna(p.origen(), "saldo_retenido")).isEqualTo(300);

        assertThatThrownBy(() -> transferir(p, "250.00", "gasto-1"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("disponible");
        assertThat(columna(p.origen(), "saldo_disponible")).isEqualTo(200);

        transferir(p, "200.00", "gasto-2");

        assertThat(columna(p.origen(), "saldo_disponible")).isZero();
        assertThat(columna(p.origen(), "saldo_retenido")).isEqualTo(300);
        assertThat(columna(p.destino(), "saldo_total")).isEqualTo(200);
        cuadraDesdeElMayor(p.origen());
        cuadraDesdeElMayor(p.destino());
    }

    @Test
    @DisplayName("concurrencia: doble gasto, dos transferencias de Bs 150 contra Bs 200 disponibles, una sola pasa")
    void dobleGastoConcurrenteRechazado() throws Exception {
        for (int ronda = 0; ronda < 5; ronda++) {
            Par p = par("500.00");
            retener(p, "300.00");
            String sufijo = ronda + "-" + UUID.randomUUID();

            int exitos = enParalelo(List.of(
                    () -> {
                        transferir(p, "150.00", "par-a-" + sufijo);
                        return null;
                    },
                    () -> {
                        transferir(p, "150.00", "par-b-" + sufijo);
                        return null;
                    }));

            assertThat(exitos).as("ronda %d", ronda).isEqualTo(1);
            assertThat(columna(p.origen(), "saldo_disponible")).isEqualTo(50);
            assertThat(columna(p.origen(), "saldo_retenido")).isEqualTo(300);
            assertThat(columna(p.destino(), "saldo_total")).isEqualTo(150);
            cuadraDesdeElMayor(p.origen());
            cuadraDesdeElMayor(p.destino());
        }
    }

    @Test
    // Traza de la prueba original: H4.S1.M3
    @DisplayName("concurrencia: retiro y transferencia compiten por el mismo saldo, no exceden el disponible")
    void retiroContraTransferencia() throws Exception {
        int ganoElRetiro = 0;
        int ganoLaTransferencia = 0;
        for (int ronda = 0; ronda < 8; ronda++) {
            var retiro = EscenarioDeRetiro.nuevo("100.00");
            UUID destino = fixtura.billetera(fixtura.usuario(), ESTANDAR, BigDecimal.ZERO);
            String sufijo = ronda + "-" + UUID.randomUUID();

            List<Callable<Object>> tareas = new ArrayList<>();
            List<Boolean> quienPaso = new java.util.concurrent.CopyOnWriteArrayList<>();
            tareas.add(() -> {
                retiro.solicitar("ret-" + sufijo, "80.00", "5.00");
                quienPaso.add(Boolean.TRUE);
                return null;
            });
            tareas.add(() -> {
                transaccion.execute(t -> transferenciaCU.ejecutar(
                        new EntradaTransferencia(
                                "tr-" + sufijo,
                                retiro.cuenta(),
                                destino,
                                bob("80.00"),
                                "prueba",
                                Optional.empty(),
                                Optional.empty()),
                        retiro.ctx()));
                quienPaso.add(Boolean.FALSE);
                return null;
            });

            int exitos = enParalelo(tareas);

            assertThat(exitos).as("ronda %d: exactamente una de las dos", ronda).isEqualTo(1);
            boolean retiroGano = quienPaso.get(0);
            assertThat(columna(retiro.cuenta(), "saldo_disponible")).isEqualTo(20);
            assertThat(columna(retiro.cuenta(), "saldo_retenido")).isEqualTo(retiroGano ? 80 : 0);
            assertThat(columna(destino, "saldo_total")).isEqualTo(retiroGano ? 0 : 80);
            assertThat(contar(
                            "SELECT count(*)::int FROM nucleo_financiero.orden_retiro WHERE cuenta_billetera_id=?",
                            retiro.cuenta()))
                    .isEqualTo(retiroGano ? 1 : 0);
            cuadraDesdeElMayor(retiro.cuenta());
            cuadraDesdeElMayor(destino);
            if (retiroGano) {
                ganoElRetiro++;
            } else {
                ganoLaTransferencia++;
            }
        }
        // Que gane uno u otro lo decide el orden en que PostgreSQL entrega el bloqueo; lo que se
        // fija es que nunca ganan los dos. Se deja constancia de la mezcla observada.
        assertThat(ganoElRetiro + ganoLaTransferencia).isEqualTo(8);
    }
}
