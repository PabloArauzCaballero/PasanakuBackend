package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.aplicacion.CU19ReembolsarPago.EntradaReembolso;
import bo.aportaya.aportes.aplicacion.CU21CobrarAporte.EntradaCobro;
import bo.aportaya.aportes.infraestructura.PagoRepositorio;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * F1.L1.M08 (ANF-DATOS-001) · R-BIL-30: los hechos de dinero de {@code aportes} no se editan ni se borran, contra
 * PostgreSQL real. Dos capas: el permiso del rol de la aplicacion y el disparador R-AUD-01. El estado cambia agregando
 * una fila de historial, y desde cada estado se sale una sola vez.
 */
class AppendOnlyTest extends BaseDeAportes {

    private final PagoRepositorio pagos = new PagoRepositorio();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private UUID pagoAcreditado(UUID usuario) {
        var obligacion = fixtura.obligacion(usuario, "100.00", 10);
        return transaccion
                .execute(t -> cobroCU.acreditar(
                        new EntradaCobro(
                                "inm-" + UUID.randomUUID(),
                                obligacion.id(),
                                Dinero.de("100.00", Moneda.BOB),
                                Dinero.de("0.00", Moneda.BOB),
                                "BILLETERA_MOVIL",
                                "ref-" + UUID.randomUUID(),
                                Optional.empty(),
                                false,
                                true),
                        contextoDe(usuario)))
                .pagoId();
    }

    private boolean reversar(UUID pago) {
        Boolean hecho = transaccion.execute(t -> pagos.reversar(dsl, pago));
        return Boolean.TRUE.equals(hecho);
    }

    private String estado(UUID pago) {
        return pagos.ver(dsl, pago).orElseThrow().estado();
    }

    @ParameterizedTest(name = "svc_aportes no puede UPDATE ni DELETE aportes.{0}")
    @ValueSource(strings = {"pago", "reembolso", "historial_estado_pago", "historial_estado_reembolso"})
    @DisplayName("rechaza la base que el rol de la aplicacion edite o borre un hecho de dinero")
    void rechazaElRolDeLaAplicacion(String tabla) {
        for (String privilegio : new String[] {"UPDATE", "DELETE"}) {
            assertThat(contar(
                            "SELECT CASE WHEN has_table_privilege('svc_aportes', ?, ?) THEN 1 ELSE 0 END",
                            "aportes." + tabla,
                            privilegio))
                    .as("svc_aportes puede %s aportes.%s", privilegio, tabla)
                    .isZero();
        }
    }

    @Test
    @DisplayName("rechaza la base un UPDATE del estado de un pago aun con el rol de la aplicacion (permission denied)")
    void rechazaUpdateConElRol() {
        UUID pago = pagoAcreditado(fixtura.usuario());

        assertThatThrownBy(() -> transaccion.execute(t -> {
                    dsl.execute("SET LOCAL ROLE svc_aportes");
                    return dsl.execute("UPDATE aportes.pago SET estado = 'REVERSADO' WHERE id = ?", pago);
                }))
                .hasMessageContaining("permission denied");
        assertThat(estado(pago)).isEqualTo("ACREDITADO");
    }

    @Test
    @DisplayName("rechaza el disparador R-AUD-01 editar o borrar un pago aun con un rol que tenga el permiso")
    void rechazaElDisparador() {
        UUID pago = pagoAcreditado(fixtura.usuario());

        assertThatThrownBy(() -> transaccion.execute(
                        t -> dsl.execute("UPDATE aportes.pago SET estado = 'REVERSADO' WHERE id = ?", pago)))
                .hasMessageContaining("R-AUD-01");
        assertThatThrownBy(() -> transaccion.execute(t -> dsl.execute("DELETE FROM aportes.pago WHERE id = ?", pago)))
                .hasMessageContaining("R-AUD-01");
    }

    @Test
    @DisplayName(
            "cuadre: reversar agrega una fila de historial, el estado vigente es REVERSADO y la segunda vez no hace nada")
    void reversarEsUnaFilaNueva() {
        UUID pago = pagoAcreditado(fixtura.usuario());

        assertThat(reversar(pago)).isTrue();
        assertThat(reversar(pago)).isFalse();

        assertThat(estado(pago)).isEqualTo("REVERSADO");
        assertThat(contar("SELECT count(*)::int FROM aportes.historial_estado_pago WHERE pago_id = ?", pago))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM aportes.pago WHERE id = ? AND estado = 'ACREDITADO'", pago))
                .as("la fila de alta del pago queda como se escribio")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos reversos simultaneos del mismo pago dejan una sola fila de historial")
    void concurrenciaDeReversos() throws Exception {
        UUID pago = pagoAcreditado(fixtura.usuario());
        var largada = new CountDownLatch(1);
        Callable<Boolean> reversar = () -> {
            largada.await();
            return reversar(pago);
        };

        var resultados = new ArrayList<Boolean>();
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(reversar);
            var b = hilos.submit(reversar);
            largada.countDown();
            resultados.add(a.get());
            resultados.add(b.get());
        }

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(contar("SELECT count(*)::int FROM aportes.historial_estado_pago WHERE pago_id = ?", pago))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("cuadre: aprobar un reembolso lo deja EJECUTADO por su historial, con quien aprobo, sin tocar la fila")
    void reembolsoEjecutadoPorHistorial() {
        UUID solicitante = fixtura.usuario();
        UUID aprobador = fixtura.usuario();
        UUID pago = pagoAcreditado(solicitante);
        var solicitud = transaccion.execute(t -> reembolsoCU.solicitar(
                new EntradaReembolso(pago, Dinero.de("30.00", Moneda.BOB), "DUPLICADO", "inm-" + pago),
                contextoDe(solicitante)));

        transaccion.execute(t -> reembolsoCU.aprobar(solicitud.reembolsoId(), contextoDe(aprobador)));

        assertThat(pagos.verReembolso(dsl, solicitud.reembolsoId())
                        .orElseThrow()
                        .estado())
                .isEqualTo("EJECUTADO");
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM aportes.historial_estado_reembolso
                         WHERE reembolso_id = ? AND estado_nuevo = 'EJECUTADO' AND ejecutado_por = ?
                        """,
                        solicitud.reembolsoId(),
                        aprobador))
                .isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.reembolso WHERE id = ? AND estado = 'SOLICITADO'",
                        solicitud.reembolsoId()))
                .isEqualTo(1);
        Boolean otraVez = transaccion.execute(
                t -> pagos.ejecutarReembolso(dsl, solicitud.reembolsoId(), aprobador, java.time.OffsetDateTime.now()));
        assertThat(otraVez)
                .as("la segunda aprobacion no devuelve la plata dos veces")
                .isFalse();
    }
}
