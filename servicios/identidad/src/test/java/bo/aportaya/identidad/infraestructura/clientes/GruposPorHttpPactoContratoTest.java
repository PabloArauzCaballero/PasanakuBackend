package bo.aportaya.identidad.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Pacto con {@code grupos} (perfil del integrante, 2026-10-21), LADO CONSUMIDOR y forma del productor. El cliente REAL
 * ({@link GruposPorHttp}) contra un servidor HTTP del JDK que responde lo que dice
 * {@code contratos/pactos/identidad-grupos-pertenencia.json}; y ese mismo archivo comparado con el OpenAPI REAL de grupos
 * ({@code grupos.yaml}, solo lectura). Lo que NO esta cubierto: el proceso real de grupos respondiendo.
 */
class GruposPorHttpPactoContratoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PACTO = "contratos/pactos/identidad-grupos-pertenencia.json";
    private static final String PORTADOR = "portador-de-quien-pregunta";
    private static final UUID GRUPO = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("d0000000-0000-4000-8000-000000000002");
    private static final UUID AJENO = UUID.fromString("d0000000-0000-4000-8000-000000000003");

    private record Visto(String metodo, String ruta, String autorizacion) {}

    private HttpServer servidor;
    private JsonNode pacto;
    private final List<Visto> vistos = new CopyOnWriteArrayList<>();
    private volatile int estadoForzado = -1;
    private volatile String cuerpoForzado;

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve(PACTO))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro " + PACTO);
        }
        return p;
    }

    private static String resolver(String t) {
        return t.replace("{grupo}", GRUPO.toString())
                .replace("{usuario}", USUARIO.toString())
                .replace("{ajeno}", AJENO.toString());
    }

    @BeforeEach
    void levantar() throws IOException {
        pacto = JSON.readTree(Files.readString(raiz().resolve(PACTO), StandardCharsets.UTF_8));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        var jwt = Jwt.withTokenValue(PORTADOR)
                .header("alg", "none")
                .claim("sub", "quien-pregunta")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
        SecurityContextHolder.clearContext();
    }

    private void atender(HttpExchange e) throws IOException {
        String ruta = e.getRequestURI().getPath();
        vistos.add(new Visto(e.getRequestMethod(), ruta, e.getRequestHeaders().getFirst("Authorization")));
        int estado = 599;
        String cuerpo = "{}";
        for (JsonNode i : pacto.path("interacciones")) {
            JsonNode p = i.path("peticion");
            if (p.path("metodo").asText().equals(e.getRequestMethod())
                    && resolver(p.path("ruta").asText()).equals(ruta)) {
                estado = i.path("respuesta").path("estado").asInt();
                cuerpo = i.path("respuesta").path("ejemplo").toString();
            }
        }
        if (estadoForzado > 0) {
            estado = estadoForzado;
            cuerpo = cuerpoForzado;
        }
        byte[] raw = cuerpo.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(estado, raw.length);
        e.getResponseBody().write(raw);
        e.close();
    }

    private GruposPorHttp cliente() {
        return new GruposPorHttp(
                RestClient.builder(),
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    @DisplayName("pacto · correcto: el cliente real pregunta con el token de quien llama y entiende comparten=true")
    void comparten() {
        assertThat(cliente().compartenGrupo(GRUPO, USUARIO)).isTrue();
        assertThat(vistos)
                .containsExactly(new Visto(
                        "GET", "/grupos/" + GRUPO + "/integrantes/" + USUARIO + "/comparte", "Bearer " + PORTADOR));
    }

    @Test
    @DisplayName("pacto · limite: comparten=false es un no")
    void noComparten() {
        assertThat(cliente().compartenGrupo(GRUPO, AJENO)).isFalse();
    }

    @Test
    @DisplayName("pacto · invalido: grupos con 500, 403, cuerpo ilegible o caido es un no, nunca un si adivinado")
    void fallaCerrado() {
        estadoForzado = 500;
        cuerpoForzado = "{}";
        assertThat(cliente().compartenGrupo(GRUPO, USUARIO)).as("500").isFalse();
        estadoForzado = 403;
        assertThat(cliente().compartenGrupo(GRUPO, USUARIO)).as("403").isFalse();
        estadoForzado = 200;
        cuerpoForzado = "no-es-json";
        assertThat(cliente().compartenGrupo(GRUPO, USUARIO)).as("ilegible").isFalse();
        cuerpoForzado = "{}";
        assertThat(cliente().compartenGrupo(GRUPO, USUARIO)).as("sin el campo").isFalse();
        var caido = cliente();
        servidor.stop(0);
        assertThat(caido.compartenGrupo(GRUPO, USUARIO)).as("caido").isFalse();
    }

    @Test
    @DisplayName("pacto · productor: grupos.yaml publica la operacion GET con el campo comparten que el pacto asume")
    void gruposPublicaLaOperacion() throws IOException {
        String yaml = Files.readString(
                        raiz().resolve("servicios/grupos/src/main/resources/openapi/grupos.yaml"),
                        StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
        int desde = yaml.indexOf("  /grupos/{grupoId}/integrantes/{usuarioId}/comparte:\n");
        assertThat(desde).as("la ruta esta publicada").isGreaterThan(0);
        String ruta = yaml.substring(desde, yaml.indexOf("\n  /", desde + 1));
        assertThat(ruta).contains("get:", "operationId: consultarSiCompartenGrupo", "'200':");
        int esquema = yaml.indexOf("    PertenenciaCompartida:\n");
        assertThat(esquema).as("el esquema esta publicado").isGreaterThan(0);
        assertThat(yaml.substring(esquema, yaml.indexOf("\n    ", esquema + 30) + 200))
                .contains("required: [comparten]", "comparten: { type: boolean }");
    }
}
