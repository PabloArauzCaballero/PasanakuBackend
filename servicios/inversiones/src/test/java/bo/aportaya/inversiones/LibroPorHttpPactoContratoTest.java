package bo.aportaya.inversiones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.inversiones.dominio.puertos.LibroDelTitular.SaldoInsuficiente;
import bo.aportaya.inversiones.infraestructura.clientes.LibroPorHttp;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Pacto con el libro, LADO CONSUMIDOR: el cliente REAL de inversiones ({@link LibroPorHttp}) contra un servidor HTTP del
 * JDK que responde lo que dice {@code contratos/pactos/inversiones-nucleo-financiero.json}. El lado productor del mismo
 * archivo es {@code PactoConInversionesTest} en nucleo-financiero (servidor real, PostgreSQL real). Prueba que el
 * cliente manda EXACTAMENTE lo pactado (metodo, ruta, cuerpo, quien firma) y entiende exactamente lo pactado.
 */
class LibroPorHttpPactoContratoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String TOKEN_PERSONA = "token-de-la-persona-del-pacto";
    private static final String CREDENCIAL = "credencial-de-servicio-del-pacto";

    private static final Map<String, String> IDS = Map.ofEntries(
            Map.entry("cuenta", "a0000000-0000-4000-8000-000000000001"),
            Map.entry("titular", "a0000000-0000-4000-8000-000000000002"),
            Map.entry("orden", "a0000000-0000-4000-8000-000000000003"),
            Map.entry("orden2", "a0000000-0000-4000-8000-000000000004"),
            Map.entry("orden3", "a0000000-0000-4000-8000-000000000005"),
            Map.entry("rescate", "a0000000-0000-4000-8000-000000000006"),
            Map.entry("retencion", "a0000000-0000-4000-8000-000000000007"),
            Map.entry("retencion2", "a0000000-0000-4000-8000-000000000008"),
            Map.entry("debito", "a0000000-0000-4000-8000-000000000009"),
            Map.entry("acreditacion", "a0000000-0000-4000-8000-00000000000a"));

    private record Visto(String interaccion, String identidad, String clave) {}

    private JsonNode pacto;
    private HttpServer servidor;
    private final List<Visto> vistos = new CopyOnWriteArrayList<>();
    private final List<String> sinPacto = new CopyOnWriteArrayList<>();

    private static String resolver(String texto) {
        String r = texto;
        for (var e : IDS.entrySet()) {
            r = r.replace("{" + e.getKey() + "}", e.getValue());
        }
        return r;
    }

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve("contratos/pactos/inversiones-nucleo-financiero.json"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro contratos/pactos/inversiones-nucleo-financiero.json");
        }
        return p;
    }

    @BeforeEach
    void levantar() throws IOException {
        pacto = JSON.readTree(Files.readString(
                raiz().resolve("contratos/pactos/inversiones-nucleo-financiero.json"), StandardCharsets.UTF_8));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        var jwt = Jwt.withTokenValue(TOKEN_PERSONA)
                .header("alg", "none")
                .claim("sub", "persona-del-pacto")
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
        SecurityContextHolder.clearContext();
    }

    private void atender(HttpExchange e) throws IOException {
        String cuerpo = new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String metodo = e.getRequestMethod();
        String ruta = e.getRequestURI().getPath();
        String autorizacion = e.getRequestHeaders().getFirst("Authorization");
        String clave = e.getRequestHeaders().getFirst("Idempotency-Key");
        JsonNode cuerpoRecibido = cuerpo.isBlank() ? null : JSON.readTree(cuerpo);

        for (JsonNode i : pacto.path("interacciones")) {
            JsonNode p = i.path("peticion");
            if (i.has("soloProductor")
                    || !p.path("metodo").asText().equals(metodo)
                    || !resolver(p.path("ruta").asText()).equals(ruta)) {
                continue;
            }
            JsonNode esperado =
                    p.has("cuerpo") ? JSON.readTree(resolver(p.get("cuerpo").toString())) : null;
            if (!java.util.Objects.equals(esperado, cuerpoRecibido)) {
                continue;
            }
            String identidad = p.path("identidad").asText();
            String firma = "SERVICIO".equals(identidad) ? "Bearer " + CREDENCIAL : "Bearer " + TOKEN_PERSONA;
            if (!firma.equals(autorizacion)) {
                sinPacto.add(
                        i.path("id").asText() + ": firmada con '" + autorizacion + "', el pacto pide " + identidad);
                continue;
            }
            vistos.add(new Visto(i.path("id").asText(), identidad, clave));
            responder(e, i.path("respuesta"));
            return;
        }
        sinPacto.add(metodo + " " + ruta + " " + cuerpo);
        byte[] raw = "{}".getBytes(StandardCharsets.UTF_8);
        e.sendResponseHeaders(599, raw.length);
        e.getResponseBody().write(raw);
        e.close();
    }

    private void responder(HttpExchange e, JsonNode respuesta) throws IOException {
        byte[] raw = resolver(respuesta.path("ejemplo").toString()).getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().add("Content-Type", "application/json");
        e.sendResponseHeaders(respuesta.path("estado").asInt(), raw.length);
        e.getResponseBody().write(raw);
        e.close();
    }

    private LibroPorHttp libro() {
        return new LibroPorHttp(
                URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), Duration.ofSeconds(2), CREDENCIAL);
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    private static UUID id(String nombre) {
        return UUID.fromString(IDS.get(nombre));
    }

    @Test
    @DisplayName(
            "pacto · el cliente real manda exactamente lo pactado (metodo, ruta, cuerpo, quien firma) y entiende lo pactado")
    void elClienteCumpleElPacto() {
        var libro = libro();

        var saldo = libro.saldo(id("cuenta"));
        assertThat(saldo.titularId()).isEqualTo(id("titular"));
        assertThat(saldo.disponible()).isEqualTo(bob("1000.00"));
        assertThat(saldo.comprometidoEnPozos()).isEqualTo(bob("0.00"));

        assertThat(libro.retener(id("cuenta"), bob("300.00"), "RETENER:o1", id("orden")))
                .isEqualTo(id("retencion"));
        assertThat(libro.debitarInversion(id("retencion"), bob("300.00"), "DEBITAR:o1"))
                .isEqualTo(id("debito"));
        assertThat(libro.acreditarRescate(id("cuenta"), bob("300.00"), "ACREDITAR:r1", id("rescate")))
                .isEqualTo(id("acreditacion"));
        assertThat(libro.retener(id("cuenta"), bob("150.00"), "RETENER:o2", id("orden2")))
                .isEqualTo(id("retencion2"));
        libro.liberar(id("retencion2"), "LIBERAR:o2");
        assertThatThrownBy(() -> libro.retener(id("cuenta"), bob("99999.00"), "RETENER:o3", id("orden3")))
                .isInstanceOf(SaldoInsuficiente.class);

        assertThat(sinPacto).as("peticiones que el pacto no contempla").isEmpty();
        Set<String> esperadas = new HashSet<>();
        pacto.path("interacciones").forEach(i -> {
            if (!i.has("soloProductor")) {
                esperadas.add(i.path("id").asText());
            }
        });
        Set<String> ejercitadas = new HashSet<>();
        vistos.forEach(v -> ejercitadas.add(v.interaccion()));
        assertThat(ejercitadas)
                .as("toda interaccion del consumidor se ejercito")
                .isEqualTo(esperadas);
    }

    @Test
    @DisplayName(
            "pacto · cada clave de idempotencia es un UUID, estable para la misma instruccion y distinta entre instrucciones")
    void clavesDeIdempotencia() {
        var libro = libro();
        libro.retener(id("cuenta"), bob("300.00"), "RETENER:o1", id("orden"));
        libro.debitarInversion(id("retencion"), bob("300.00"), "DEBITAR:o1");
        libro.debitarInversion(id("retencion"), bob("300.00"), "DEBITAR:o1");

        List<String> claves = new ArrayList<>();
        vistos.forEach(v -> claves.add(v.clave()));
        assertThat(claves).hasSize(3).allSatisfy(c -> assertThat(UUID.fromString(c))
                .isNotNull());
        assertThat(claves.get(1)).as("el reintento usa la MISMA clave").isEqualTo(claves.get(2));
        assertThat(claves.get(0)).isNotEqualTo(claves.get(1));
    }

    @Test
    @DisplayName("pacto · debitar y acreditar salen con la credencial de servicio aunque haya una sesion de persona")
    void quienFirma() {
        var libro = libro();
        libro.retener(id("cuenta"), bob("300.00"), "RETENER:o1", id("orden"));
        libro.debitarInversion(id("retencion"), bob("300.00"), "DEBITAR:o1");
        libro.acreditarRescate(id("cuenta"), bob("300.00"), "ACREDITAR:r1", id("rescate"));

        assertThat(vistos)
                .extracting(Visto::interaccion, Visto::identidad)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("retener", "PERSONA"),
                        org.assertj.core.groups.Tuple.tuple("debitar", "SERVICIO"),
                        org.assertj.core.groups.Tuple.tuple("acreditar", "SERVICIO"));
        assertThat(sinPacto).isEmpty();
    }
}
