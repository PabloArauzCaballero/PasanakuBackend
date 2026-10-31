package bo.aportaya.grupos.infraestructura.clientes;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Un {@code transparencia} de ensayo por HTTP real (servidor del JDK) para pruebas de otros paquetes (2026-10-30). DOBLE
 * declarado del contrato {@code listarEtapas} y {@code consultarEtapasDePersonas}: catalogo SINTETICO de tres tramos
 * (BAJA 0–500, MEDIA 500–700, ALTA 700–1000) y la etapa que la prueba le asigna a cada persona. Como la guardia de
 * transparencia, la ruta de personas solo acepta el token de servicio {@link #TOKEN}. Entrega el {@link HechosPorHttp}
 * REAL apuntado a el, con esa credencial de servicio.
 */
public final class HechosDeEnsayo {

    public static final String TOKEN = "token-sintetico-de-servicio-de-grupos";

    private static final String CATALOGO =
            """
            [{"codigo":"BAJA","nombre":"En recuperacion","orden":0,"desde":"0.00","hasta":"500.00"},
             {"codigo":"MEDIA","nombre":"Etapa 1 · Media","orden":1,"desde":"500.00","hasta":"700.00"},
             {"codigo":"ALTA","nombre":"Etapa 2 · Alta","orden":2,"desde":"700.00","hasta":"1000.00"}]
            """;

    private final HttpServer servidor;
    private final HechosPorHttp hechos;
    private final Map<UUID, String[]> etapas = new ConcurrentHashMap<>();
    private final List<String> autorizaciones = new CopyOnWriteArrayList<>();
    private final List<String> rutas = new CopyOnWriteArrayList<>();
    private volatile boolean rechazarTodo;
    private volatile boolean abajo;

    private HechosDeEnsayo(HttpServer servidor) {
        this.servidor = servidor;
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(Duration.ofMillis(400));
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        this.hechos = new HechosPorHttp(
                RestClient.builder().requestFactory(fabrica),
                url,
                url,
                url,
                url,
                url,
                url,
                url,
                url,
                CredencialDeServicio.fija(TOKEN));
    }

    public static HechosDeEnsayo levantar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HechosDeEnsayo ensayo = new HechosDeEnsayo(servidor);
        servidor.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath();
            ensayo.rutas.add(ruta);
            String estado = "404";
            String cuerpo = "{}";
            if ("/reputacion/etapas".equals(ruta)) {
                estado = "200";
                cuerpo = CATALOGO;
            } else if ("/reputacion/etapas/de-personas".equals(ruta)) {
                String autorizacion = intercambio.getRequestHeaders().getFirst("Authorization");
                ensayo.autorizaciones.add(autorizacion == null ? "" : autorizacion);
                if (ensayo.rechazarTodo || !("Bearer " + TOKEN).equals(autorizacion)) {
                    estado = "403";
                } else {
                    estado = "200";
                    cuerpo = ensayo.respuesta(intercambio.getRequestURI().getRawQuery());
                }
            }
            byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(Integer.parseInt(estado), bytes.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(bytes);
            }
        });
        servidor.start();
        return ensayo;
    }

    private String respuesta(String query) {
        return java.util.Arrays.stream(query == null ? new String[0] : query.split("&"))
                .filter(p -> p.startsWith("usuarioId="))
                .map(p -> UUID.fromString(p.substring("usuarioId=".length())))
                .map(id -> {
                    String[] e = etapas.get(id);
                    return e == null
                            ? "{\"usuarioId\":\"" + id + "\",\"tieneHistorial\":false}"
                            : "{\"usuarioId\":\"%s\",\"tieneHistorial\":true,\"etapa\":{\"codigo\":\"%s\",\"nombre\":\"Etapa\",\"orden\":1,\"desde\":\"%s\"}}"
                                    .formatted(id, e[0], e[1]);
                })
                .collect(Collectors.joining(",", "[", "]"));
    }

    /** El {@link HechosPorHttp} real, apuntado a este transparencia y con la credencial de servicio {@link #TOKEN}. */
    public HechosDeOtrosServicios hechos() {
        return hechos;
    }

    public void etapaDe(UUID usuarioId, String codigo, String desde) {
        etapas.put(usuarioId, new String[] {codigo, desde});
    }

    /** Desde ahora la ruta de personas responde 403 a cualquiera (no reconoce la identidad de servicio). */
    public void rechazarTodoToken() {
        rechazarTodo = true;
    }

    public List<String> autorizacionesDePersonas() {
        return List.copyOf(autorizaciones);
    }

    public boolean pidioUnPuntaje() {
        return rutas.stream().anyMatch(r -> r.endsWith("/puntaje"));
    }

    /** Apaga el servidor: transparencia caida. Idempotente. */
    public void bajar() {
        if (!abajo) {
            abajo = true;
            servidor.stop(0);
        }
    }
}
