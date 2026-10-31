package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
abstract class BaseDeEtapasPorHttp {

    protected static final UUID USUARIO = UUID.randomUUID();
    protected static final String TOKEN_DE_SERVICIO = "token-de-la-cuenta-de-servicio-de-grupos";
    protected static final String DE_PERSONAS = "/reputacion/etapas/de-personas";
    protected static final String CATALOGO =
            """
            [{"codigo":"MEDIA","nombre":"Etapa 1 · Media","orden":1,"desde":"500.00","hasta":"700.00",
              "desbloqueo":{"cuposMax":10,"aporteMax":"500.00","moneda":"BOB","estado":"VIGENTE"}},
             {"codigo":"BAJA","nombre":"En recuperacion","orden":0,"desde":"0.00","hasta":"500.00"},
             {"codigo":"ALTA","nombre":"Etapa 2 · Alta","orden":2,"desde":"700.00","hasta":"1000.00"}]
            """;

    protected HttpServer servidor;
    private String url;
    protected final Map<String, String[]> respuestas = new ConcurrentHashMap<>();

    /** Lo que llego a la ruta de personas: la cabecera Authorization y los usuarioId de la query, por pedido. */
    protected final List<String> autorizaciones = new CopyOnWriteArrayList<>();

    protected final List<List<String>> idsPedidos = new CopyOnWriteArrayList<>();
    protected final List<String> rutas = new CopyOnWriteArrayList<>();

    /** Si no es nulo, el doble contesta la ruta de personas sola con la etapa de cada id pedido (eco). */
    protected volatile String etapaEco;

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

    protected HechosPorHttp hechos(CredencialDeServicio credencial) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(Duration.ofMillis(400));
        return new HechosPorHttp(
                RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url, credencial);
    }

    protected HechosPorHttp hechos() {
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

    protected void catalogo(String estado, String cuerpo) {
        respuestas.put("/reputacion/etapas", new String[] {estado, cuerpo});
    }

    protected void personas(String estado, String cuerpo) {
        respuestas.put(DE_PERSONAS, new String[] {estado, cuerpo});
    }

    protected static String persona(UUID id, boolean historial, String codigo, String desde) {
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
}
