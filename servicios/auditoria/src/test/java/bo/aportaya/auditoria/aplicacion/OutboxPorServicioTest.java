package bo.aportaya.auditoria.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.dominio.OutboxDeServicio;
import bo.aportaya.auditoria.infraestructura.ConsultaPrometheus;
import bo.aportaya.plataforma.dominio.Reloj;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Sistemas › Outbox contra un DOBLE de Prometheus (HttpServer del JDK con su API {@code /api/v1/query}, como
 * {@code PrometheusDobleTest}). El Prometheus real y los nombres que le da el Collector se verifican en TEST.
 */
class OutboxPorServicioTest {

    private HttpServer doble;

    private OutboxPorServicio levantar(int estado, Function<String, String> responder) throws IOException {
        doble = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        doble.createContext("/api/v1/query", x -> {
            String q = URLDecoder.decode(x.getRequestURI().getRawQuery(), StandardCharsets.UTF_8);
            byte[] cuerpo = responder.apply(q).getBytes(StandardCharsets.UTF_8);
            x.sendResponseHeaders(estado, cuerpo.length);
            x.getResponseBody().write(cuerpo);
            x.close();
        });
        doble.start();
        var prometheus =
                new ConsultaPrometheus("http://127.0.0.1:" + doble.getAddress().getPort(), Duration.ofSeconds(2));
        return new OutboxPorServicio(
                prometheus,
                Reloj.fijo(Instant.parse("2026-10-29T15:00:00Z")),
                "aportaya_outbox_pendientes",
                "aportaya_outbox_fallidos",
                "aportaya_outbox_edad_mas_viejo_segundos",
                "aportaya_outbox_publicaciones_total");
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
        return "{\"metric\":{" + etiquetas + "},\"value\":[1761750000,\"" + valor + "\"]}";
    }

    @Test
    @DisplayName("correcto · por servicio: pendientes, fallidos, edad y publicaciones ok/error de 24 h (redondeadas)")
    void porServicio() throws IOException {
        var outbox = levantar(200, q -> {
            if (q.contains("aportaya_outbox_pendientes"))
                return vector(serie("\"service_name\":\"aportes\"", "3"), serie("\"service_name\":\"erp\"", "0"));
            if (q.contains("aportaya_outbox_fallidos")) return vector(serie("\"service_name\":\"aportes\"", "1"));
            if (q.contains("edad_mas_viejo")) return vector(serie("\"service_name\":\"aportes\"", "42.4"));
            return vector(
                    serie("\"service_name\":\"aportes\",\"resultado\":\"ok\"", "120.6"),
                    serie("\"service_name\":\"aportes\",\"resultado\":\"error\"", "2"));
        });
        var salida = outbox.consultar();
        assertThat(salida.fuenteDisponible()).isTrue();
        assertThat(salida.servicios()).extracting(OutboxDeServicio::servicio).containsExactly("aportes", "erp");
        var aportes = salida.servicios().get(0);
        assertThat(aportes.pendientes()).contains(3L);
        assertThat(aportes.fallidos()).contains(1L);
        assertThat(aportes.edadMasViejoSegundos()).contains(42L);
        assertThat(aportes.publicados24h()).contains(121L);
        assertThat(aportes.errores24h()).contains(2L);
        var erp = salida.servicios().get(1);
        assertThat(erp.pendientes()).contains(0L);
        assertThat(erp.fallidos()).isEmpty();
    }

    @Test
    @DisplayName("limite · sin series: fuente disponible y lista vacia; una serie sin service_name no se atribuye")
    void sinSeries() throws IOException {
        var outbox = levantar(200, q -> q.contains("pendientes") ? vector(serie("\"job\":\"x\"", "5")) : vector());
        var salida = outbox.consultar();
        assertThat(salida.fuenteDisponible()).isTrue();
        assertThat(salida.servicios()).isEmpty();
    }

    @Test
    @DisplayName("invalido · Prometheus contesta 500: fuente no disponible, nunca una lista vacia que diga «todo bien»")
    void caido() throws IOException {
        var salida = levantar(500, q -> "{}").consultar();
        assertThat(salida.fuenteDisponible()).isFalse();
        assertThat(salida.servicios()).isEmpty();
        assertThat(Optional.of(salida.consultadoEn())).contains(Instant.parse("2026-10-29T15:00:00Z"));
    }
}
