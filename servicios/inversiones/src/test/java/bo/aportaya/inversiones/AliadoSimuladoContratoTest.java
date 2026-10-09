package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoNoDisponible;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.AliadoRechazo;
import bo.aportaya.inversiones.dominio.puertos.AliadoDeInversion.Estado;
import bo.aportaya.inversiones.infraestructura.clientes.AliadoSimuladoHttp;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba de contrato del lado CONSUMIDOR contra el aliado simulado REAL
 * ({@code herramientas/aliado_simulado}, Python con SQLite), levantado como proceso aparte
 * en loopback. No hay doble: es el adaptador {@link AliadoSimuladoHttp} hablando HTTP con
 * firma HMAC contra el servidor que mantiene el estado.
 *
 * <p>La parte importante: el interes de un DPF lo calcula el servidor Python con su propio
 * codigo y el servicio lo calcula con el suyo; aca se comparan entre si y con el valor hecho a
 * mano. Si divergen, esta prueba lo dice antes que un cliente.
 */
class AliadoSimuladoContratoTest extends ServidorDelAliadoSimulado {

    private final UUID titular = UUID.randomUUID();

    @Test
    @DisplayName("El catalogo llega firmado, con fecha de cotizacion, fuente y marcado SINTETICO")
    void catalogo() {
        var productos = aliado().catalogo();

        assertThat(productos)
                .extracting(p -> p.codigo())
                .contains("DPF-DEMO-180", "DPF-DEMO-360-ANTICIPABLE", "FONDO-DEMO-ABIERTO");
        var dpf = productos.stream()
                .filter(p -> p.codigo().equals("DPF-DEMO-180"))
                .findFirst()
                .orElseThrow();
        assertThat(dpf.plazoDias()).contains(180);
        assertThat(dpf.tasaNominalAnual().orElseThrow()).isEqualByComparingTo("0.040000");
        assertThat(dpf.sintetico()).isTrue();
        assertThat(dpf.fuente()).contains("SINTETICO");
        assertThat(dpf.fechaCotizacion()).isNotNull();
        var fondo = productos.stream()
                .filter(p -> p.codigo().equals("FONDO-DEMO-ABIERTO"))
                .findFirst()
                .orElseThrow();
        assertThat(fondo.horaCorte()).contains("15:00");
        assertThat(fondo.tasaNominalAnual()).as("un fondo no promete tasa").isEmpty();
    }

    @Test
    @DisplayName(
            "Suscribir un DPF: confirma, devuelve posicion externa y vencimiento; repetir la referencia devuelve lo mismo; otro contenido se rechaza")
    void suscribirDpfEIdempotencia() {
        UUID ref = UUID.randomUUID();
        var a = aliado().suscribir(ref, "DPF-DEMO-180", new BigDecimal("10000.00"), titular);
        var b = aliado().suscribir(ref, "DPF-DEMO-180", new BigDecimal("10000.00"), titular);

        assertThat(a.estado()).isEqualTo(Estado.CONFIRMADO);
        assertThat(a.posicionExterna()).contains("POS-" + ref);
        assertThat(a.detalle()).containsKeys("vencimiento", "constitucion");
        assertThat(b.transaccion()).isEqualTo(a.transaccion());
        assertThatThrownBy(() -> aliado().suscribir(ref, "DPF-DEMO-180", new BigDecimal("20000.00"), titular))
                .isInstanceOfSatisfying(AliadoRechazo.class, e -> assertThat(e.codigo())
                        .isEqualTo("IDEMPOTENCIA_CON_CONTENIDO_DISTINTO"));
    }

    @Test
    @DisplayName(
            "Rechazos definitivos del aliado: monto bajo el minimo y producto inexistente; consultar lo que no conoce da vacio")
    void rechazosYDesconocidos() {
        assertThatThrownBy(
                        () -> aliado().suscribir(UUID.randomUUID(), "DPF-DEMO-180", new BigDecimal("499.99"), titular))
                .isInstanceOfSatisfying(
                        AliadoRechazo.class, e -> assertThat(e.codigo()).isEqualTo("MONTO_BAJO_EL_MINIMO"));
        assertThatThrownBy(() -> aliado().suscribir(UUID.randomUUID(), "NO-EXISTE", new BigDecimal("1000.00"), titular))
                .isInstanceOfSatisfying(
                        AliadoRechazo.class, e -> assertThat(e.codigo()).isEqualTo("PRODUCTO_INEXISTENTE"));
        assertThat(aliado().consultar(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName(
            "RESPUESTA PERDIDA real: el servidor registra la operacion y corta la conexion; el adaptador ve «no disponible» y la consulta recupera la confirmacion")
    void respuestaPerdida() throws Exception {
        UUID ref = UUID.randomUUID();
        String cuerpo = "{\"referencia\":\"" + ref
                + "\",\"producto\":\"DPF-DEMO-180\",\"monto\":\"1000.00\",\"moneda\":\"BOB\",\"titularRef\":\""
                + titular + "\",\"escenario\":\"RESPUESTA_PERDIDA\"}";
        var pedido = HttpRequest.newBuilder(base.resolve("/v1/suscripciones"))
                .header("Authorization", "Bearer " + API)
                .header("Idempotency-Key", ref.toString())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        assertThatThrownBy(() -> HTTP.send(pedido, HttpResponse.BodyHandlers.ofString()))
                .isInstanceOf(IOException.class);

        var recuperada = aliado().consultar(ref);

        assertThat(recuperada).isPresent();
        assertThat(recuperada.get().estado()).isEqualTo(Estado.CONFIRMADO);
        assertThat(recuperada.get().monto().orElseThrow()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Una respuesta firmada con OTRO secreto no se acepta: «no disponible», nunca una confirmacion")
    void firmaInvalida() {
        var conOtroSecreto = nuevo("otro-" + "z".repeat(40));
        assertThatThrownBy(conOtroSecreto::catalogo)
                .isInstanceOf(AliadoNoDisponible.class)
                .hasMessageContaining("Firma");
        assertThatThrownBy(() ->
                        conOtroSecreto.suscribir(UUID.randomUUID(), "DPF-DEMO-180", new BigDecimal("1000.00"), titular))
                .isInstanceOf(AliadoNoDisponible.class);
    }

    @Test
    @DisplayName(
            "Sin servidor escuchando el adaptador falla rapido con «no disponible» (timeout y conexion rechazada, no un cuelgue)")
    void servidorCaido() {
        int puertoCerrado;
        try (ServerSocket s = new ServerSocket(0)) {
            puertoCerrado = s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        var aislado = new AliadoSimuladoHttp(
                "simulado", URI.create("http://127.0.0.1:" + puertoCerrado), API, FIRMA, Duration.ofSeconds(2));
        long inicio = System.nanoTime();
        assertThatThrownBy(aislado::catalogo).isInstanceOf(AliadoNoDisponible.class);
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(5));
    }
}
