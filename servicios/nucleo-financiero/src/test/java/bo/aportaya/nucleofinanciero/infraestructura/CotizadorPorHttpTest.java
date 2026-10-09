package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/**
 * Prueba de contrato del CONSUMIDOR: lo que este servicio le manda a {@code tarifas} tiene que ser lo que el
 * OpenAPI de {@code tarifas} admite. Se lee su contrato (solo lectura) y se compara; no se levanta tarifas.
 *
 * <p>Esta prueba existe porque el cotizador mandaba {@code referenciaTipo = "OPERACION"}, un valor que el
 * contrato de tarifas no admite: contra el servicio real toda cotizacion habria fallado y, como quien
 * pregunta rechaza ante la duda, TODA recarga y TODO retiro. Con un doble que acepta cualquier cosa nadie
 * lo veia.
 */
class CotizadorPorHttpTest {

    private static final Pattern ENUM_TIPO = Pattern.compile(
            "EntradaCotizacion:.*?referenciaTipo:\\s*\\R\\s*type: string\\s*\\R\\s*enum: \\[([^\\]]+)\\]",
            Pattern.DOTALL);

    private HttpServer server;
    private final AtomicReference<String> cuerpoRecibido = new AtomicReference<>();
    private final AtomicReference<String> claveRecibida = new AtomicReference<>();
    private final AtomicInteger estado = new AtomicInteger(200);
    private final AtomicReference<String> respuesta = new AtomicReference<>();
    private final List<String> rutas = new ArrayList<>();
    private CotizadorPorHttp cotizador;

