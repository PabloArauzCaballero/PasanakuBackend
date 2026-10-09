package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.nucleofinanciero.aplicacion.CU10RecargarSaldo;
import bo.aportaya.nucleofinanciero.aplicacion.RecargasConProveedor;
import bo.aportaya.nucleofinanciero.infraestructura.RecargasProveedorSimulado;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * H3.S1.M2/M3 · H4.S1.M1 · El servidor Python REAL y el backend REAL, de punta a punta.
 *
 * <p>Nada se mockea del lado del proveedor: el adaptador HTTP firma, verifica y reintenta
 * contra el proceso de {@code herramientas/proveedor_simulado}, con su SQLite en disco. Lo
 * que se rompe a proposito —matar el proceso, perder la respuesta, cortar antes de
 * registrar, firmar con otro secreto— se rompe de verdad. Lo que NO se prueba aca: un
 * proveedor distinto del ficticio (no existe contrato real todavia) ni la app completa.
 */
class CU10ServidorRealTest extends BaseDeBilletera {

    private ProveedorPythonLocal proveedor;
    private RecargasConProveedor flujo;
    private UUID cuenta;
    private ContextoSesion titular;

    @BeforeEach
    void preparar() throws Exception {
        proveedor = new ProveedorPythonLocal(Files.createTempDirectory("proveedor-simulado"));
        proveedor.arrancar();
        flujo = FlujosDePrueba.recargas(adaptador(proveedor.firma));
        fixtura.tipoDeCambioDeHoy();
        fixtura.limite("RECARGA", "ESTANDAR", "MES", new BigDecimal("10000.00"), null);
        UUID usuario = fixtura.usuario();
        cuenta = fixtura.billetera(usuario, "ESTANDAR", BigDecimal.ZERO);
        titular = contextoDe(usuario);
    }

    @AfterEach
    void limpiar() throws Exception {
        proveedor.close();
        fixtura.limpiarBilleteras();
    }

    private RecargasProveedorSimulado adaptador(String firma) {
        return new RecargasProveedorSimulado(
                "simulado", "simulado", proveedor.url(), proveedor.apiKey, firma, Duration.ofSeconds(3));
    }

    private UUID solicitar(String clave, String monto) {
        return flujo.solicitar(
                        new CU10RecargarSaldo.EntradaSolicitud(
                                clave,
                                cuenta,
                                Dinero.de(monto, Moneda.BOB),
                                Dinero.cero(Moneda.BOB),
                                "QR",
                                Optional.empty()),
                        titular)
                .ordenRecargaId();
    }

    private int saldo() {
        return contar("SELECT saldo_disponible::int FROM nucleo_financiero.cuenta_billetera WHERE id=?", cuenta);
    }

    private int abonos() {
        return contar(
                "SELECT count(*)::int FROM nucleo_financiero.movimiento_billetera WHERE cuenta_billetera_id=? AND sentido='CREDITO'",
                cuenta);
    }

    @Test
    @DisplayName(
            "Dada una recarga pendiente en el proveedor real · Cuando se intenta confirmar antes de que el proveedor resuelva · Entonces no acredita · Y cuando el proveedor confirma se acredita una sola vez y repetir devuelve lo mismo")
    void cicloCompletoContraElProcesoReal() throws Exception {
        UUID orden = solicitar("real-1", "500.00");
        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);
        assertThat(saldo()).isZero();
        assertThat(proveedor.resumen().get("pendientes").asInt()).isEqualTo(1);

        proveedor.resolver(orden, "CONFIRMADO");
        var primera = flujo.confirmar(orden, titular);
        var repetida = flujo.confirmar(orden, titular);

