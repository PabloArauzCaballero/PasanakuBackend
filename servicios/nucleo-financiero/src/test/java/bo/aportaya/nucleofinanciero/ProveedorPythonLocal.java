package bo.aportaya.nucleofinanciero;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * El proveedor ficticio de {@code herramientas/proveedor_simulado}, como proceso real.
 *
 * <p>Lo que se prueba contra esto no es un doble en memoria: es el servidor HTTP con su
 * SQLite, sus firmas y sus cortes. Requisito del entorno: un interprete de Python en el
 * PATH ({@code python}, {@code python3} o la variable {@code PYTHON_EJECUTABLE}). Si no
 * esta, la prueba <b>falla con ese motivo</b>; no se saltea.
 *
 * <p>Los tres secretos se generan al azar en cada corrida, viven solo en la memoria de la
 * prueba y nunca se imprimen.
 */
final class ProveedorPythonLocal implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    final String apiKey = secreto();
    final String controlKey = secreto();
    final String firma = secreto();
    private final Path baseDeDatos;
    private final Path registro;
    private final int puerto;
    private final HttpClient http = HttpClient.newHttpClient();
    private Process proceso;

    ProveedorPythonLocal(Path directorio) throws IOException {
        this.baseDeDatos = directorio.resolve("proveedor.sqlite");
        this.registro = directorio.resolve("proveedor.log");
        try (var libre = new ServerSocket(0)) {
            this.puerto = libre.getLocalPort();
        }
    }

    URI url() {
        return URI.create("http://127.0.0.1:" + puerto);
    }

    /** Arranca (o rearranca sobre el mismo archivo) el servidor y espera a que responda. */
    void arrancar() throws Exception {
        var comando = List.of(
                interprete(),
                "-m",
                "herramientas.proveedor_simulado.servidor",
                "--database",
                baseDeDatos.toString(),
                "--port",
                String.valueOf(puerto));
        var proceso = new ProcessBuilder(comando)
                .directory(raizDelRepositorio().toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(registro.toFile()));
        proceso.environment().put("PASANAKU_AMBIENTE", "simulado");
        proceso.environment().put("PROVEEDOR_API_KEY", apiKey);
        proceso.environment().put("PROVEEDOR_CONTROL_KEY", controlKey);
        proceso.environment().put("PROVEEDOR_FIRMA_SECRETO", firma);
        this.proceso = proceso.start();
        var limite = Instant.now().plus(Duration.ofSeconds(20));
        while (Instant.now().isBefore(limite)) {
            if (!this.proceso.isAlive()) {
                throw new IllegalStateException(
                        "El proveedor ficticio termino al arrancar: " + Files.readString(registro));
            }
            try {
                if (get("/health", null).statusCode() == 200) {
                    return;
                }
            } catch (IOException aunNoEscucha) {
                Thread.sleep(100);
            }
        }
        throw new IllegalStateException("El proveedor ficticio no respondio /health en 20 s.");
    }

    /** Lo corta como lo haria una caida: sin despedirse. El archivo conserva el estado. */
    void detener() throws InterruptedException {
        if (proceso != null && proceso.isAlive()) {
            proceso.destroyForcibly();
            proceso.waitFor();
        }
    }

    JsonNode control(String ruta, String cuerpo) throws Exception {
        var respuesta = http.send(
                HttpRequest.newBuilder(url().resolve(ruta))
                        .header("Authorization", "Bearer " + controlKey)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (respuesta.statusCode() != 200) {
            throw new IllegalStateException("Control " + ruta + " respondio " + respuesta.statusCode());
        }
        return JSON.readTree(respuesta.body());
    }

    void resolver(UUID referencia, String estado) throws Exception {
        control("/control/resolver", "{\"referencia\":\"" + referencia + "\",\"estado\":\"" + estado + "\"}");
    }

    void armarFallo(String modo, int cantidad) throws Exception {
        control("/control/fallo", "{\"modo\":\"" + modo + "\",\"cantidad\":" + cantidad + "}");
    }

    /** {@code operaciones}, {@code confirmadas}, {@code pendientes} y {@code rechazadas}. */
    JsonNode resumen() throws Exception {
        return JSON.readTree(get("/control/estado", controlKey).body());
    }

    private HttpResponse<String> get(String ruta, String clave) throws Exception {
        var peticion = HttpRequest.newBuilder(url().resolve(ruta)).timeout(Duration.ofSeconds(3));
        if (clave != null) {
            peticion.header("Authorization", "Bearer " + clave);
        }
        return http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Lo que el proceso escribio en su salida: tiene que estar libre de secretos. */
    String registro() throws IOException {
        return Files.exists(registro) ? Files.readString(registro) : "";
    }

    @Override
    public void close() {
        try {
            detener();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String secreto() {
        byte[] azar = new byte[24];
        new SecureRandom().nextBytes(azar);
        return "prueba-" + HexFormat.of().formatHex(azar);
    }

    private static String interprete() {
        var candidatos = new java.util.ArrayList<String>();
        var indicado = System.getenv("PYTHON_EJECUTABLE");
        if (indicado != null && !indicado.isBlank()) {
            candidatos.add(indicado);
        }
        candidatos.addAll(List.of("python", "python3"));
        for (String candidato : candidatos) {
            try {
                var p = new ProcessBuilder(candidato, "--version")
                        .redirectErrorStream(true)
                        .start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() == 0) {
                    return candidato;
                }
            } catch (IOException | InterruptedException noEsta) {
                // se prueba el siguiente
            }
        }
        throw new IllegalStateException(
                "Estas pruebas necesitan Python en el PATH (python, python3 o PYTHON_EJECUTABLE) para levantar el proveedor ficticio.");
    }

    private static Path raizDelRepositorio() {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null) {
            if (Files.isRegularFile(actual.resolve("herramientas/proveedor_simulado/servidor.py"))) {
                return actual;
            }
            actual = actual.getParent();
        }
        throw new IllegalStateException("No se encontro herramientas/proveedor_simulado desde "
                + Path.of("").toAbsolutePath());
    }
}
