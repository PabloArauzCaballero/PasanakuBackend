package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.infraestructura.clientes.HechosPorHttp;
import bo.aportaya.plataforma.dominio.Moneda;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
 * Carril Q · «tiene obligaciones abiertas» y «comprometido en pozos» por USUARIO, resueltos a traves de las participaciones
 * de {@code grupos} y del estado de pagos de {@code aportes} POR PARTICIPANTE. Antes se le preguntaba a {@code aportes} con el
 * usuarioId, que no indexa nada (indexa por participante_id de grupos): contestaba «al dia» siempre y la billetera de quien
 * debia se podia cerrar. Tres niveles (regla 65): correcto, limite, invalido; los servicios remotos son dobles de contrato
 * (rutas y campos del OpenAPI de {@code grupos} y de {@code aportes}); NO esta cubierto el proceso real de cada uno.
 */
class HechosPorHttpPorUsuarioTest {

    private final UUID usuario = UUID.randomUUID();
    private final Map<String, String> cuerpos = new ConcurrentHashMap<>();
    private final Map<String, Integer> estados = new ConcurrentHashMap<>();
    private final List<String> pedidas = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private HechosPorHttp hechos;

    @BeforeEach
    void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String ruta = exchange.getRequestURI().getPath();
            pedidas.add(ruta);
            byte[] raw = cuerpos.getOrDefault(ruta, "{}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(estados.getOrDefault(ruta, 200), raw.length);
            try (var salida = exchange.getResponseBody()) {
                salida.write(raw);
            }
        });
        server.start();
        String url = "http://127.0.0.1:" + server.getAddress().getPort();
        hechos = new HechosPorHttp(RestClient.builder(), url, url);
        sesionDe(usuario);
    }

    @AfterEach
    void cerrar() {
        SecurityContextHolder.clearContext();
        server.stop(0);
    }

    private static void sesionDe(UUID quien) {
        SecurityContextHolder.getContext()
                .setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .subject(quien.toString())
                        .build()));
    }

    private UUID participa(String... estadosDePagos) {
        cuerpos.clear();
        var elementos = new java.util.ArrayList<String>();
        for (String estado : estadosDePagos) {
            UUID participante = UUID.randomUUID();
            elementos.add("{\"grupoId\":\"%s\",\"participanteId\":\"%s\",\"estado\":\"ACTIVO\"}"
                    .formatted(UUID.randomUUID(), participante));
            cuerpos.put("/aportes/participantes/" + participante + "/estado", estado);
        }
        cuerpos.put("/grupos/participaciones", "[" + String.join(",", elementos) + "]");
        return usuario;
    }

    private static String pagos(int abiertas, String deuda, String porAportar) {
        return "{\"alDia\":%s,\"totalAportado\":\"0.00\",\"deudaVigente\":\"%s\",\"porAportar\":\"%s\",\"obligacionesAbiertas\":%d,\"moneda\":\"BOB\"}"
                .formatted(abiertas == 0, deuda, porAportar, abiertas);
    }

    @Test
    @DisplayName(
            "correcto: con una deuda abierta en UNA de sus dos participaciones, tieneObligacionesAbiertas es true y lo"
                    + " comprometido suma las dos")
    void conDeudaAbierta() {
        participa(pagos(0, "0.00", "0.00"), pagos(2, "300.00", "1200.00"));

        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB).orElseThrow().toString())
                .isEqualTo("1500.00");
        // La regresion: nada de lo pedido a aportes lleva el usuarioId.
        assertThat(pedidas).noneMatch(r -> r.contains(usuario.toString()));
        assertThat(pedidas).contains("/grupos/participaciones");
    }

    @Test
    @DisplayName(
            "correcto: al dia en todas sus participaciones no tiene obligaciones abiertas y lo comprometido es cero")
    void sinDeuda() {
        participa(pagos(0, "0.00", "0.00"), pagos(0, "0.00", "0.00"));

        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isFalse();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB).orElseThrow().esCero())
                .isTrue();
    }

    @Test
    @DisplayName(
            "limite: una persona sin participaciones no debe nada (no se le pidio nada), y eso es distinto de no saber")
    void sinParticipaciones() {
        cuerpos.put("/grupos/participaciones", "[]");

        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isFalse();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB).orElseThrow().esCero())
                .isTrue();
        assertThat(pedidas).containsExactly("/grupos/participaciones", "/grupos/participaciones");
    }

    @Test
    @DisplayName(
            "limite: plata comprometida en OTRA moneda no se suma ni se convierte: lo comprometido se omite (vacio)")
    void otraMoneda() {
        participa(pagos(1, "100.00", "0.00"));

        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.USD)).isEmpty();
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
    }

    @Test
    @DisplayName(
            "invalido: la sesion no es la del titular (un operador, un trabajo) · Entonces no se sabe: se asume que"
                    + " tiene obligaciones, se omite lo comprometido y NO se llama a nadie")
    void sesionAjena() {
        participa(pagos(0, "0.00", "0.00"));
        sesionDe(UUID.randomUUID());

        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
        SecurityContextHolder.clearContext();
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(pedidas).isEmpty();
    }

    @Test
    @DisplayName(
            "invalido: grupos caido o ilegible, aportes caido en una participacion, cuerpo sin conteo o importe mal"
                    + " formado · Entonces tiene obligaciones (cerrado) y lo comprometido se omite, nunca cero")
    void dependidosCaidos() {
        // grupos 500
        participa(pagos(0, "0.00", "0.00"));
        estados.put("/grupos/participaciones", 500);
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
        estados.clear();

        // grupos ilegible
        cuerpos.put("/grupos/participaciones", "esto no es json");
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();

        // aportes 500 en la segunda participacion
        participa(pagos(0, "0.00", "0.00"), pagos(0, "0.00", "0.00"));
        var rutaDeAportes = cuerpos.keySet().stream()
                .filter(r -> r.startsWith("/aportes/"))
                .sorted()
                .toList();
        estados.put(rutaDeAportes.get(1), 500);
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
        estados.clear();

        // aportes responde 200 sin el conteo, o con importes que no son importes
        participa("{}");
        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        participa("{\"deudaVigente\":\"abc\",\"porAportar\":\"1.001\",\"obligacionesAbiertas\":0,\"moneda\":\"BOB\"}");
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
    }

    @Test
    @DisplayName("invalido: con TODO apagado (el servidor no contesta) tiene obligaciones y no compromete un numero")
    void todoApagado() {
        participa(pagos(0, "0.00", "0.00"));
        server.stop(0);

        assertThat(hechos.tieneObligacionesAbiertas(usuario)).isTrue();
        assertThat(hechos.comprometidoEnPozos(usuario, Moneda.BOB)).isEmpty();
    }
}