    @BeforeEach
    void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            rutas.add(
                    exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
            cuerpoRecibido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            claveRecibida.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            byte[] raw = respuesta.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estado.get(), raw.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(raw);
            }
        });
        server.start();
        cotizador = new CotizadorPorHttp(
                RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "GENERAL",
                Duration.ofSeconds(2));
    }

    @AfterEach
    void cerrar() {
        server.stop(0);
    }

    private static List<String> tiposQueTarifasAdmite() throws IOException {
        Path actual = Path.of("").toAbsolutePath();
        while (actual != null
                && !Files.isRegularFile(actual.resolve("servicios/tarifas/src/main/resources/openapi/tarifas.yaml"))) {
            actual = actual.getParent();
        }
        String yaml = Files.readString(
                actual.resolve("servicios/tarifas/src/main/resources/openapi/tarifas.yaml"), StandardCharsets.UTF_8);
        var m = ENUM_TIPO.matcher(yaml);
        assertThat(m.find())
                .as("no se encontro referenciaTipo en EntradaCotizacion del contrato de tarifas")
                .isTrue();
        return Arrays.stream(m.group(1).split(",")).map(String::trim).toList();
    }

    private static final String SALIDA =
            """
            {"cotizacionId":"%s","montoComision":{"monto":"4.00","moneda":"BOB"},
             "montoImpuesto":{"monto":"1.00","moneda":"BOB"},"montoTotal":{"monto":"5.00","moneda":"BOB"},
             "desglose":[],"validaHasta":"2026-10-08T15:00:00Z","esNueva":true}
            """;

    @Test
    @DisplayName(
            "contrato: el referenciaTipo que se manda es uno de los que el OpenAPI de tarifas admite, para cada hecho")
    void referenciaTipoValidoSegunElContratoDeTarifas() throws IOException {
        List<String> admitidos = tiposQueTarifasAdmite();
        respuesta.set(SALIDA.formatted(UUID.randomUUID()));
        for (String hecho : new String[] {"RECARGA", "RETIRO_ACREDITADO", "OTRO_HECHO"}) {
            cotizador.cotizar(
                    hecho,
                    UUID.randomUUID(),
                    Dinero.de("100.00", Moneda.BOB),
                    UUID.randomUUID().toString());
            String tipo = cuerpoRecibido.get().replaceAll(".*\"referenciaTipo\":\"([A-Z_]+)\".*", "$1");
            assertThat(admitidos).as("referenciaTipo enviado para %s", hecho).contains(tipo);
        }
    }

    @Test
    @DisplayName("correcto: la respuesta de tarifas se traduce a base, comision, impuesto, total y vigencia")
    void respuestaCorrecta() {
        UUID id = UUID.randomUUID();
        respuesta.set(SALIDA.formatted(id));
        String clave = UUID.randomUUID().toString();

        var cotizacion = cotizador
                .cotizar("RECARGA", UUID.randomUUID(), Dinero.de("500.00", Moneda.BOB), clave)
                .orElseThrow();

        assertThat(cotizacion.id()).contains(id);
        assertThat(cotizacion.comision()).isEqualTo(Dinero.de("4.00", Moneda.BOB));
        assertThat(cotizacion.impuesto()).isEqualTo(Dinero.de("1.00", Moneda.BOB));
        assertThat(cotizacion.total()).isEqualTo(Dinero.de("5.00", Moneda.BOB));
        assertThat(cotizacion.base()).isEqualTo(Dinero.de("500.00", Moneda.BOB));
        assertThat(cotizacion.validaHasta()).isPresent();
        assertThat(cotizacion.gratuita()).isFalse();
        assertThat(claveRecibida.get()).isEqualTo(clave);
        assertThat(cotizador.costoDe("RECARGA", UUID.randomUUID(), Dinero.de("500.00", Moneda.BOB), clave))
                .contains(Dinero.de("5.00", Moneda.BOB));
    }

    @Test
    @DisplayName(
            "limite: sin concepto en el tarifario (AP-CU30-01 gratuita) el precio es cero; es un precio, no una falla")
    void sinConceptoEsGratuita() {
        estado.set(422);
        respuesta.set(
                "{\"codigo\":\"AP-CU30-01\",\"mensaje\":\"x\",\"detalle\":{\"gratuita\":\"true\"},\"trazaId\":\"t\"}");

        var cotizacion = cotizador
                .cotizar(
                        "RECARGA",
                        UUID.randomUUID(),
                        Dinero.de("500.00", Moneda.BOB),
                        UUID.randomUUID().toString())
                .orElseThrow();

        assertThat(cotizacion.gratuita()).isTrue();
        assertThat(cotizacion.id()).isEmpty();
        assertThat(cotizacion.total()).isEqualTo(Dinero.cero(Moneda.BOB));
    }

    @Test
    @DisplayName(
            "invalido: cualquier otra respuesta de error, un cuerpo ilegible o un servidor caido es «no se pudo saber»")
    void loDemasEsDesconocido() {
        estado.set(500);
        respuesta.set("{}");
        assertThat(cotizador.cotizar("RECARGA", UUID.randomUUID(), Dinero.de("5.00", Moneda.BOB), "k"))
                .isEmpty();
        estado.set(422);
        respuesta.set("{\"codigo\":\"AP-CU30-02\"}");
        assertThat(cotizador.cotizar("RECARGA", UUID.randomUUID(), Dinero.de("5.00", Moneda.BOB), "k"))
                .isEmpty();
        estado.set(200);
        respuesta.set("no es json");
        assertThat(cotizador.cotizar("RECARGA", UUID.randomUUID(), Dinero.de("5.00", Moneda.BOB), "k"))
                .isEmpty();
        server.stop(0);
        assertThat(cotizador.cotizar("RECARGA", UUID.randomUUID(), Dinero.de("5.00", Moneda.BOB), "k"))
                .isEmpty();
    }

    @Test
    @DisplayName("la aceptacion va a la ruta del contrato y solo cuenta si tarifas responde aceptada=true")
    void aceptacion() {
        UUID id = UUID.randomUUID();
        respuesta.set("{\"aceptada\":true}");
        assertThat(cotizador.aceptar(id)).isTrue();
        assertThat(rutas).contains("POST /comisiones/cotizaciones/" + id + "/aceptacion");
        respuesta.set("{\"aceptada\":false}");
        assertThat(cotizador.aceptar(id)).isFalse();
        estado.set(422);
        respuesta.set("{\"codigo\":\"AP-CU30-03\"}");
        assertThat(cotizador.aceptar(id)).as("vencida").isFalse();
        server.stop(0);
        assertThat(cotizador.aceptar(id)).as("sin respuesta").isFalse();
    }
}
