package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.aplicacion.CU11RetirarSaldo.SalidaRetiro;
import bo.aportaya.nucleofinanciero.aplicacion.RetirosConProveedor;
import bo.aportaya.nucleofinanciero.aplicacion.RetirosConProveedor.Desenlace;
import bo.aportaya.nucleofinanciero.infraestructura.RetirosProveedorSimulado;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H4.S1.M4 / H3.S1.M3 · El pago de un retiro contra el proceso Python REAL.
 *
 * <p>Lo que importa: «proveedor con una sola liquidacion y ledger coincidente». Se mata el
 * proceso, se pierde la respuesta despues de liquidar y se cortan los envios antes de
 * llegar; en todos los casos el proveedor termina con UNA operacion y el libro con UN debito.
 */
class CU11ServidorRealTest extends BaseDeBilletera {

    private ProveedorPythonLocal proveedor;
    private RetirosConProveedor flujo;
    private EscenarioDeRetiro e;
    private SalidaRetiro orden;

    @BeforeEach
    void preparar() throws Exception {
        proveedor = new ProveedorPythonLocal(Files.createTempDirectory("proveedor-simulado"));
        proveedor.arrancar();
        flujo = FlujosDePrueba.retiros(new RetirosProveedorSimulado(
                "simulado", "simulado", proveedor.url(), proveedor.apiKey, proveedor.firma, Duration.ofSeconds(3)));
        e = EscenarioDeRetiro.con("1000.00");
        orden = e.solicitar("real-" + UUID.randomUUID(), "400.00", "5.00");
    }

    @AfterEach
    void limpiar() {
        proveedor.close();
        fixtura.limpiarBilleteras();
    }

    private int debitos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=? AND sentido='DEBITO' AND glosa='Retiro pagado'",
                e.cuenta());
    }

    private int saldo(String columna) {
        // El nombre de la columna no es un parametro SQL: se elige entre consultas fijas, no se concatena.
        String consulta =
                switch (columna) {
                    case "saldo_disponible" ->
                        "SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?";
                    case "saldo_retenido" ->
                        "SELECT saldo_retenido::int FROM nucleo_financiero.cuenta_billetera WHERE id=?";
                    default -> throw new IllegalArgumentException("columna no prevista: " + columna);
                };
        return contar(consulta, e.cuenta());
    }

    @Test
    @DisplayName(
            "Dado el proveedor real que liquida y pierde la respuesta · Cuando se despacha la orden · Entonces la consulta por referencia encuentra la operación y el libro paga una sola vez · Y repetir el despacho devuelve lo mismo sin otra operación en el proveedor")
    void liquidaYPierdeLaRespuesta() throws Exception {
        proveedor.armarFallo("LIQUIDAR_Y_PERDER", 1);

        var resultado = flujo.despachar(orden.ordenRetiroId(), e.ctx());

        assertThat(resultado.desenlace()).isEqualTo(Desenlace.PAGADO);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(proveedor.resumen().get("confirmadas").asInt()).isEqualTo(1);
        assertThat(debitos()).isEqualTo(1);
        assertThat(saldo("saldo_disponible")).isEqualTo(600);
        assertThat(saldo("saldo_retenido")).isZero();
        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx())).isEqualTo(resultado);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(debitos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado el proveedor real que muere con la orden pendiente · Cuando se resuelve la orden con el proveedor caído y luego vuelve con su archivo · Entonces caído el resultado es desconocido y el dinero sigue retenido · Y al volver se resuelve sin segundo envío: una operación y un débito")
    void reinicioConLaOrdenPendiente() throws Exception {
        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        proveedor.detener();
        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace())
                .as("caido: resultado desconocido, el dinero sigue retenido")
                .isEqualTo(Desenlace.EN_PROCESO);
        assertThat(saldo("saldo_retenido")).isEqualTo(400);

        proveedor.arrancar();
        proveedor.resolver(orden.ordenRetiroId(), "CONFIRMADO");
        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.PAGADO);

        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(debitos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un envío que se corta antes de llegar al proveedor real · Cuando se despacha la orden y se vuelve a intentar · Entonces la primera vez no hay operación en el proveedor y el dinero sigue retenido · Y el siguiente intento la crea una sola vez y el libro paga una vez")
    void corteAntesDeLlegar() throws Exception {
        proveedor.armarFallo("PERDER_ANTES", 1);

        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isZero();
        assertThat(saldo("saldo_retenido")).isEqualTo(400);

        assertThat(flujo.despachar(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.EN_PROCESO);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        proveedor.resolver(orden.ordenRetiroId(), "CONFIRMADO");
        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.PAGADO);
        assertThat(debitos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una orden despachada que el proveedor real rechaza · Cuando se resuelve la orden · Entonces el dinero vuelve a estar disponible y no queda nada retenido · Y el libro no registra ningún débito")
    void rechazoDelProveedor() throws Exception {
        flujo.despachar(orden.ordenRetiroId(), e.ctx());
        proveedor.resolver(orden.ordenRetiroId(), "RECHAZADO");

        assertThat(flujo.resolver(orden.ordenRetiroId(), e.ctx()).desenlace()).isEqualTo(Desenlace.RECHAZADO);

        assertThat(saldo("saldo_disponible")).isEqualTo(1000);
        assertThat(saldo("saldo_retenido")).isZero();
        assertThat(debitos()).isZero();
    }

    @Test
    @DisplayName("concurrencia: tres despachos simultaneos contra el proceso real, una operacion y un debito")
    void despachosSimultaneos() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<RetirosConProveedor.ResultadoDeRetiro>> salidas = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                salidas.add(pool.submit(() -> flujo.despachar(orden.ordenRetiroId(), e.ctx())));
            }
            for (var salida : salidas) {
                salida.get();
            }
        } finally {
            pool.shutdown();
        }
        proveedor.resolver(orden.ordenRetiroId(), "CONFIRMADO");
        flujo.resolver(orden.ordenRetiroId(), e.ctx());
        flujo.resolver(orden.ordenRetiroId(), e.ctx());

        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(debitos()).isEqualTo(1);
        assertThat(saldo("saldo_disponible")).isEqualTo(600);
    }
}
