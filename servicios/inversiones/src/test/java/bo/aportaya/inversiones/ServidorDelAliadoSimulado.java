package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.inversiones.infraestructura.clientes.AliadoSimuladoHttp;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * El simulador REAL ({@code herramientas/aliado_simulado}, Python con SQLite) levantado como
 * proceso aparte en loopback, uno por prueba, con el reloj de negocio fijo en el lunes
 * 12/10/2026 08:00 de La Paz. Las pruebas de contrato del adaptador heredan de aca.
 */
abstract class ServidorDelAliadoSimulado {

    protected static final String API = "api-" + "a".repeat(40);
    protected static final String CONTROL = "control-" + "b".repeat(40);
    protected static final String FIRMA = "firma-" + "c".repeat(40);

    protected static final HttpClient HTTP = HttpClient.newHttpClient();

    private Process proceso;
    protected URI base;
    private Path carpeta;

    private final UUID titular = UUID.randomUUID();

    @BeforeEach
    void arrancarElSimulador() throws Exception {
        Path raiz = Path.of("").toAbsolutePath();
        while (raiz != null && !Files.isDirectory(raiz.resolve("herramientas/aliado_simulado"))) {
            raiz = raiz.getParent();
        }
        if (raiz == null) {
            throw new IllegalStateException("No encontre herramientas/aliado_simulado subiendo desde "
                    + Path.of("").toAbsolutePath());
        }
        int puerto;
        try (ServerSocket s = new ServerSocket(0)) {
            puerto = s.getLocalPort();
        }
        carpeta = Files.createTempDirectory("aliado-simulado-");
        var constructor = new ProcessBuilder(
                        "python",
                        "-m",
                        "herramientas.aliado_simulado.servidor",
                        "--database",
                        carpeta.resolve("aliado.sqlite").toString(),
                        "--port",
                        Integer.toString(puerto),
                        "--inicio",
                        "2026-10-12T12:00:00+00:00")
                .directory(raiz.toFile())
                .redirectErrorStream(true)
                .redirectOutput(carpeta.resolve("servidor.log").toFile());
        constructor.environment().put("PASANAKU_AMBIENTE", "simulado");
        constructor.environment().put("ALIADO_API_KEY", API);
        constructor.environment().put("ALIADO_CONTROL_KEY", CONTROL);
        constructor.environment().put("ALIADO_FIRMA_SECRETO", FIRMA);
        proceso = constructor.start();
        base = URI.create("http://127.0.0.1:" + puerto);
        for (int i = 0; i < 100; i++) {
            try {
                if (HTTP.send(
                                        HttpRequest.newBuilder(base.resolve("/health"))
                                                .build(),
                                        HttpResponse.BodyHandlers.ofString())
                                .statusCode()
                        == 200) {
                    return;
                }
            } catch (IOException aunNoEscucha) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException(
                "El simulador no arranco: " + Files.readString(carpeta.resolve("servidor.log")));
    }

    @AfterEach
    void apagarElSimulador() throws Exception {
        if (proceso != null) {
            proceso.destroy();
            proceso.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            proceso.destroyForcibly();
        }
        if (carpeta != null) {
            try (var arbol = Files.walk(carpeta)) {
                arbol.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> p.toFile().delete());
            }
        }
    }

    protected AliadoSimuladoHttp nuevo(String firma) {
        return new AliadoSimuladoHttp("simulado", base, API, firma, Duration.ofSeconds(5));
    }

    protected AliadoSimuladoHttp aliado() {
        return nuevo(FIRMA);
    }

    protected String control(String ruta, String cuerpo) throws Exception {
        var r = HttpRequest.newBuilder(base.resolve(ruta))
                .header("Authorization", "Bearer " + CONTROL)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();
        HttpResponse<String> respuesta = HTTP.send(r, HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(200);
        return respuesta.body();
    }
}
