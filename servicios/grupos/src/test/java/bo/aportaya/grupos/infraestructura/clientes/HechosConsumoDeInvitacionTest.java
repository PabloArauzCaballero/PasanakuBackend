package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Carril G · quien pierde la carrera por un enlace de invitacion de un solo uso.
 *
 * <p>Hallado con cuatro canjes simultaneos del mismo enlace: identidad contesta 422 a los que llegan despues del primero y
 * este adaptador dejaba salir la excepcion HTTP, que el servicio devolvia como 500. Tiene que ser la regla de negocio
 * AP-CU69-05 («esa invitacion ya no es valida»), igual que el segundo canje en secuencia. Tres niveles contra un
 * {@code identidad} de ensayo: correcto, limite (422) e invalido (cuerpo vacio).
 */
class HechosConsumoDeInvitacionTest {

    private static final UUID TOKEN = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID CLAVE = UUID.randomUUID();

    private HttpServer servidor;

    @AfterEach
    void apagar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    private HechosPorHttp contra(int estado, String respuesta) throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                intercambio.getResponseBody().write(bytes);
            }
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
    @DisplayName("concurrencia: el segundo en llegar a un enlace de un solo uso recibe la regla de negocio, no un 500")
    void elQuePierdeRecibeLaReglaDeNegocio() throws Exception {
        var hechos = contra(422, "{\"codigo\":\"AP-CU69-05\",\"mensaje\":\"ya consumida\"}");

        assertThatThrownBy(() -> hechos.consumirInvitacion(TOKEN, GRUPO, CLAVE, "secreto"))
                .isInstanceOf(ErrorDeNegocio.class)
                .hasMessageContaining("ya no es valida");
    }

    @Test
    @DisplayName("correcto: el consumo confirmado devuelve el recibo con la clave con la que se pidio")
    void elConsumoConfirmado() throws Exception {
        var hechos = contra(200, "{\"clave\":\"" + CLAVE + "\"}");

        var recibo = hechos.consumirInvitacion(TOKEN, GRUPO, CLAVE, "secreto");

        assertThat(recibo.clave()).isEqualTo(CLAVE);
    }

    @Test
    @DisplayName("rechaza un consumo sin cuerpo: no se inventa un recibo")
    void sinCuerpoNoHayRecibo() throws Exception {
        var hechos = contra(200, "");

        assertThatThrownBy(() -> hechos.consumirInvitacion(TOKEN, GRUPO, CLAVE, "secreto"))
                .isInstanceOf(RuntimeException.class);
    }
}
