package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.clientes.HechosPorHttp;
import bo.aportaya.plataforma.dominio.Moneda;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Prueba de contrato del CONSUMIDOR de {@code aportes} (lo comprometido en pozos), en tres niveles (regla 65):
 * correcto, limite e invalido, comparado ademas con su OpenAPI real (solo lectura). El consumidor de la oferta de
 * {@code entregas} se retiro con la cesion de turnos (D-L2-1).
 */
class ContratosConOtrosServiciosTest {

    private HttpServer server;
    private final AtomicInteger estado = new AtomicInteger(200);
    private final AtomicReference<String> cuerpo = new AtomicReference<>("{}");
    private final AtomicReference<String> ruta = new AtomicReference<>();
    /** Carril Q: el estado de pagos se pide por participacion (la de grupos), no por usuario; el usuario es el de la sesion. */
    private final UUID usuario = UUID.randomUUID();

    private final UUID participante = UUID.randomUUID();
    private final AtomicReference<String> participaciones = new AtomicReference<>();
    private HechosPorHttp hechos;

    @BeforeEach
    void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            ruta.set(exchange.getRequestURI().getPath());
            boolean deGrupos =
                    "/grupos/participaciones".equals(exchange.getRequestURI().getPath());
            byte[] raw = (deGrupos ? participaciones.get() : cuerpo.get()).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado.get(), raw.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(raw);
            }
        });
        server.start();
        participaciones.set("[{\"grupoId\":\"%s\",\"participanteId\":\"%s\",\"estado\":\"ACTIVO\"}]"
                .formatted(UUID.randomUUID(), participante));
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .subject(usuario.toString())
                        .build()));
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        hechos = new HechosPorHttp(RestClient.builder(), url, url);
    }

    @AfterEach
    void cerrar() {
        SecurityContextHolder.clearContext();
        server.stop(0);
    }

    private static final String ESTADO =
            """
            {"alDia":false,"totalAportado":"900.00","deudaVigente":"300.00","porAportar":"1200.00",
             "obligacionesAbiertas":2,"moneda":"BOB","campoNuevoQueNoConozco":true}
            """;

    @Test
    @DisplayName(
            "contrato aportes: el estado del participante trae deudaVigente, porAportar y moneda como el OpenAPI de aportes")
    void aportesPublicaLosCamposQueSeLeen() throws IOException {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null
                && !Files.isRegularFile(actual.resolve("servicios/aportes/src/main/resources/openapi/aportes.yaml"))) {
            actual = actual.getParent();
        }
        String yaml = Files.readString(
                actual.resolve("servicios/aportes/src/main/resources/openapi/aportes.yaml"), StandardCharsets.UTF_8);
        String bloque = yaml.substring(yaml.indexOf("    EstadoDelParticipante:"));
        bloque = bloque.substring(0, bloque.indexOf("    Morosos:"));
        assertThat(bloque).contains("deudaVigente:", "porAportar:", "moneda:", "obligacionesAbiertas:");
        assertThat(bloque)
                .contains("required: [alDia, totalAportado, deudaVigente, porAportar, obligacionesAbiertas, moneda]");
    }

    @Test
    @DisplayName("correcto: lo comprometido es deudaVigente mas porAportar, y un campo desconocido no rompe la lectura")
    void comprometidoCorrecto() {
        cuerpo.set(ESTADO);
        var comprometido = hechos.comprometidoEnPozos(usuario, Moneda.BOB);
        assertThat(comprometido).isPresent();
        assertThat(comprometido.get().toString()).isEqualTo("1500.00");
        // Se pregunta a aportes por la PARTICIPACION de grupos, nunca por el usuario.
        assertThat(ruta.get()).isEqualTo("/aportes/participantes/" + participante + "/estado");
    }

    @Test
    @DisplayName("limite: sin deuda ni por aportar es cero, que es distinto de no saber")
    void comprometidoCero() {
        cuerpo.set("{\"deudaVigente\":\"0.00\",\"porAportar\":\"0.00\",\"obligacionesAbiertas\":0,\"moneda\":\"BOB\"}");
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB).orElseThrow().esCero())
                .isTrue();
    }

    @Test
    @DisplayName("invalido: otra moneda, 404, 500, cuerpo ilegible o importe mal formado dan vacio (nunca cero)")
    void comprometidoInvalido() {
        cuerpo.set(ESTADO);
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.USD)).isEmpty();
        for (int codigo : new int[] {404, 500}) {
            estado.set(codigo);
            assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
        }
        estado.set(200);
        cuerpo.set("esto no es json");
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
        cuerpo.set("{\"deudaVigente\":\"abc\",\"porAportar\":\"1.001\",\"moneda\":\"BOB\"}");
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
    }
}
