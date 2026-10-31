package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.plataforma.dominio.ErrorDeDominio;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Lo que `grupos` sabe de la cuota 1, preguntado por el contrato de `aportes` contra un servidor HTTP real (el del JDK)
 * que hace de «aportes». Doble declarado, en los tres niveles: la respuesta correcta, los bordes (sin obligaciones, cuerpo
 * incompleto, demora) y lo invalido (4xx, 5xx). Lo que se fija es que ante la duda se devuelve vacio y CU-60 deniega.
 */
class PrimerAportePorHttpTest {
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID CUPO_A = UUID.randomUUID();
    private static final UUID CUPO_B = UUID.randomUUID();
    private static final UUID CUPO_C = UUID.randomUUID();
    private static final String RUTA = "/aportes/grupos/" + GRUPO + "/primera-cuota";

    private HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private PrimerAportePorHttp aportes;

    record Respuesta(int estado, String cuerpo, long demoraMs) {
        static Respuesta ok(String json) {
            return new Respuesta(200, json, 0);
        }

        static Respuesta estado(int codigo) {
            return new Respuesta(codigo, "{}", 0);
        }
    }

    @BeforeEach
    void levantar() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/", intercambio -> {
            var r = respuestas.getOrDefault(intercambio.getRequestURI().getPath(), Respuesta.estado(404));
            try {
                if (r.demoraMs() > 0) Thread.sleep(r.demoraMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] cuerpo = r.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(r.estado(), cuerpo.length);
            try (var salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(400));
        fabrica.setReadTimeout(Duration.ofMillis(400));
        aportes = new PrimerAportePorHttp(
                RestClient.builder().requestFactory(fabrica),
                "http://127.0.0.1:" + servidor.getAddress().getPort());
    }

    @AfterEach
    void bajar() {
        servidor.stop(0);
    }

    @Test
    @DisplayName("Correcto: aportes dice que dos de tres cupos pagaron · se devuelven exactamente esos dos")
    void correcto() {
        respuestas.put(
                RUTA,
                Respuesta.ok(
                        """
                        {"grupoId":"%s","periodoId":"%s","cuposPagados":["%s","%s"],"cuposPendientes":["%s"]}
                        """
                                .formatted(GRUPO, UUID.randomUUID(), CUPO_A, CUPO_B, CUPO_C)));

        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).contains(java.util.Set.of(CUPO_A, CUPO_B));
    }

    @Test
    @DisplayName(
            "Limite: el grupo todavia no tiene obligaciones · listas vacias y periodo nulo se leen como «nadie pago»")
    void limiteSinObligaciones() {
        respuestas.put(
                RUTA,
                Respuesta.ok("{\"grupoId\":\"" + GRUPO
                        + "\",\"periodoId\":null,\"cuposPagados\":[],\"cuposPendientes\":[]}"));

        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).contains(java.util.Set.of());
    }

    @Test
    @DisplayName("Limite: un cuerpo sin las listas no se completa con suposiciones · vacio")
    void limiteCuerpoIncompleto() {
        respuestas.put(RUTA, Respuesta.ok("{\"grupoId\":\"" + GRUPO + "\"}"));

        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).isEmpty();
    }

    @Test
    @DisplayName("Limite: aportes tarda mas que el tiempo maximo · vacio, no se espera para siempre")
    void limiteDemora() {
        respuestas.put(
                RUTA,
                new Respuesta(200, "{\"grupoId\":\"" + GRUPO + "\",\"cuposPagados\":[],\"cuposPendientes\":[]}", 1500));

        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).isEmpty();
    }

    @Test
    @DisplayName("Invalido: aportes responde 404 o 403 · vacio")
    void invalido4xx() {
        respuestas.put(RUTA, Respuesta.estado(403));
        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).isEmpty();

        respuestas.remove(RUTA);
        assertThat(aportes.cuposConPrimeraCuotaPagada(GRUPO)).isEmpty();
    }

    @Test
    @DisplayName("Invalido: aportes responde 500 · error de dominio, como el resto de los hechos remotos")
    void invalido5xx() {
        respuestas.put(RUTA, Respuesta.estado(500));

        assertThatThrownBy(() -> aportes.cuposConPrimeraCuotaPagada(GRUPO)).isInstanceOf(ErrorDeDominio.class);
    }
}
