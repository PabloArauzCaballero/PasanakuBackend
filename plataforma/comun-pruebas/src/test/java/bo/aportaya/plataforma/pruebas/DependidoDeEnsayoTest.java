package bo.aportaya.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El dependido de ensayo hace lo que dice: responde, tarda, se cae, vuelve, y cuenta. Sin esto
 * la prueba de caida de un cliente podria estar verde porque el doble no se cae de verdad.
 */
class DependidoDeEnsayoTest {

    private final HttpClient cliente =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();

    private HttpResponse<String> pedir(DependidoDeEnsayo dependido) throws IOException, InterruptedException {
        return cliente.send(
                HttpRequest.newBuilder(URI.create(dependido.urlBase() + "/x"))
                        .timeout(Duration.ofSeconds(2))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("responde lo que se le dice y cuenta las peticiones que llegaron")
    void respondeYCuenta() throws Exception {
        try (var dependido = new DependidoDeEnsayo().responder(200, "{\"ok\":true}")) {
            var primera = pedir(dependido);
            dependido.responder(503, "{}");
            var segunda = pedir(dependido);

            assertThat(primera.statusCode()).isEqualTo(200);
            assertThat(primera.body()).isEqualTo("{\"ok\":true}");
            assertThat(segunda.statusCode()).isEqualTo(503);
            assertThat(dependido.peticionesRecibidas()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("caido, rechaza la conexion y NO cuenta peticiones; al levantarlo vuelve en el mismo puerto")
    void caeYVuelve() throws Exception {
        try (var dependido = new DependidoDeEnsayo()) {
            String url = dependido.urlBase();
            dependido.caer();

            assertThatThrownBy(() -> pedir(dependido)).isInstanceOf(ConnectException.class);
            assertThat(dependido.peticionesRecibidas()).isZero();

            dependido.levantar();
            assertThat(dependido.urlBase()).isEqualTo(url);
            assertThat(pedir(dependido).statusCode()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("lento, hace saltar el tiempo maximo del cliente")
    void tardaMasQueElTiempoMaximo() throws Exception {
        try (var dependido = new DependidoDeEnsayo().tardar(1500)) {
            var corto = HttpRequest.newBuilder(URI.create(dependido.urlBase() + "/x"))
                    .timeout(Duration.ofMillis(300))
                    .build();

            assertThatThrownBy(() -> cliente.send(corto, HttpResponse.BodyHandlers.ofString()))
                    .isInstanceOf(java.net.http.HttpTimeoutException.class);
        }
    }
}
