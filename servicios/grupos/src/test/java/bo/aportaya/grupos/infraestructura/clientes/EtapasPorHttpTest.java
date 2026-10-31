package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * La etapa de una o varias personas segun {@code transparencia} (2026-10-30), contra un servidor HTTP real del JDK que
 * hace de transparencia. DOBLE declarado: no es transparencia, es su contrato ({@code listarEtapas} y
 * {@code consultarEtapasDePersonas}, [CONTRATO · SOLO SERVICIO] con {@code SERVICIO_GRUPOS}) en los tres niveles.
 * Catalogo SINTETICO de tres tramos. Lo que se fija: la etapa de una persona se pide con el token de SERVICIO, nunca con
 * el de la sesion; nunca se lee {@code /reputacion/{id}/puntaje}; y ante cualquier duda la respuesta es vacia (quien
 * decide un ingreso no deja pasar).
 */
class EtapasPorHttpTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private static final String TOKEN_DE_SERVICIO = "token-de-la-cuenta-de-servicio-de-grupos";
    private static final String DE_PERSONAS = "/reputacion/etapas/de-personas";
    private static final String CATALOGO =
            """
            [{"codigo":"MEDIA","nombre":"Etapa 1 · Media","orden":1,"desde":"500.00","hasta":"700.00",
              "desbloqueo":{"cuposMax":10,"aporteMax":"500.00","moneda":"BOB","estado":"VIGENTE"}},
             {"codigo":"BAJA","nombre":"En recuperacion","orden":0,"desde":"0.00","hasta":"500.00"},
             {"codigo":"ALTA","nombre":"Etapa 2 · Alta","orden":2,"desde":"700.00","hasta":"1000.00"}]
            """;

    private HttpServer servidor;
    private String url;
    private final Map<String, String[]> respuestas = new ConcurrentHashMap<>();

    /** Lo que llego a la ruta de personas: la cabecera Authorization y los usuarioId de la query, por pedido. */
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();

    private final List<List<String>> idsPedidos = new CopyOnWriteArrayList<>();
    private final List<String> rutas = new CopyOnWriteArrayList<>();

    /** Si no es nulo, el doble contesta la ruta de personas sola con la etapa de cada id pedido (eco). */
    private volatile String etapaEco;

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath();
            rutas.add(ruta);
            String[] r = respuestas.getOrDefault(ruta, new String[] {"404", "{}"});
            if (DE_PERSONAS.equals(ruta)) {
                String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
                autorizaciones.add(autorizacion == null ? "" : autorizacion);
                List<String> ids = ids(intercambio.getRequestURI().getRawQuery());
                idsPedidos.add(ids);
                // Como la guardia de transparencia: la ruta de servicio solo acepta la identidad de servicio de grupos.
                if (!("Bearer " + TOKEN_DE_SERVICIO).equals(autorizacion)) {
                    r = new String[] {"403", "{}"};
                } else if (etapaEco != null) {
                    r = new String[] {"200", eco(ids, etapaEco)};
                }
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
        url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        sesionConToken("token-de-la-sesion");
    }

    @AfterEach
    void bajar() {
        if (servidor != null) servidor.stop(0);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    private HechosPorHttp hechos(CredencialDeServicio credencial) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(Duration.ofMillis(400));
        return new HechosPorHttp(
                RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url, credencial);
    }

    private HechosPorHttp hechos() {
        return hechos(CredencialDeServicio.fija(TOKEN_DE_SERVICIO));
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

    private void catalogo(String estado, String cuerpo) {
        respuestas.put("/reputacion/etapas", new String[] {estado, cuerpo});
    }

    private void personas(String estado, String cuerpo) {
        respuestas.put(DE_PERSONAS, new String[] {estado, cuerpo});
    }

    private static String persona(UUID id, boolean historial, String codigo, String desde) {
        return "{\"usuarioId\":\"%s\",\"tieneHistorial\":%s,\"etapa\":{\"codigo\":\"%s\",\"nombre\":\"Etapa\",\"orden\":1,\"desde\":\"%s\"}}"
                .formatted(id, historial, codigo, desde);
    }

    private static List<String> ids(String query) {
        if (query == null) return List.of();
        return Arrays.stream(query.split("&"))
                .filter(p -> p.startsWith("usuarioId="))
                .map(p -> URLDecoder.decode(p.substring("usuarioId=".length()), StandardCharsets.UTF_8))
                .toList();
    }

    private static String eco(List<String> ids, String codigo) {
        return ids.stream()
                .map(id -> persona(UUID.fromString(id), true, codigo, "500.00"))
                .collect(Collectors.joining(",", "[", "]"));
    }

    // --- correcto ---

    @Test
    void laEtapaDeUnaPersonaSePideConElTokenDeServicioYNuncaElPuntaje() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        var persona = hechos().etapaDe(USUARIO).orElseThrow();

        assertThat(persona.etapa().codigo()).isEqualTo("MEDIA");
        // Grupos no conoce el puntaje exacto: compara con el piso de la etapa (tramos contiguos).
        assertThat(persona.puntaje()).isEqualByComparingTo("500.00");
        assertThat(persona.etapa().desbloqueo().vigente()).isTrue();
        assertThat(persona.catalogo()).extracting(a -> a.codigo()).containsExactly("BAJA", "MEDIA", "ALTA");
        assertThat(autorizaciones).containsExactly("Bearer " + TOKEN_DE_SERVICIO);
        assertThat(idsPedidos).containsExactly(List.of(USUARIO.toString()));
        assertThat(rutas).noneMatch(r -> r.endsWith("/puntaje"));
    }

    @Test
    void elDetallePideLasEtapasDeTodosEnUnaSolaConsultaYTraeCodigoYNombre() {
        UUID otra = UUID.randomUUID();
        personas(
                "200",
                "[" + persona(USUARIO, true, "MEDIA", "500.00") + ","
                        + "{\"usuarioId\":\"" + otra + "\",\"tieneHistorial\":false,"
                        + "\"etapa\":{\"codigo\":\"ALTA\",\"nombre\":\"Etapa 2 · Alta\",\"orden\":2,\"desde\":\"700.00\"}}]");

        var etapas = hechos().etapasDe(List.of(USUARIO, otra)).orElseThrow();

        assertThat(etapas).hasSize(2);
        assertThat(etapas.get(otra).codigo()).isEqualTo("ALTA");
        assertThat(etapas.get(otra).nombre()).isEqualTo("Etapa 2 · Alta");
        assertThat(idsPedidos).hasSize(1);
        assertThat(idsPedidos.get(0)).containsExactly(USUARIO.toString(), otra.toString());
        // El catalogo no hace falta para el detalle: la ruta de servicio ya trae codigo y nombre.
        assertThat(rutas).containsExactly(DE_PERSONAS);
    }

    @Test
    void elCatalogoSeLeeSinPreguntarPorNingunaPersona() {
        catalogo("200", CATALOGO);
        assertThat(hechos().catalogoDeEtapas().orElseThrow())
                .extracting(a -> a.codigo())
                .containsExactly("BAJA", "MEDIA", "ALTA");
        assertThat(autorizaciones).isEmpty();
    }

    // --- limite ---

    @Test
    void sinHistorialCuentaElPisoDeLaEtapaBaseNoUnCero() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, false, "MEDIA", "500.00") + "]");

        var persona = hechos().etapaDe(USUARIO).orElseThrow();

        assertThat(persona.puntaje()).isEqualByComparingTo("500.00");
        assertThat(persona.etapa().codigo()).isEqualTo("MEDIA");
    }

    @Test
    void masDeSesentaPersonasVanEnTandasDeSesentaYLasRepetidasUnaVez() {
        etapaEco = "MEDIA";
        List<UUID> pedidos = new ArrayList<>(
                IntStream.range(0, 61).mapToObj(i -> UUID.randomUUID()).toList());
        pedidos.add(pedidos.get(0));
        Collections.shuffle(pedidos.subList(1, 61));

        var etapas = hechos().etapasDe(pedidos).orElseThrow();

        assertThat(etapas).hasSize(61);
        assertThat(idsPedidos).extracting(List::size).containsExactly(60, 1);
    }

    @Test
    void quienNoTieneEtapaEnLaRespuestaQuedaAfueraSinTirarALosDemas() {
        UUID sinEtapa = UUID.randomUUID();
        personas(
                "200",
                "[" + persona(USUARIO, true, "MEDIA", "500.00") + ",{\"usuarioId\":\"" + sinEtapa
                        + "\",\"tieneHistorial\":false}]");

        var etapas = hechos().etapasDe(List.of(USUARIO, sinEtapa)).orElseThrow();

        assertThat(etapas).containsOnlyKeys(USUARIO);
    }

    @Test
    void sinNadieQuePreguntarNoSaleNingunaPeticion() {
        assertThat(hechos().etapasDe(List.of()).orElseThrow()).isEmpty();
        assertThat(rutas).isEmpty();
    }

    // --- invalido ---

    @Test
    void sinCredencialDeServicioNoSaleNingunaPeticionYLaRespuestaEsVacia() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        for (CredencialDeServicio sin : List.of(CredencialDeServicio.ninguna(), CredencialDeServicio.fija("  "))) {
            assertThat(hechos(sin).etapaDe(USUARIO)).isEmpty();
            assertThat(hechos(sin).etapasDe(List.of(USUARIO))).isEmpty();
        }
        assertThat(autorizaciones).as("ni siquiera con el token de la sesion").isEmpty();
    }

    @Test
    void conUnTokenQueTransparenciaNoAceptaComoServicioLaRespuestaEsVacia() {
        catalogo("200", CATALOGO);
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        assertThat(hechos(CredencialDeServicio.fija("token-de-otra-cosa")).etapaDe(USUARIO))
                .isEmpty();
    }

    @Test
    void caidoLentoIlegibleOConUnaEtapaQueNoEstaEnElCatalogoEsVacio() {
        catalogo("200", CATALOGO);
        var hechos = hechos();

        personas("503", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("5xx").isEmpty();
        personas("200", "[" + persona(USUARIO, true, "OTRA", "500.00") + "]");
        assertThat(hechos.etapaDe(USUARIO)).as("etapa desconocida").isEmpty();
        personas("200", "{\"no\":\"es una lista\"}");
        assertThat(hechos.etapasDe(List.of(USUARIO))).as("cuerpo ilegible").isEmpty();
        personas("200", "[" + persona(UUID.randomUUID(), true, "MEDIA", "500.00") + "]");
        assertThat(hechos.etapasDe(List.of(USUARIO)))
                .as("alguien que no se pidio")
                .isEmpty();
        respuestas.put(
                DE_PERSONAS, new String[] {"200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]", "1200"});
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia lenta").isEmpty();

        servidor.stop(0);
        assertThat(hechos.etapaDe(USUARIO)).as("red caida").isEmpty();
        servidor = null;
    }

    @Test
    void sinCatalogoVacioOIlegibleLaEtapaParaDecidirEsVacia() {
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");
        var hechos = hechos();

        catalogo("404", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia sin catalogo").isEmpty();
        catalogo("200", "[]");
        assertThat(hechos.etapaDe(USUARIO)).as("catalogo vacio").isEmpty();
        catalogo("200", CATALOGO.replace("\"500.00\",\"moneda\"", "\"quinientos\",\"moneda\""));
        assertThat(hechos.etapaDe(USUARIO)).as("un importe que no es decimal").isEmpty();
        catalogo("500", "{}");
        assertThat(hechos.etapaDe(USUARIO)).as("transparencia con 5xx").isEmpty();
    }

    @Test
    void elDesbloqueoPropuestoLlegaPeroNoEsVigente() {
        catalogo("200", CATALOGO.replace("\"VIGENTE\"", "\"PROPUESTO\""));
        personas("200", "[" + persona(USUARIO, true, "MEDIA", "500.00") + "]");

        var desbloqueo = hechos().etapaDe(USUARIO).orElseThrow().etapa().desbloqueo();

        assertThat(desbloqueo.vigente()).isFalse();
        assertThat(desbloqueo.cuposMax()).isEqualTo(10);
        assertThat(new BigDecimal("500.00")).isEqualByComparingTo(desbloqueo.aporteMax());
    }
}