        assertThat(repetida).isEqualTo(primera);
        assertThat(saldo()).isEqualTo(500);
        assertThat(abonos()).isEqualTo(1);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(proveedor.resumen().get("confirmadas").asInt()).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND referencia_externa IS NOT NULL",
                        orden))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un proveedor real que se cae con una recarga pendiente · Cuando se intenta confirmar y luego vuelve con su archivo · Entonces con el proveedor caído no se acredita porque el resultado es desconocido · Y al volver conserva la operación y la confirmación se acredita una sola vez")
    void reinicioDelProveedorConservaElEstado() throws Exception {
        UUID orden = solicitar("real-2", "300.00");
        proveedor.detener();
        assertThatThrownBy(() -> flujo.confirmar(orden, titular))
                .as("con el proveedor caido el resultado es desconocido, no un abono")
                .isInstanceOf(ErrorDeNegocio.class);
        assertThat(saldo()).isZero();

        proveedor.arrancar();
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        proveedor.resolver(orden, "CONFIRMADO");
        flujo.confirmar(orden, titular);
        flujo.confirmar(orden, titular);

        assertThat(saldo()).isEqualTo(300);
        assertThat(abonos()).isEqualTo(1);
    }

    @Test
    @DisplayName("reintento: respuesta perdida despues de registrar, la misma clave no crea otra operacion")
    void respuestaPerdidaNoDuplicaLaOperacion() throws Exception {
        proveedor.armarFallo("PERDER_DESPUES", 1);
        assertThatThrownBy(() -> solicitar("real-3", "250.00")).isInstanceOf(ErrorDeNegocio.class);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?",
                        cuenta))
                .as("la intencion ya estaba persistida antes de llamar al proveedor")
                .isEqualTo(1);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);

        UUID orden = solicitar("real-3", "250.00");

        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE cuenta_billetera_id=?",
                        cuenta))
                .isEqualTo(1);
        proveedor.resolver(orden, "CONFIRMADO");
        flujo.confirmar(orden, titular);
        assertThat(saldo()).isEqualTo(250);
    }

    @Test
    @DisplayName("reintento: corte antes de registrar, el proveedor no sabe nada y el reintento lo crea una sola vez")
    void corteAntesDeRegistrar() throws Exception {
        proveedor.armarFallo("PERDER_ANTES", 1);
        assertThatThrownBy(() -> solicitar("real-4", "120.00")).isInstanceOf(ErrorDeNegocio.class);
        assertThat(proveedor.resumen().get("operaciones").asInt()).isZero();

        UUID orden = solicitar("real-4", "120.00");

        assertThat(proveedor.resumen().get("operaciones").asInt()).isEqualTo(1);
        proveedor.resolver(orden, "CONFIRMADO");
        flujo.confirmar(orden, titular);
        assertThat(saldo()).isEqualTo(120);
        assertThat(abonos()).isEqualTo(1);
    }

    @Test
    @DisplayName("concurrencia: doble confirmacion simultanea contra el proceso real, un abono")
    void dobleConfirmacionSimultanea() throws Exception {
        UUID orden = solicitar("real-5", "500.00");
        proveedor.resolver(orden, "CONFIRMADO");

        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<CU10RecargarSaldo.SalidaAcreditacion>> resultados = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                resultados.add(pool.submit(() -> flujo.confirmar(orden, titular)));
            }
            var primera = resultados.get(0).get();
            for (var otra : resultados) {
                assertThat(otra.get()).isEqualTo(primera);
            }
        } finally {
            pool.shutdown();
        }

        assertThat(saldo()).isEqualTo(500);
        assertThat(abonos()).isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dado un proveedor real que rechaza la recarga · Cuando se intenta confirmar · Entonces la orden queda RECHAZADA · Y no se acredita nada")
    void rechazoDelProveedor() throws Exception {
        UUID orden = solicitar("real-6", "80.00");
        proveedor.resolver(orden, "RECHAZADO");

        assertThatThrownBy(() -> flujo.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.orden_recarga WHERE id=? AND estado='RECHAZADA'",
                        orden))
                .isEqualTo(1);
    }

    @Test
    @DisplayName(
            "Dada una confirmación firmada con otro secreto contra el servidor real · Cuando se intenta confirmar la recarga · Entonces no acredita y deja una discrepancia FIRMA_INVALIDA · Y con el secreto correcto la misma confirmación sí acredita")
    void firmaDeOtroSecreto() throws Exception {
        UUID orden = solicitar("real-7", "500.00");
        proveedor.resolver(orden, "CONFIRMADO");
        var conOtroSecreto = FlujosDePrueba.recargas(adaptador("otro-secreto-que-no-es-el-del-proveedor-0001"));

        assertThatThrownBy(() -> conOtroSecreto.confirmar(orden, titular)).isInstanceOf(ErrorDeNegocio.class);

        assertThat(saldo()).isZero();
        assertThat(abonos()).isZero();
        assertThat(contar(
                        "SELECT count(*)::int FROM nucleo_financiero.discrepancia_proveedor WHERE referencia_id=? AND tipo='FIRMA_INVALIDA'",
                        orden))
                .isEqualTo(1);
        // Con el secreto correcto, esa misma confirmacion si se acredita: el rechazo era de la firma.
        flujo.confirmar(orden, titular);
        assertThat(saldo()).isEqualTo(500);
    }

    @Test
    @DisplayName(
            "Dada una recarga confirmada contra el proveedor real · Cuando se revisa el log del proveedor · Entonces no contiene la clave de acceso, la clave de control ni la firma de la corrida")
    void elRegistroNoFiltraSecretos() throws Exception {
        UUID orden = solicitar("real-8", "10.00");
        proveedor.resolver(orden, "CONFIRMADO");
        flujo.confirmar(orden, titular);
        proveedor.close();
        String registro = proveedor.registro();
        assertThat(registro).doesNotContain(proveedor.apiKey, proveedor.controlKey, proveedor.firma);
    }
}
