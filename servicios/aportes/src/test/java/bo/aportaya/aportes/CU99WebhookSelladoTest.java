package bo.aportaya.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.aportes.infraestructura.WebhookRepositorio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F1.L1.M08 (opcion A) · R-BIL-33, contra PostgreSQL real: el aviso de la pasarela es append-only y esta particionado
 * por mes; la deduplicacion (proveedor, clave) vive en {@code webhook_clave} y el cierre del procesamiento es una fila
 * de {@code historial_estado_webhook}.
 */
class CU99WebhookSelladoTest extends BaseDeAportes {

    private static final OffsetDateTime RECIBIDO = OffsetDateTime.parse("2026-11-21T12:00:00Z");

    private final WebhookRepositorio webhooks = new WebhookRepositorio();

    @AfterEach
    void limpiar() {
        fixtura.limpiar();
    }

    private UUID proveedor() {
        return fixtura.proveedor("P" + UUID.randomUUID().toString().substring(0, 8), false, 50);
    }

    private UUID registrar(UUID proveedor, String clave) {
        return transaccion.execute(t -> webhooks.registrar(
                dsl, proveedor, "pago.acreditado", "{\"sintetico\":true}", "firma-sintetica", clave, RECIBIDO));
    }

    @Test
    @DisplayName("rechaza por R-BIL-33: el aviso no se edita ni se borra, ni con el rol de la aplicacion ni sin el")
    void rechazaRBIL33() {
        UUID aviso = registrar(proveedor(), "evt-sellado-1");

        assertThatThrownBy(() -> transaccion.execute(t ->
                        dsl.execute("UPDATE aportes.webhook_pasarela SET estado = 'PROCESADO' WHERE id = ?", aviso)))
                .hasMessageContaining("R-AUD-01");
        assertThatThrownBy(() -> transaccion.execute(
                        t -> dsl.execute("DELETE FROM aportes.webhook_pasarela WHERE id = ?", aviso)))
                .hasMessageContaining("R-AUD-01");
        for (String privilegio : new String[] {"UPDATE", "DELETE"}) {
            for (String tabla : new String[] {"webhook_pasarela", "historial_estado_webhook"}) {
                assertThat(contar(
                                "SELECT CASE WHEN has_table_privilege('svc_aportes', ?, ?) THEN 1 ELSE 0 END",
                                "aportes." + tabla,
                                privilegio))
                        .as("svc_aportes puede %s aportes.%s", privilegio, tabla)
                        .isZero();
            }
        }
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.webhook_pasarela WHERE id = ? AND estado = 'RECIBIDO'",
                        aviso))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("rechaza el segundo alta de la misma (proveedor, clave): choca en pk_webhook_clave y no deja el aviso")
    void rechazaElDuplicado() {
        UUID proveedor = proveedor();
        UUID original = registrar(proveedor, "evt-dup-sellado");

        assertThatThrownBy(() -> registrar(proveedor, "evt-dup-sellado")).hasMessageContaining("pk_webhook_clave");

        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE proveedor_id = ?", proveedor))
                .as("el segundo aviso se deshizo con su clave")
                .isEqualTo(1);
        assertThat(transaccion
                        .execute(t -> webhooks.porClaveDeEvento(dsl, proveedor, "evt-dup-sellado"))
                        .orElseThrow()
                        .id())
                .isEqualTo(original);
        // Otro proveedor puede mandar la misma clave: es otro hecho.
        registrar(proveedor(), "evt-dup-sellado");
        assertThat(contar(
                        "SELECT count(*)::int FROM aportes.webhook_clave WHERE clave_idempotencia = 'evt-dup-sellado'"))
                .isEqualTo(2);
    }

    @Test
    @DisplayName(
            "cuadre: cerrar el procesamiento agrega una fila de historial, el estado vigente es el nuevo y la segunda salida no hace nada")
    void cuadreDelCierre() {
        UUID proveedor = proveedor();
        UUID aviso = registrar(proveedor, "evt-cierre-1");

        transaccion.execute(t -> {
            webhooks.marcarProcesado(dsl, aviso, "DESCARTADO", "sin pago con esa referencia", null, RECIBIDO);
            return null;
        });
        transaccion.execute(t -> {
            webhooks.marcarProcesado(dsl, aviso, "PROCESADO", null, null, RECIBIDO.plusMinutes(1));
            return null;
        });

        var vigente = transaccion
                .execute(t -> webhooks.porClaveDeEvento(dsl, proveedor, "evt-cierre-1"))
                .orElseThrow();
        assertThat(vigente.estado()).isEqualTo("DESCARTADO");
        assertThat(contar("SELECT count(*)::int FROM aportes.historial_estado_webhook WHERE webhook_id = ?", aviso))
                .isEqualTo(1);
        assertThat(contar(
                        """
                        SELECT count(*)::int FROM aportes.webhook_pasarela
                         WHERE id = ? AND estado = 'RECIBIDO' AND procesado_en IS NULL
                        """,
                        aviso))
                .as("la fila del aviso queda como llego")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: dos altas simultaneas del mismo aviso dejan un solo aviso y una sola clave")
    void concurrenciaDeAltas() throws Exception {
        UUID proveedor = proveedor();
        var largada = new CountDownLatch(1);
        Callable<Boolean> alta = () -> {
            largada.await();
            try {
                registrar(proveedor, "evt-carrera-1");
                return true;
            } catch (RuntimeException duplicado) {
                return false;
            }
        };

        var resultados = new ArrayList<Boolean>();
        try (var hilos = Executors.newFixedThreadPool(2)) {
            var a = hilos.submit(alta);
            var b = hilos.submit(alta);
            largada.countDown();
            resultados.add(a.get());
            resultados.add(b.get());
        }

        assertThat(resultados).containsExactlyInAnyOrder(true, false);
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE proveedor_id = ?", proveedor))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_clave WHERE proveedor_id = ?", proveedor))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "cuadre: la purga suelta el mes vencido con su clave y su historial, sin UPDATE ni DELETE sobre el aviso, y la segunda vez no hace nada")
    void cuadreDeLaPurga() {
        UUID proveedor = proveedor();
        OffsetDateTime enero = OffsetDateTime.now(java.time.ZoneOffset.UTC)
                .withDayOfYear(3)
                .withHour(12)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        UUID viejo = transaccion.execute(t ->
                webhooks.registrar(dsl, proveedor, "pago.acreditado", "{}", "firma-sintetica", "evt-enero", enero));
        transaccion.execute(t -> {
            webhooks.marcarProcesado(dsl, viejo, "PROCESADO", null, null, enero);
            return null;
        });
        // Julio del mismo anio: siempre despues del corte (febrero) y dentro de una particion que existe.
        UUID vigente = transaccion.execute(t -> webhooks.registrar(
                dsl, proveedor, "pago.acreditado", "{}", "firma-sintetica", "evt-vigente", enero.plusMonths(6)));
        String corte = enero.toLocalDate().withDayOfMonth(1).plusMonths(1).toString();

        int primera = contar("SELECT aportes.fn_aportes_purgar_webhooks(?::date)", corte);
        int segunda = contar("SELECT aportes.fn_aportes_purgar_webhooks(?::date)", corte);

        assertThat(primera).as("enero entero queda antes del corte").isEqualTo(1);
        assertThat(segunda).isZero();
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE id = ?", viejo))
                .isZero();
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_clave WHERE webhook_id = ?", viejo))
                .isZero();
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_pasarela WHERE id = ?", vigente))
                .isEqualTo(1);
        assertThat(contar("SELECT count(*)::int FROM aportes.webhook_clave WHERE webhook_id = ?", vigente))
                .isEqualTo(1);
    }
}
