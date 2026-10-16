package bo.aportaya.entregas.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Lo comun de las pruebas de {@link FondosPorHttp}: un nucleo-financiero DE ENSAYO con la forma publicada en
 * {@code nucleo-financiero.yaml} (HTTP real del JDK). Las pruebas viven en {@link FondosPorHttpTest} (correcto y limite) y
 * {@link FondosPorHttpInvalidoTest}.
 *
 * <p>El adaptador {@link FondosPorHttp} contra un nucleo-financiero DE ENSAYO con la forma publicada en
 * {@code nucleo-financiero.yaml} (HTTP real del JDK), en tres niveles. Lado CONSUMIDOR: lo que sale (ruta, cuerpo,
 * quien firma, clave de idempotencia) y como se lee lo que vuelve. El productor real lo ejerce el script de
 * integracion de {@code despliegue/ensayos/integracion} contra el stack compose.
 */
abstract class BaseDeFondosPorHttp {

    static final UUID COMPRADOR = UUID.fromString("f9000000-0000-4000-8000-000000000001");
    static final UUID OTRA_PERSONA = UUID.fromString("f9000000-0000-4000-8000-000000000002");
    static final UUID VENDEDOR = UUID.fromString("f9000000-0000-4000-8000-000000000003");
    static final UUID OFERTA = UUID.fromString("f9000000-0000-4000-8000-000000000004");
    static final UUID CUENTA = UUID.fromString("f9000000-0000-4000-8000-000000000005");
    static final UUID RETENCION = UUID.fromString("f9000000-0000-4000-8000-000000000006");
    static final UUID TRANSACCION = UUID.fromString("f9000000-0000-4000-8000-000000000007");
    static final String TOKEN_DEL_COMPRADOR = "token-del-comprador";
    static final String TOKEN_DE_SERVICIO = "token-de-servicio-entregas";

    record Pedido(String metodo, String ruta, String consulta, String autorizacion, String clave, String cuerpo) {}

    record Respuesta(int estado, String cuerpo) {}

    HttpServer servidor;
    final List<Pedido> recibidos = new CopyOnWriteArrayList<>();
    final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/", intercambio -> {
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String ruta = intercambio.getRequestURI().getPath();
            recibidos.add(new Pedido(
                    intercambio.getRequestMethod(),
                    ruta,
                    intercambio.getRequestURI().getQuery(),
                    intercambio.getRequestHeaders().getFirst("Authorization"),
                    intercambio.getRequestHeaders().getFirst("Idempotency-Key"),
                    cuerpo));
            var r = respuestas.getOrDefault(intercambio.getRequestMethod() + " " + ruta, new Respuesta(404, "{}"));
            byte[] bytes = r.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(r.estado(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                intercambio.getResponseBody().write(bytes);
            }
            intercambio.close();
        });
        servidor.start();
        // El nucleo sano: cuenta, retencion, pago y cierre.
        respuestas.put(
                "GET /billetera/cuentas/propia",
                new Respuesta(200, "{\"cuentaId\":\"" + CUENTA + "\",\"moneda\":\"BOB\",\"estado\":\"ACTIVA\"}"));
        respuestas.put(
                "POST /billetera/retenciones",
                new Respuesta(
                        201,
                        "{\"retencionId\":\"" + RETENCION
                                + "\",\"saldoDisponible\":{\"monto\":\"4500.00\",\"moneda\":\"BOB\"},"
                                + "\"saldoRetenido\":{\"monto\":\"5500.00\",\"moneda\":\"BOB\"}}"));
        respuestas.put(
                "POST /billetera/retenciones/" + RETENCION + "/pago-a-tercero",
                new Respuesta(
                        200,
                        "{\"transaccionId\":\"" + TRANSACCION
                                + "\",\"saldoDisponible\":{\"monto\":\"4500.00\",\"moneda\":\"BOB\"},"
                                + "\"saldoRetenido\":{\"monto\":\"0.00\",\"moneda\":\"BOB\"}}"));
        respuestas.put(
                "POST /billetera/retenciones/" + RETENCION + "/cierre",
                new Respuesta(
                        200,
                        "{\"retencionId\":\"" + RETENCION
                                + "\",\"estado\":\"LIBERADA\",\"saldoDisponible\":{\"monto\":\"10000.00\","
                                + "\"moneda\":\"BOB\"},\"saldoRetenido\":{\"monto\":\"0.00\",\"moneda\":\"BOB\"}}"));
    }

    @AfterEach
    void apagar() {
        SecurityContextHolder.clearContext();
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    FondosPorHttp adaptador(CredencialDeServicio credencial) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(500);
        fabrica.setReadTimeout(500);
        return new FondosPorHttp(
                RestClient.builder().requestFactory(fabrica),
                "http://127.0.0.1:" + servidor.getAddress().getPort(),
                ConfiguracionDeCircuito.porOmision(),
                credencial);
    }

    FondosPorHttp adaptador() {
        return adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));
    }

    static void sesionDe(UUID persona) {
        Jwt jwt = Jwt.withTokenValue(TOKEN_DEL_COMPRADOR)
                .header("alg", "none")
                .subject(persona.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }
}
