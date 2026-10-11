package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.clientes.HechosPorHttp;
import bo.aportaya.nucleofinanciero.infraestructura.clientes.OfertasPorHttp;
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
import org.springframework.web.client.RestClient;

/**
 * Prueba de contrato del CONSUMIDOR de {@code aportes} (lo comprometido en pozos) y de {@code entregas} (la oferta),
 * en tres niveles (regla 65): correcto, limite e invalido. El de {@code aportes} se compara ademas con su OpenAPI real
 * (solo lectura); el de {@code entregas} es un contrato PROPUESTO que {@code entregas} todavia no publica: lo que queda
 * verificado es que nucleo FALLA CERRADO (sin oferta confirmada no se paga).
 */
class ContratosConOtrosServiciosTest {

    private HttpServer server;
    private final AtomicInteger estado = new AtomicInteger(200);
    private final AtomicReference<String> cuerpo = new AtomicReference<>("{}");
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private HechosPorHttp hechos;
    private OfertasPorHttp ofertas;

    @BeforeEach
    void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            ruta.set(exchange.getRequestURI().getPath());
            byte[] raw = cuerpo.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado.get(), raw.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(raw);
            }
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        hechos = new HechosPorHttp(RestClient.builder(), url, url);
        ofertas = new OfertasPorHttp(RestClient.builder(), url);
    }

    @AfterEach
    void cerrar() {
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
        var comprometido = hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.BOB);
        assertThat(comprometido).isPresent();
        assertThat(comprometido.get().toString()).isEqualTo("1500.00");
    }

    @Test
    @DisplayName("limite: sin deuda ni por aportar es cero, que es distinto de no saber")
    void comprometidoCero() {
        cuerpo.set("{\"deudaVigente\":\"0.00\",\"porAportar\":\"0.00\",\"obligacionesAbiertas\":0,\"moneda\":\"BOB\"}");
        assertThat(hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.BOB)
                        .orElseThrow()
                        .esCero())
                .isTrue();
    }

    @Test
    @DisplayName("invalido: otra moneda, 404, 500, cuerpo ilegible o importe mal formado dan vacio (nunca cero)")
    void comprometidoInvalido() {
        cuerpo.set(ESTADO);
        assertThat(hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.USD)).isEmpty();
        for (int codigo : new int[] {404, 500}) {
            estado.set(codigo);
            assertThat(hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.BOB))
                    .isEmpty();
        }
        estado.set(200);
        cuerpo.set("esto no es json");
        assertThat(hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.BOB)).isEmpty();
        cuerpo.set("{\"deudaVigente\":\"abc\",\"porAportar\":\"1.001\",\"moneda\":\"BOB\"}");
        assertThat(hechos.comprometidoEnPozos(UUID.randomUUID(), Moneda.BOB)).isEmpty();
    }

    @Test
    @DisplayName("correcto: la oferta trae vendedor, precio y estado")
    void ofertaCorrecta() {
        UUID oferta = UUID.randomUUID();
        UUID vendedor = UUID.randomUUID();
        cuerpo.set(
                "{\"ofertaId\":\"%s\",\"vendedorUsuarioId\":\"%s\",\"precio\":{\"monto\":\"480.00\",\"moneda\":\"BOB\"},\"estado\":\"RESERVADA\"}"
                        .formatted(oferta, vendedor));
        var o = ofertas.oferta(oferta).orElseThrow();
        assertThat(o.vendedorUsuarioId()).isEqualTo(vendedor);
        assertThat(o.precio().toString()).isEqualTo("480.00");
        assertThat(o.estado()).isEqualTo("RESERVADA");
        assertThat(ruta.get()).isEqualTo("/entregas/mercado/ofertas/" + oferta);
    }

    @Test
    @DisplayName(
            "invalido: oferta ajena a la pedida, sin vendedor, 404, 500 o importe mal formado fallan CERRADO (vacio)")
    void ofertaInvalida() {
        UUID oferta = UUID.randomUUID();
        cuerpo.set(
                "{\"ofertaId\":\"%s\",\"vendedorUsuarioId\":\"%s\",\"precio\":{\"monto\":\"480.00\",\"moneda\":\"BOB\"},\"estado\":\"RESERVADA\"}"
                        .formatted(UUID.randomUUID(), UUID.randomUUID()));
        assertThat(ofertas.oferta(oferta)).as("otra oferta").isEmpty();
        cuerpo.set("{\"ofertaId\":\"%s\",\"precio\":{\"monto\":\"480.00\",\"moneda\":\"BOB\"}}".formatted(oferta));
        assertThat(ofertas.oferta(oferta)).as("sin vendedor").isEmpty();
        cuerpo.set(
                "{\"ofertaId\":\"%s\",\"vendedorUsuarioId\":\"%s\",\"precio\":{\"monto\":\"480.001\",\"moneda\":\"BOB\"}}"
                        .formatted(oferta, UUID.randomUUID()));
        assertThat(ofertas.oferta(oferta)).as("importe con tres decimales").isEmpty();
        for (int codigo : new int[] {404, 500}) {
            estado.set(codigo);
            assertThat(ofertas.oferta(oferta)).isEmpty();
        }
    }
}
