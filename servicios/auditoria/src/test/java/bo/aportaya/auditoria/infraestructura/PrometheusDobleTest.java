package bo.aportaya.auditoria.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.aplicacion.ConsultasDeTrafico;
import bo.aportaya.auditoria.aplicacion.EndpointsDelDia;
import bo.aportaya.auditoria.aplicacion.IndicadoresDeOperacion;
import bo.aportaya.plataforma.dominio.Indicador.Motivo;
import bo.aportaya.plataforma.dominio.PeriodoDeTablero;
import bo.aportaya.plataforma.dominio.Reloj;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tableros · OPERACION contra un DOBLE de Prometheus (un HttpServer del JDK que habla su API {@code /api/v1/query}).
 * El Prometheus real se ejercita con el stack levantado; aca se cubren los tres niveles del contrato (regla 65):
 * respuesta con series, respuesta sin series o con NaN, y Prometheus caido o contestando error.
 */
class PrometheusDobleTest {

    private HttpServer doble;
    private final AtomicReference<Function<String, String>> responder = new AtomicReference<>();
    private final AtomicReference<Integer> estado = new AtomicReference<>(200);

    private String levantar() throws IOException {
        doble = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        doble.createContext("/api/v1/query", intercambio -> {
            String query = URLDecoder.decode(intercambio.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            byte[] cuerpo = responder.get().apply(query).getBytes(StandardCharsets.UTF_8);
            intercambio.sendResponseHeaders(estado.get(), cuerpo.length);
            intercambio.getResponseBody().write(cuerpo);
            intercambio.close();
        });
        doble.start();
        return "http://127.0.0.1:" + doble.getAddress().getPort();
    }

    @AfterEach
    void bajar() {
        if (doble != null) {
            doble.stop(0);
        }
    }

    private static String vector(String... series) {
        return "{\"status\":\"success\",\"data\":{\"resultType\":\"vector\",\"result\":[" + String.join(",", series)
                + "]}}";
    }

    private static String serie(String etiquetas, String valor) {
        return "{\"metric\":{" + etiquetas + "},\"value\":[1760000000,\"" + valor + "\"]}";
    }

    private IndicadoresDeOperacion calculador(String url) {
        return new IndicadoresDeOperacion(
                new ConsultaPrometheus(url, Duration.ofSeconds(2)),
                new ConsultasDeTrafico("http_server_requests_seconds"));
    }

    @Test
    @DisplayName("correcto: con series, cuenta solicitudes, separa 4xx y 5xx, calcula la tasa de error y las latencias")
    void conSeries() throws IOException {
        responder.set(q -> {
            if (q.contains("histogram_quantile(0.95")) return vector(serie("", "182.4"));
            if (q.contains("histogram_quantile")) return vector(serie("", "90"));
            if (q.contains("status=~\"5..\"")) return vector(serie("", "5"));
            if (q.contains("status=~\"4..\"")) return vector(serie("", "15"));
            if (q.contains("status=~\"2..|3..\"")) return vector(serie("", "180"));
            return vector(serie("", "200"));
        });
        var c = calculador(levantar()).calcular(null, PeriodoDeTablero.de("2026-10-15"));

        assertThat(c)
                .extracting(k -> k.indicador().codigo())
                .containsExactly(
                        "SOLICITUDES_TOTALES",
                        "SOLICITUDES_EXITOSAS",
                        "SOLICITUDES_FALLIDAS_4XX",
                        "SOLICITUDES_FALLIDAS_5XX",
                        "TASA_ERROR",
                        "LATENCIA_P50",
                        "LATENCIA_P95",
                        "LATENCIA_P99");
        assertThat(c.get(0).indicador().valor()).isEqualByComparingTo("200");
        assertThat(c.get(4).indicador().valor()).isEqualByComparingTo("2.50");
        assertThat(c.get(6).indicador().valor()).isEqualByComparingTo("182.40");
    }

    @Test
    @DisplayName("límite: sin trafico los conteos son CERO real (la consulta corrió) y un percentil NaN no se publica")
    void sinTrafico() throws IOException {
        responder.set(q -> q.contains("histogram_quantile") ? vector(serie("", "NaN")) : vector());
        var c = calculador(levantar()).calcular(null, PeriodoDeTablero.de("2026-10-15"));

        assertThat(c.get(0).indicador().valor()).isEqualByComparingTo("0");
        assertThat(c.get(5).indicador().valor()).isNull();
    }

    @Test
    @DisplayName("inválido: Prometheus contestando 500 o caído deja todo FUENTE_NO_CONFIGURADA, nunca ceros")
    void caido() throws IOException {
        responder.set(q -> "{}");
        estado.set(500);
        var conError = calculador(levantar()).calcular(null, PeriodoDeTablero.de("2026-10-15"));
        assertThat(conError).allSatisfy(k -> {
            assertThat(k.indicador().valor()).isNull();
            assertThat(k.indicador().motivo()).isEqualTo(Motivo.FUENTE_NO_CONFIGURADA);
        });

        var sinServidor = calculador("http://127.0.0.1:1").calcular(null, PeriodoDeTablero.de("2026-10-15"));
        assertThat(sinServidor)
                .allSatisfy(k -> assertThat(k.indicador().valor()).isNull());
        assertThat(calculador("").calcular(null, PeriodoDeTablero.de("2026-10-15")))
                .allSatisfy(k -> assertThat(k.indicador().valor()).isNull());
    }

    @Test
    @DisplayName(
            "correcto: los endpoints del día salen ordenados por solicitudes, con sus errores y p95, con la ruta plantilla")
    void endpoints() throws IOException {
        String grupos = "\"service_name\":\"grupos\",\"method\":\"GET\",\"uri\":\"/grupos/{id}\"";
        String pagos = "\"service_name\":\"aportes\",\"method\":\"POST\",\"uri\":\"/pagos\"";
        responder.set(q -> {
            if (q.contains("histogram_quantile")) return vector(serie(grupos, "120.5"));
            if (q.contains("status=~\"5..\"")) return vector(serie(pagos, "2"));
            if (q.contains("status=~\"4..\"")) return vector();
            return vector(serie(pagos, "40"), serie(grupos, "300"));
        });
        var reloj = Reloj.fijo(Instant.parse("2026-10-16T18:00:00Z"));
        var salida = new EndpointsDelDia(
                        new ConsultaPrometheus(levantar(), Duration.ofSeconds(2)),
                        new ConsultasDeTrafico("http_server_requests_seconds"),
                        reloj)
                .consultar(25);

        assertThat(salida.fuenteDisponible()).isTrue();
        assertThat(salida.endpoints())
                .extracting(EndpointsDelDia.Endpoint::ruta)
                .containsExactly("/grupos/{id}", "/pagos");
        assertThat(salida.endpoints().get(0).p95Ms())
                .hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("120.50"));
        assertThat(salida.endpoints().get(1).errores5xx()).isEqualTo(2);
    }

    @Test
    @DisplayName(
            "inválido: con Prometheus caído los endpoints dicen fuente no disponible, no una lista vacía de «nadie llamó»")
    void endpointsSinFuente() {
        var salida = new EndpointsDelDia(
                        new ConsultaPrometheus("http://127.0.0.1:1", Duration.ofMillis(500)),
                        new ConsultasDeTrafico("http_server_requests_seconds"),
                        Reloj.delSistema())
                .consultar(500);

        assertThat(salida.fuenteDisponible()).isFalse();
        assertThat(salida.endpoints()).isEmpty();
    }
}
