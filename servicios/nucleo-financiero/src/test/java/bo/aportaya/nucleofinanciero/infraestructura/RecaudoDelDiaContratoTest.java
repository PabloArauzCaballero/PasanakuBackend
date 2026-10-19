package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.clientes.RecaudoDelDiaPorHttp;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.ConfiguracionDeClientes;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * El adaptador de {@code GET /aportes/recaudo-diario} contra un {@code aportes} FALSO (HttpServer del JDK),
 * en los tres niveles del contrato (regla 65): correcto, limite e invalido. Con los tiempos maximos de la
 * plataforma ({@link ConfiguracionDeClientes}), como en produccion.
 *
 * <p>Es un doble: lo que valida es que este lado entiende y rechaza bien el contrato. Que {@code aportes}
 * lo cumpla lo prueban {@code CU51RecaudoDiarioTest} y {@code RecaudoDiarioWebTest} en ese servicio.
 */
class RecaudoDelDiaContratoTest {

    private static final LocalDate DIA = LocalDate.of(2026, 3, 10);

    private HttpServer servidor;

    @AfterEach
    void parar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private RecaudoDelDiaPorHttp contra(int status, String cuerpo, long demoraMs) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/aportes/recaudo-diario", exchange -> {
            try {
                Thread.sleep(demoraMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
        return adaptador("http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    private static RecaudoDelDiaPorHttp adaptador(String url) {
        var constructor = RestClient.builder();
        new ConfiguracionDeClientes().tiemposMaximosDeSalida().customize(constructor);
        return new RecaudoDelDiaPorHttp(constructor, url);
    }

    private static String cuerpo(String fecha, String porMoneda, int excepciones) {
        return """
                {"fecha":"%s","zonaHoraria":"America/La_Paz","porMoneda":[%s],"excepcionesAbiertas":%d}
                """
                .formatted(fecha, porMoneda, excepciones);
    }

    private static final String BOB =
            """
            {"moneda":"BOB","totalRecaudado":"800.00","totalConciliado":"500.00","totalExcepciones":"20.00","cantidadPagos":2}""";

    @Test
    @DisplayName("correcto: un dia en BOB se lee con sus totales exactos y la cantidad de excepciones")
    void correcto() throws IOException {
        var resumen = contra(200, cuerpo("2026-03-10", BOB, 1), 0).consultar(DIA);

        assertThat(resumen).hasValueSatisfying(r -> {
            assertThat(r.excepcionesAbiertas()).isEqualTo(1);
            assertThat(r.porMoneda()).singleElement().satisfies(m -> {
                assertThat(m.moneda()).isEqualTo(Moneda.BOB);
                assertThat(m.recaudado()).isEqualByComparingTo("800.00");
                assertThat(m.conciliado()).isEqualByComparingTo("500.00");
                assertThat(m.excepciones()).isEqualByComparingTo("20.00");
                assertThat(m.cantidadPagos()).isEqualTo(2);
            });
        });
    }

    @Test
    @DisplayName("limite: un dia sin pagos llega como lista vacia, no como un cero inventado en BOB")
    void diaVacio() throws IOException {
        assertThat(contra(200, cuerpo("2026-03-10", "", 0), 0).consultar(DIA))
                .hasValueSatisfying(r -> assertThat(r.porMoneda()).isEmpty());
    }

    @Test
    @DisplayName("limite: la respuesta de OTRA fecha no se usa para cerrar este dia")
    void otraFecha() throws IOException {
        assertThat(contra(200, cuerpo("2026-03-09", BOB, 0), 0).consultar(DIA)).isEmpty();
    }

    @Test
    @DisplayName("invalido: un 500 de aportes es «no se sabe», nunca un resumen")
    void error500() throws IOException {
        assertThat(contra(500, "{\"codigo\":\"AP-XX\"}", 0).consultar(DIA)).isEmpty();
    }

    @Test
    @DisplayName("invalido: un 403 (el token no alcanza) es «no se sabe»")
    void error403() throws IOException {
        assertThat(contra(403, "{\"codigo\":\"AP-SEG-01\"}", 0).consultar(DIA)).isEmpty();
    }

    @Test
    @DisplayName("invalido: JSON roto, moneda desconocida o importe negativo son «no se sabe»")
    void fueraDeContrato() throws IOException {
        assertThat(contra(200, "{no es json", 0).consultar(DIA)).isEmpty();
        parar();
        assertThat(contra(200, cuerpo("2026-03-10", BOB.replace("BOB", "XYZ"), 0), 0)
                        .consultar(DIA))
                .isEmpty();
        parar();
        assertThat(contra(200, cuerpo("2026-03-10", BOB.replace("\"800.00\"", "\"-800.00\""), 0), 0)
                        .consultar(DIA))
                .isEmpty();
    }

    @Test
    @DisplayName("invalido: aportes caido (nadie escucha) es «no se sabe»")
    void caido() throws IOException {
        var adaptador = contra(200, cuerpo("2026-03-10", BOB, 0), 0);
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        parar();
        servidor = null;
        assertThat(adaptador(url).consultar(DIA)).isEmpty();
        assertThat(adaptador).isNotNull();
    }

    @Test
    @DisplayName("invalido: aportes que tarda mas que el tiempo maximo (3 s) es «no se sabe», no un hilo colgado")
    void tardaDemasiado() throws IOException {
        assertThat(contra(200, cuerpo("2026-03-10", BOB, 0), 4_000).consultar(DIA))
                .isEmpty();
    }
}
