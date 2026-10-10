package bo.aportaya.nucleofinanciero;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Pacto con {@code inversiones}, LADO PRODUCTOR: el servidor REAL de nucleo-financiero (contexto completo de Spring,
 * HTTP de verdad, PostgreSQL real) recibe exactamente lo que dice {@code contratos/pactos/inversiones-nucleo-financiero.json}
 * y responde lo pactado. El token se verifica contra un JWKS real (servidor HTTP local con una clave RSA de la prueba),
 * por el mismo camino que en produccion, y tiene la forma que emite identidad: el rol de servicio viaja en
 * {@code permisos} y el reclamo {@code rol} es el piso de la sesion (BACKOFFICE). El lado consumidor del mismo archivo es
 * {@code LibroPorHttpPactoContratoTest} en inversiones (cliente real, servidor del JDK).
 *
 * <p>Doble declarado: {@code aportes} (lo comprometido en pozos), un servidor HTTP local con la forma del OpenAPI de aportes.
 * Identidad e inversiones NO estan en esta prueba: el token se firma aca con la forma de identidad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CU13PactoInversionesTest extends BaseDeInversiones {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @LocalServerPort
    private int puerto;

    private static IdentidadesDelPacto identidades;

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) throws Exception {
        identidades = new IdentidadesDelPacto();
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", identidades::urlDelJwks);
        registro.add("aportaya.servicios.aportes", identidades::urlDeAportes);
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba");
        registro.add("CUENTA_PUENTE_CUSTODIA", () -> "00000000-0000-0000-0000-0000000000c0");
        registro.add("QR_INTERNO_CLAVE", () -> "clave-de-prueba-de-los-qr-internos-0001");
    }

    @AfterAll
    static void apagar() {
        identidades.close();
    }

    // ---------------------------------------------------------------- el pacto
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

    private static JsonNode pacto() throws IOException {
        return JSON.readTree(Files.readString(
                raiz().resolve("contratos/pactos/inversiones-nucleo-financiero.json"), StandardCharsets.UTF_8));
    }

    private static String resolver(String texto, Map<String, String> v) {
        String r = texto;
        for (var e : v.entrySet()) {
            r = r.replace("{" + e.getKey() + "}", e.getValue());
        }
        return r;
    }

    private static JsonNode campo(JsonNode raizJson, String ruta) {
        JsonNode n = raizJson;
        for (String parte : ruta.split("\\.")) {
            n = n.path(parte);
        }
        return n;
    }

    private HttpResponse<String> enviar(JsonNode peticion, Map<String, String> v, UUID titular) throws Exception {
        String ruta = resolver(peticion.path("ruta").asText(), v);
        String quien = peticion.path("identidad").asText();
        String bearer = "SERVICIO".equals(quien)
                ? identidades.delServicio("SERVICIO_INVERSIONES")
                : identidades.delTitular(titular);
        var b = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + bearer);
        if ("POST".equals(peticion.path("metodo").asText())) {
            b.header("Content-Type", "application/json")
                    .header(
                            "Idempotency-Key",
                            v.get(peticion.path("clave").asText().replaceAll("[{}]", "")))
                    .POST(HttpRequest.BodyPublishers.ofString(
                            resolver(peticion.path("cuerpo").toString(), v)));
        } else {
            b.GET();
        }
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName(
            "cuadre: el servidor real responde lo pactado a cada peticion del pacto y el libro queda cuadrado (custodia en cero, titular con su saldo)")
    void elProductorCumpleElPacto() throws Exception {
        UUID titular = fixtura.usuario();
        UUID cuenta = fixtura.billetera(titular, "ESTANDAR", new BigDecimal("1000.00"));
        UUID custodia = sistemaFx.custodiaDeInversion("BOB");
        BigDecimal custodiaAntes = sistemaFx.saldoDelLibro(custodia);
        Map<String, String> v = new HashMap<>();
        v.put("cuenta", cuenta.toString());
        v.put("titular", titular.toString());
        for (String n : List.of("orden", "orden2", "orden3", "rescate")) {
            v.put(n, UUID.randomUUID().toString());
        }
        for (int i = 1; i <= 7; i++) {
            v.put("clave" + i, UUID.randomUUID().toString());
        }

        for (JsonNode interaccion : pacto().path("interacciones")) {
            String id = interaccion.path("id").asText();
            JsonNode esperado = interaccion.path("respuesta");
            var respuesta = enviar(interaccion.path("peticion"), v, titular);

            assertThat(respuesta.statusCode())
                    .as("%s: estado", id)
                    .isEqualTo(esperado.path("estado").asInt());
            if (respuesta.body() != null && !respuesta.body().isBlank()) {
                JsonNode cuerpo = JSON.readTree(respuesta.body());
                for (JsonNode c : esperado.path("campos")) {
                    assertThat(campo(cuerpo, c.asText()).isMissingNode()
                                    || campo(cuerpo, c.asText()).isNull())
                            .as("%s: falta el campo %s", id, c.asText())
                            .isFalse();
                }
                if (esperado.has("codigo")) {
                    assertThat(cuerpo.path("codigo").asText())
                            .as("%s: codigo", id)
                            .isEqualTo(esperado.get("codigo").asText());
                }
                for (var e : esperado.path("captura").properties()) {
                    v.put(e.getKey(), cuerpo.path(e.getValue().asText()).asText());
                }
                for (var e : esperado.path("igualA").properties()) {
                    assertThat(cuerpo.path(e.getKey()).asText())
                            .as(
                                    "%s: %s igual al capturado '%s'",
                                    id, e.getKey(), e.getValue().asText())
                            .isEqualTo(v.get(e.getValue().asText()));
                }
            }
            System.out.println("PACTO | " + id + " -> " + respuesta.statusCode());
        }

        // El libro cuadra: 1000 - 300 (inversion) + 300 (rescate) = 1000, la segunda retencion se libero, y la
        // custodia recibio 300 y devolvio 300.
        assertThat(sistemaFx.saldoDelLibro(cuenta)).isEqualByComparingTo("1000.00");
        assertThat(sistemaFx.saldoDelLibro(custodia)).isEqualByComparingTo(custodiaAntes);
        assertThat(contar("INVERSION", v.get("orden")))
                .as("una sola transaccion de inversion para la orden")
                .isEqualTo(1);
        assertThat(contar("RESCATE_INVERSION", v.get("rescate"))).isEqualTo(1);
        System.out.println("LIBRO | titular=" + sistemaFx.saldoDelLibro(cuenta) + " custodia(delta)="
                + sistemaFx.saldoDelLibro(custodia).subtract(custodiaAntes) + " | inversion=1 rescate=1");
    }

    private long contar(String tipo, String origen) {
        return ((Number) dsl.fetchOne(
                                "SELECT count(*) FROM nucleo_financiero.transaccion_billetera WHERE tipo = ? AND origen_id = ?::uuid",
                                tipo,
                                origen)
                        .get(0))
                .longValue();
    }

    @Test
    @DisplayName(
            "rechaza: sin token (401), con un token firmado por otra clave (401) y vencido (401); un servicio que no es inversiones no debita (403)")
    void identidadesQueNoEntran() throws Exception {
        String ruta = "http://127.0.0.1:" + puerto + "/billetera/inversiones/debitos";
        String cuerpo =
                "{\"retencionId\":\"" + UUID.randomUUID() + "\",\"monto\":{\"monto\":\"1.00\",\"moneda\":\"BOB\"}}";
        var firmas = new String[] {
            null,
            identidades.token(
                    false,
                    UUID.randomUUID(),
                    "BACKOFFICE",
                    List.of("SERVICIO_INVERSIONES"),
                    Instant.now().plusSeconds(600)),
            identidades.token(
                    true,
                    UUID.randomUUID(),
                    "BACKOFFICE",
                    List.of("SERVICIO_INVERSIONES"),
                    Instant.now().minusSeconds(120)),
        };
        for (String t : firmas) {
            var b = HttpRequest.newBuilder(URI.create(ruta))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo));
            if (t != null) {
                b.header("Authorization", "Bearer " + t);
            }
            assertThat(HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString())
                            .statusCode())
                    .isEqualTo(401);
        }
        String deEntregas = identidades.delServicio("SERVICIO_ENTREGAS");
        var r = HTTP.send(
                HttpRequest.newBuilder(URI.create(ruta))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer " + deEntregas)
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(r.statusCode()).isEqualTo(403);
    }
}
