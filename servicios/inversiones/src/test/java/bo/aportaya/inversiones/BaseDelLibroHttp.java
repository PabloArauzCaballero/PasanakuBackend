package bo.aportaya.inversiones;

import bo.aportaya.inversiones.infraestructura.clientes.LibroPorHttp;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * El servidor HTTP local que habla el contrato propuesto de {@code nucleo-financiero}, comun a las
 * pruebas de contrato del consumidor ({@link LibroPorHttp}): registra lo que recibe y responde lo que la
 * prueba le diga. Es un doble declarado, no el productor.
 */
abstract class BaseDelLibroHttp {

    protected record Peticion(String metodo, String ruta, String clave, String autorizacion, String cuerpo) {}

    protected HttpServer servidor;
    protected final List<Peticion> recibidas = new CopyOnWriteArrayList<>();
    protected volatile int estado = 200;
    protected volatile String respuesta = "{}";
    protected volatile long demoraMs = 0;
    protected LibroPorHttp libro;

    @BeforeEach
    void levantar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        libro = nuevo(Duration.ofSeconds(2));
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    /** Una credencial de PRUEBA, inventada para esta clase: no abre nada en ningun entorno. */
    protected static final String CREDENCIAL = "credencial-de-servicio-de-prueba";

    protected LibroPorHttp conCredencial(String credencial) {
        return new LibroPorHttp(
                URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), Duration.ofSeconds(2), credencial);
    }

    protected LibroPorHttp nuevo(Duration timeout) {
        return new LibroPorHttp(
                URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), timeout);
    }

    private void atender(HttpExchange e) throws IOException {
        String cuerpo = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        recibidas.add(new Peticion(
                e.getRequestMethod(),
                e.getRequestURI().getPath(),
                e.getRequestHeaders().getFirst("Idempotency-Key"),
                e.getRequestHeaders().getFirst("Authorization"),
                cuerpo));
        try {
            Thread.sleep(demoraMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(estado, bytes.length);
        e.getResponseBody().write(bytes);
        e.close();
    }

    protected void responder(int status, String json) {
        this.estado = status;
        this.respuesta = json;
    }

    protected static Dinero bob(String m) {
        return Dinero.de(m, Moneda.BOB);
    }
}
