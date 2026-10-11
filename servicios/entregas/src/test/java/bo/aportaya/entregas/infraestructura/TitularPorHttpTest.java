package bo.aportaya.entregas.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Carril I (quinta vuelta) · CU-18, el adaptador {@link TitularPorHttp} contra una identidad DE ENSAYO con la forma publicada
 * ({@code POST /usuarios/{id}/titularidad/verificacion} que contesta {@code {coincide}}), HTTP real del JDK.
 *
 * <p>Lo que fija: QUIEN firma la pregunta (la credencial de SERVICIO, no el token del participante, que no tiene
 * {@code DATOS_SENSIBLES_LEER} y por eso su 403 se leia como «no coincide»), que el veredicto sale de lo que identidad
 * CONTESTO, y que todo lo demas (sin credencial, 403, 500, cuerpo ilegible, identidad caida) es «no» sin adivinar. La
 * integracion contra identidad de verdad vive en {@code despliegue/ensayos/integracion}.
 */
class TitularPorHttpTest {

    private static final UUID PARTICIPANTE = UUID.fromString("f1800000-0000-4000-8000-000000000001");
    private static final String TOKEN_DEL_PARTICIPANTE = "token-del-participante";
    private static final String TOKEN_DE_SERVICIO = "token-de-servicio-entregas";

    private record Pedido(String metodo, String ruta, String autorizacion, String cuerpo) {}

    private HttpServer identidad;
    private final List<Pedido> recibidos = new CopyOnWriteArrayList<>();
    private volatile int estado = 200;
    private volatile String respuesta = "{\"coincide\":true}";
    private boolean detenida;

    @BeforeEach
    void levantar() throws IOException {
        identidad = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        identidad.createContext("/", intercambio -> {
            recibidos.add(new Pedido(
                    intercambio.getRequestMethod(),
                    intercambio.getRequestURI().getPath(),
                    intercambio.getRequestHeaders().getFirst("Authorization"),
                    new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(estado, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        identidad.start();
        sesionDelParticipante();
    }

    @AfterEach
    void apagar() {
        SecurityContextHolder.clearContext();
        detener();
    }

    private void detener() {
        if (!detenida) {
            detenida = true;
            identidad.stop(0);
        }
    }

    private static void sesionDelParticipante() {
        Jwt jwt = Jwt.withTokenValue(TOKEN_DEL_PARTICIPANTE)
                .header("alg", "none")
                .subject(PARTICIPANTE.toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private TitularPorHttp adaptador(CredencialDeServicio credencial) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(500);
        fabrica.setReadTimeout(500);
        return new TitularPorHttp(
                RestClient.builder().requestFactory(fabrica),
                "http://127.0.0.1:" + identidad.getAddress().getPort(),
                credencial);
    }

    @Test
    @DisplayName(
            "correcto: la pregunta va firmada con la credencial de SERVICIO (no con el token del participante) y el"
                    + " veredicto es lo que identidad contesto")
    void firmaConLaCredencialDeServicio() {
        var adaptador = adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));

        assertThat(adaptador.esElMismo(PARTICIPANTE, "Ana Quispe", "1234567")).isTrue();

        assertThat(recibidos).hasSize(1);
        var p = recibidos.get(0);
        assertThat(p.metodo()).isEqualTo("POST");
        assertThat(p.ruta()).isEqualTo("/usuarios/" + PARTICIPANTE + "/titularidad/verificacion");
        assertThat(p.autorizacion()).isEqualTo("Bearer " + TOKEN_DE_SERVICIO);
        assertThat(p.autorizacion()).doesNotContain(TOKEN_DEL_PARTICIPANTE);
        assertThat(p.cuerpo()).contains("\"nombreCompleto\":\"Ana Quispe\"").contains("\"documento\":\"1234567\"");

        respuesta = "{\"coincide\":false}";
        assertThat(adaptador.esElMismo(PARTICIPANTE, "Otra Persona", "1234567")).isFalse();
    }

    @Test
    @DisplayName("rechaza: sin credencial de servicio NO sale ninguna peticion y el veredicto es no (falla cerrado)")
    void sinCredencialNoPregunta() {
        assertThat(adaptador(CredencialDeServicio.ninguna()).esElMismo(PARTICIPANTE, "Ana Quispe", "1234567"))
                .isFalse();
        assertThat(recibidos).isEmpty();
    }

    @Test
    @DisplayName(
            "rechaza: si identidad no admite a la credencial (403), responde 500, un cuerpo ilegible o esta caida, el"
                    + " veredicto es no")
    void todoLoQueNoSeaUnSiEsNo() {
        var adaptador = adaptador(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));

        estado = 403;
        respuesta = "{\"codigo\":\"AP-SEG-01\"}";
        assertThat(adaptador.esElMismo(PARTICIPANTE, "Ana Quispe", "1234567")).isFalse();

        estado = 500;
        respuesta = "{}";
        assertThat(adaptador.esElMismo(PARTICIPANTE, "Ana Quispe", "1234567")).isFalse();

        estado = 200;
        respuesta = "esto no es json";
        assertThat(adaptador.esElMismo(PARTICIPANTE, "Ana Quispe", "1234567")).isFalse();

        // Identidad caida (el puerto deja de aceptar, como un contenedor apagado).
        detener();
        assertThat(adaptador.esElMismo(PARTICIPANTE, "Ana Quispe", "1234567")).isFalse();
    }
}
