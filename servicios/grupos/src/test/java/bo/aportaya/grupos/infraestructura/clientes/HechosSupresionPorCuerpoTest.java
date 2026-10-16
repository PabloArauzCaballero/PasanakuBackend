package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Carril G · la pregunta «este contacto pidio no recibir» (CU-69) viaja con el contacto en el CUERPO y no en la URL
 * (regla 90.2.2): {@code POST /notificaciones/supresion/consultas}, el reemplazo del GET deprecado con la query.
 *
 * <p>Tres niveles contra un {@code notificaciones} de ensayo con la forma publicada: correcto, limite e invalido
 * (no responde, 429 por tope de consultas, cuerpo ilegible: deniega, o sea «esta suprimido»).
 */
class HechosSupresionPorCuerpoTest {

    private static final String TELEFONO = "+59171234567";

    private record Pedido(String metodo, String ruta, String consulta, String cuerpo) {}

    private HttpServer servidor;
    private final List<Pedido> recibidos = new CopyOnWriteArrayList<>();

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HechosPorHttp contra(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            recibidos.add(new Pedido(
                    intercambio.getRequestMethod(),
                    intercambio.getRequestURI().getPath(),
                    intercambio.getRequestURI().getQuery(),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new HechosPorHttp(RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url);
    }

    @Test
    @DisplayName("correcto: el contacto va en el cuerpo del POST, sin query, y la respuesta se lee tal cual")
    void elContactoVaEnElCuerpo() throws Exception {
        var hechos = contra(200, "{\"suprimido\":false}");

        assertThat(hechos.contactoSuprimido(TELEFONO, "INVITACION_GRUPO")).isFalse();

        assertThat(recibidos).hasSize(1);
        Pedido p = recibidos.get(0);
        assertThat(p.metodo()).isEqualTo("POST");
        assertThat(p.ruta()).isEqualTo("/notificaciones/supresion/consultas");
        assertThat(p.consulta())
                .as("ni el telefono ni la categoria en la URL (90.2.2)")
                .isNull();
        assertThat(p.cuerpo())
                .contains("\"identificador\":\"" + TELEFONO + "\"")
                .contains("INVITACION_GRUPO");
    }

    @Test
    @DisplayName("limite: suprimido=true se respeta, y {} (campo ausente) no inventa una supresion")
    void suprimidoYAusente() throws Exception {
        assertThat(contra(200, "{\"suprimido\":true}").contactoSuprimido(TELEFONO, "INVITACION_GRUPO"))
                .isTrue();
        apagar();
        assertThat(contra(200, "{}").contactoSuprimido(TELEFONO, "INVITACION_GRUPO"))
                .isFalse();
    }

    @Test
    @DisplayName(
            "rechaza: 429 por tope de consultas, 403 y servicio caido se leen como SUPRIMIDO (denegar por omision)")
    void sinRespuestaEntendibleSeDeniega() throws Exception {
        for (int estado : new int[] {429, 403, 404}) {
            assertThat(contra(estado, "{\"codigo\":\"AP-X\"}").contactoSuprimido(TELEFONO, "INVITACION_GRUPO"))
                    .as("HTTP %d", estado)
                    .isTrue();
            apagar();
        }
        var caido = contra(200, "{\"suprimido\":false}");
        servidor.stop(0);
        assertThat(caido.contactoSuprimido(TELEFONO, "INVITACION_GRUPO")).isTrue();
    }
}
