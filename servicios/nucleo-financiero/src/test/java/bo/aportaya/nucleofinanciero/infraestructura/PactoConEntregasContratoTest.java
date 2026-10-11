package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.clientes.OfertasPorHttp;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Pacto con {@code entregas} (H8.S1.M3/M4, H9.S1.M5), LADO CONSUMIDOR y forma del productor. El cliente REAL
 * ({@link OfertasPorHttp}) contra un servidor HTTP del JDK que responde lo que dice
 * {@code contratos/pactos/nucleo-financiero-entregas.json}; y ese mismo archivo comparado con el OpenAPI REAL de
 * entregas ({@code entregas.yaml}, solo lectura). Lo que NO esta cubierto: el proceso real de entregas respondiendo.
 */
class PactoConEntregasContratoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PORTADOR = "portador-de-servicio-entregas-del-pacto";
    private static final UUID OFERTA = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID OFERTA_2 = UUID.fromString("b0000000-0000-4000-8000-000000000002");
    private static final UUID VENDEDOR = UUID.fromString("b0000000-0000-4000-8000-000000000003");

    private record Visto(String metodo, String ruta, String autorizacion) {}

    private HttpServer servidor;
    private JsonNode pacto;
    private final List<Visto> vistos = new CopyOnWriteArrayList<>();
    private volatile int estadoForzado = -1;
    private volatile String cuerpoForzado;

    private static Path raiz() {
        Path p = Path.of("").toAbsolutePath();
        while (p != null && !Files.isRegularFile(p.resolve("contratos/pactos/nucleo-financiero-entregas.json"))) {
            p = p.getParent();
        }
        if (p == null) {
            throw new IllegalStateException("No se encontro contratos/pactos/nucleo-financiero-entregas.json");
        }
        return p;
    }

    private static String resolver(String t) {
        return t.replace("{oferta}", OFERTA.toString())
                .replace("{oferta2}", OFERTA_2.toString())
                .replace("{vendedor}", VENDEDOR.toString());
    }

    @BeforeEach
    void levantar() throws IOException {
        pacto = JSON.readTree(Files.readString(
                raiz().resolve("contratos/pactos/nucleo-financiero-entregas.json"), StandardCharsets.UTF_8));
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", this::atender);
        servidor.start();
        var jwt = Jwt.withTokenValue(PORTADOR)
                .header("alg", "none")
                .claim("sub", "entregas")
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
                cuerpo = resolver(i.path("respuesta").path("ejemplo").toString());
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

    private OfertasPorHttp cliente() {
        return new OfertasPorHttp(
                RestClient.builder(),
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @Test
    @DisplayName(
            "pacto · correcto: el cliente real pide la oferta con el token de quien llama y entiende vendedor, precio y estado")
    void ofertaReservada() {
        var oferta = cliente().oferta(OFERTA).orElseThrow();

        assertThat(oferta.vendedorUsuarioId()).isEqualTo(VENDEDOR);
        assertThat(oferta.precio().toString()).isEqualTo("480.00");
        assertThat(oferta.estado()).isEqualTo("RESERVADA");
        assertThat(vistos)
                .containsExactly(new Visto("GET", "/entregas/mercado/ofertas/" + OFERTA, "Bearer " + PORTADOR));
    }

    @Test
    @DisplayName("pacto · invalido: una oferta que entregas no tiene (422 AP-CU130-05) es vacio: nucleo no paga")
    void ofertaInexistente() {
        assertThat(cliente().oferta(OFERTA_2)).isEmpty();
    }

    @Test
    @DisplayName(
            "pacto · limite: cada estado que entregas publica llega tal cual al caso de uso; la decision de pagar es de nucleo")
    void todosLosEstadosPublicados() {
        for (JsonNode estado : pacto.path("estadosDeOferta")) {
            estadoForzado = 200;
            cuerpoForzado = "{\"ofertaId\":\"" + OFERTA + "\",\"vendedorUsuarioId\":\"" + VENDEDOR
                    + "\",\"precio\":{\"monto\":\"480.00\",\"moneda\":\"BOB\"},\"estado\":\"" + estado.asText() + "\"}";
            assertThat(cliente().oferta(OFERTA).orElseThrow().estado()).isEqualTo(estado.asText());
        }
    }

    @Test
    @DisplayName(
            "pacto · invalido: entregas caido (500), sin cuerpo util o respondiendo por otra oferta: vacio, nunca un vendedor adivinado")
    void respuestasInutiles() {
        estadoForzado = 500;
        cuerpoForzado = "{}";
        assertThat(cliente().oferta(OFERTA)).isEmpty();
        estadoForzado = 200;
        cuerpoForzado = "no es json";
        assertThat(cliente().oferta(OFERTA)).isEmpty();
        cuerpoForzado = "{\"ofertaId\":\"" + OFERTA_2 + "\",\"vendedorUsuarioId\":\"" + VENDEDOR
                + "\",\"precio\":{\"monto\":\"1.00\",\"moneda\":\"BOB\"},\"estado\":\"RESERVADA\"}";
        assertThat(cliente().oferta(OFERTA)).as("respuesta de otra oferta").isEmpty();
        cuerpoForzado = "{\"ofertaId\":\"" + OFERTA + "\",\"precio\":{\"monto\":\"1.00\",\"moneda\":\"BOB\"}}";
        assertThat(cliente().oferta(OFERTA)).as("sin vendedor").isEmpty();
    }

    @Test
    @DisplayName(
            "pacto · productor: entregas.yaml publica la operacion con los campos, los estados y el 422 que el pacto asume")
    void entregasPublicaLaOferta() throws IOException {
        String yaml = Files.readString(
                raiz().resolve("servicios/entregas/src/main/resources/openapi/entregas.yaml"), StandardCharsets.UTF_8);
        int desde = yaml.indexOf("  /entregas/mercado/ofertas/{ofertaId}:\n");
        assertThat(desde).as("la ruta GET de la oferta esta publicada").isGreaterThan(0);
        String ruta = yaml.substring(desde, yaml.indexOf("  /entregas/mercado/ofertas/{ofertaId}/cancelacion:"));
        assertThat(ruta)
                .contains("get:", "operationId: consultarOfertaParaPago", "'200':", "'422':", "SERVICIO_ENTREGAS");

        String esquema = yaml.substring(yaml.indexOf("    OfertaParaPago:"), yaml.indexOf("    PaginaDeOfertas:"));
        assertThat(esquema).contains("required: [ofertaId, vendedorUsuarioId, precio, estado]");
        List<String> publicados = new ArrayList<>();
        pacto.path("estadosDeOferta").forEach(n -> publicados.add(n.asText()));
        assertThat(esquema).contains("enum: [" + String.join(", ", publicados) + "]");
        for (JsonNode campo :
                pacto.path("interacciones").get(0).path("respuesta").path("campos")) {
            assertThat(esquema).contains(campo.asText().split("\\.")[0] + ":");
        }
    }
}
