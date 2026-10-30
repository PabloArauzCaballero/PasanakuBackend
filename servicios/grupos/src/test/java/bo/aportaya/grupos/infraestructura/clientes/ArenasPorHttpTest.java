package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * La arena de una persona segun {@code transparencia} (2026-10-30), contra un servidor HTTP real del JDK que hace de
 * transparencia. DOBLE declarado: no es transparencia, es su contrato ({@code listarArenas} y {@code consultarPuntaje}
 * con {@code arena}) en los tres niveles. Catalogo SINTETICO de tres tramos. Lo que se fija: sin historial cuenta el piso
 * de la arena base, y ante cualquier duda la respuesta es vacia (quien pregunta no deja pasar).
 */
class ArenasPorHttpTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final String CATALOGO =
            """
            [{"codigo":"MEDIA","nombre":"Media","orden":1,"desde":"500.00","hasta":"700.00",
              "desbloqueo":{"cuposMax":10,"aporteMax":"500.00","moneda":"BOB","estado":"VIGENTE"}},
             {"codigo":"BAJA","nombre":"Baja","orden":0,"desde":"0.00","hasta":"500.00"},
             {"codigo":"ALTA","nombre":"Alta","orden":2,"desde":"700.00","hasta":"1000.00"}]
            """;

    private HttpServer servidor;
    private final Map<String, String[]> respuestas = new ConcurrentHashMap<>();
    private HechosPorHttp hechos;

    /** Si no es nulo, el doble contesta 403 a todo token que no sea este (como la guardia de transparencia). */
    private volatile String unicoTokenAceptado;

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String[] r = respuestas.getOrDefault(intercambio.getRequestURI().getPath(), new String[] {"404", "{}"});
            String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
            if (unicoTokenAceptado != null && !("Bearer " + unicoTokenAceptado).equals(autorizacion)) {
                r = new String[] {"403", "{}"};
            }
            if (r.length > 2) {
                // Una transparencia LENTA: contesta bien, pero despues del tiempo maximo de lectura del cliente.
                try {
                    Thread.sleep(Long.parseLong(r[2]));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] cuerpo = r[1].getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(Integer.parseInt(r[0]), cuerpo.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        // Un hilo por pedido: un pedido lento no hace esperar en cola al siguiente (que venceria por eso, no por si).
        servidor.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        servidor.start();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(Duration.ofMillis(400));
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        hechos =
                new HechosPorHttp(RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url);
    }

    @AfterEach
    void bajar() {
        if (servidor != null) servidor.stop(0);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private static void sesionConToken(String valor) {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue(valor)
                .header("alg", "none")
                .subject(UUID.randomUUID().toString())
                .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(
                        new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                                jwt));
    }

    // --- quien pregunta: el token de la sesion viaja; transparencia decide con SU guardia ---

    @Test
    void elTokenDeLaSesionViajaATransparencia() {
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"MEDIA\"}}");
        unicoTokenAceptado = "token-de-quien-pregunta";
        sesionConToken("token-de-quien-pregunta");

        assertThat(hechos.arenaDe(USUARIO)).isPresent();
    }

    @Test
    void siTransparenciaNoLeRespondeAEseRol403LaRespuestaEsVaciaYNoSeDejaPasar() {
        // Hoy listarArenas y consultarPuntaje exigen @Permiso("PARTICIPANTE") (ReputacionController.java:95 y :118):
        // una sesion de backoffice sin ese rol recibe 403. grupos falla cerrado; lo que lo destraba es que
        // transparencia acepte a backoffice en esas dos lecturas (carril A), no suponer una arena aca.
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"MEDIA\"}}");
        unicoTokenAceptado = "token-de-participante";
        sesionConToken("token-de-backoffice");

        assertThat(hechos.arenaDe(USUARIO)).isEmpty();
    }

    private void catalogo(String estado, String cuerpo) {
        respuestas.put("/reputacion/arenas", new String[] {estado, cuerpo});
    }

    private void puntaje(String estado, String cuerpo) {
        respuestas.put("/reputacion/" + USUARIO + "/puntaje", new String[] {estado, cuerpo});
    }

    // --- correcto ---

    @Test
    void conHistorialCuentaSuPuntajeYSuArenaConLoQueDesbloquea() {
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"MEDIA\"}}");

        var persona = hechos.arenaDe(USUARIO).orElseThrow();

        assertThat(persona.puntaje()).isEqualByComparingTo("612.40");
        assertThat(persona.arena().codigo()).isEqualTo("MEDIA");
        assertThat(persona.arena().desbloqueo().vigente()).isTrue();
        assertThat(persona.arena().desbloqueo().aporteMax()).isEqualByComparingTo("500.00");
        // El catalogo llega desordenado y se usa por `orden`.
        assertThat(persona.catalogo()).extracting(a -> a.codigo()).containsExactly("BAJA", "MEDIA", "ALTA");
    }

    @Test
    void sinHistorialCuentaElPisoDeLaArenaBaseNoUnCero() {
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":false,\"puntaje\":\"0\",\"arena\":{\"codigo\":\"MEDIA\"}}");

        var persona = hechos.arenaDe(USUARIO).orElseThrow();

        assertThat(persona.puntaje()).isEqualByComparingTo("500.00");
        assertThat(persona.arena().codigo()).isEqualTo("MEDIA");
    }

    // --- limite ---

    @Test
    void sinCampoArenaYConHistorialSeUbicaElPuntajeEnElCatalogoConSusBordes() {
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"700.00\"}");
        assertThat(hechos.arenaDe(USUARIO).orElseThrow().arena().codigo())
                .as("desde incluido")
                .isEqualTo("ALTA");

        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"699.99\"}");
        assertThat(hechos.arenaDe(USUARIO).orElseThrow().arena().codigo())
                .as("hasta excluido")
                .isEqualTo("MEDIA");

        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"1000.00\"}");
        assertThat(hechos.arenaDe(USUARIO).orElseThrow().arena().codigo())
                .as("la ultima incluye su hasta")
                .isEqualTo("ALTA");
    }

    // --- invalido ---

    @Test
    void sinHistorialYSinCampoArenaNoSeInventaNada() {
        catalogo("200", CATALOGO);
        puntaje("200", "{\"tieneHistorial\":false,\"puntaje\":\"0\"}");
        assertThat(hechos.arenaDe(USUARIO)).isEmpty();
    }

    @Test
    void sinCatalogoVacioIlegibleOCaidoLaRespuestaEsVacia() {
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"MEDIA\"}}");

        catalogo("404", "{}");
        assertThat(hechos.arenaDe(USUARIO)).as("transparencia sin catalogo").isEmpty();
        catalogo("200", "[]");
        assertThat(hechos.arenaDe(USUARIO)).as("catalogo vacio").isEmpty();
        catalogo("200", CATALOGO.replace("\"500.00\",\"moneda\"", "\"quinientos\",\"moneda\""));
        assertThat(hechos.arenaDe(USUARIO)).as("un importe que no es decimal").isEmpty();
        catalogo("500", "{}");
        assertThat(hechos.arenaDe(USUARIO)).as("transparencia con 5xx").isEmpty();
    }

    @Test
    void puntajeCaidoOConUnaArenaQueNoEstaEnElCatalogoEsVacio() {
        catalogo("200", CATALOGO);

        puntaje("503", "{}");
        assertThat(hechos.arenaDe(USUARIO)).as("5xx").isEmpty();
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"OTRA\"}}");
        assertThat(hechos.arenaDe(USUARIO)).as("arena desconocida").isEmpty();
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"no-es-numero\"}");
        assertThat(hechos.arenaDe(USUARIO)).as("puntaje ilegible").isEmpty();

        servidor.stop(0);
        assertThat(hechos.arenaDe(USUARIO)).as("red caida").isEmpty();
        servidor = null;
    }

    @Test
    void transparenciaLentaVenceElTiempoMaximoYLaRespuestaEsVacia() {
        // El cliente lee con 400 ms de tope (como el de produccion, ConfiguracionDeClientes, pero corto): una respuesta
        // correcta que llega a los 1200 ms no cuenta, y quien pregunta no deja pasar (falla cerrado).
        respuestas.put("/reputacion/arenas", new String[] {"200", CATALOGO, "1200"});
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\",\"arena\":{\"codigo\":\"MEDIA\"}}");
        assertThat(hechos.arenaDe(USUARIO)).as("catalogo lento").isEmpty();

        catalogo("200", CATALOGO);
        respuestas.put(
                "/reputacion/" + USUARIO + "/puntaje",
                new String[] {"200", "{\"tieneHistorial\":true,\"puntaje\":\"612.40\"}", "1200"});
        assertThat(hechos.arenaDe(USUARIO)).as("puntaje lento").isEmpty();
    }

    @Test
    void elDesbloqueoPropuestoLlegaPeroNoEsVigente() {
        catalogo("200", CATALOGO.replace("\"VIGENTE\"", "\"PROPUESTO\""));
        puntaje("200", "{\"tieneHistorial\":true,\"puntaje\":\"600.00\"}");

        var desbloqueo = hechos.arenaDe(USUARIO).orElseThrow().arena().desbloqueo();

        assertThat(desbloqueo.vigente()).isFalse();
        assertThat(desbloqueo.cuposMax()).isEqualTo(10);
        assertThat(new BigDecimal("500.00")).isEqualByComparingTo(desbloqueo.aporteMax());
    }
}
