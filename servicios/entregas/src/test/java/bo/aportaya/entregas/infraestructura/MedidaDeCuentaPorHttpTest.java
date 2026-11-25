package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * CU-134 (D-11) · el adaptador {@link MedidaDeCuentaPorHttp} contra una identidad DE ENSAYO con la forma publicada
 * ({@code GET /identidad/participantes/{id}/medida-vigente} que contesta {@code {usuarioId, retenerEntrega}}), HTTP
 * real del JDK. Es un doble declarado: la integracion contra identidad de verdad queda pendiente (regla 65).
 *
 * <p>Tres niveles del contrato: correcto (sin medida, no retiene; con medida, retiene), limite (respuesta sin el
 * campo) e invalido (sin credencial, 403, 500, cuerpo ilegible, identidad caida): todo lo que no sea un «no»
 * explicito retiene la entrega.
 */
class MedidaDeCuentaPorHttpTest {

    private static final UUID BENEFICIARIO = UUID.fromString("f1340000-0000-4000-8000-000000000001");
    private static final String TOKEN_DE_SERVICIO = "token-de-servicio-entregas";

    private record Pedido(String metodo, String ruta, String autorizacion) {}

    private HttpServer identidad;
    private final List<Pedido> recibidos = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{\"usuarioId\":\"" + BENEFICIARIO + "\",\"retenerEntrega\":false}";
    private boolean detenida;

    @BeforeEach
    void levantar() throws IOException {
        identidad = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        identidad.createContext("/", intercambio -> {
            recibidos.add(new Pedido(
                    intercambio.getRequestMethod(),
                    intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("Authorization")));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        identidad.start();
    }

    @AfterEach
    void apagar() {
        detener();
    }

    private void detener() {
        if (!detenida) {
            detenida = true;
            identidad.stop(0);
        }
    }

    private MedidaDeCuentaPorHttp adaptador(CredencialDeServicio credencial) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(500);
        fabrica.setReadTimeout(500);
        return new MedidaDeCuentaPorHttp(
                RestClient.builder().requestFactory(fabrica),
                "http://127.0.0.1:" + identidad.getAddress().getPort(),
                credencial);
    }

    @Test
    @DisplayName("correcto: firma con la credencial de servicio y respeta lo que identidad contesta")
    void correcto() {
        var adaptador = adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));

        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isFalse();
        assertThat(recibidos).hasSize(1);
        assertThat(recibidos.get(0).metodo()).isEqualTo("GET");
        assertThat(recibidos.get(0).ruta()).isEqualTo("/identidad/participantes/" + BENEFICIARIO + "/medida-vigente");
        assertThat(recibidos.get(0).autorizacion()).isEqualTo("Bearer " + TOKEN_DE_SERVICIO);

        respuesta = "{\"usuarioId\":\"" + BENEFICIARIO + "\",\"retenerEntrega\":true,\"expedienteId\":\""
                + UUID.randomUUID() + "\"}";
        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isTrue();
    }

    @Test
    @DisplayName("limite: una respuesta sin el campo retenerEntrega retiene")
    void limite() {
        respuesta = "{\"usuarioId\":\"" + BENEFICIARIO + "\"}";
        assertThat(adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO)).retieneEntrega(BENEFICIARIO))
                .isTrue();
    }

    @Test
    @DisplayName("rechaza: sin credencial no pregunta y retiene; 403, 500, cuerpo ilegible e identidad caida retienen")
    void invalido() {
        assertThat(adaptador(CredencialDeServicio.ninguna()).retieneEntrega(BENEFICIARIO)).isTrue();
        assertThat(recibidos).isEmpty();

        var adaptador = adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));
        estado = 403;
        respuesta = "{\"codigo\":\"AP-SEG-01\"}";
        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isTrue();

        estado = 500;
        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isTrue();

        estado = 200;
        respuesta = "esto no es json";
        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isTrue();

        detener();
        assertThat(adaptador.retieneEntrega(BENEFICIARIO)).isTrue();
    }
}
