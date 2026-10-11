package bo.aportaya.grupos.infraestructura.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import bo.aportaya.grupos.dominio.puertos.HechosDeOtrosServicios;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Lo que este servicio sabe de los otros, preguntado por sus contratos, contra un servidor HTTP real (el del JDK)
 * que hace de «el otro servicio». Doble declarado: no es identidad, organizador ni aportes; es su contrato en los
 * tres niveles. Lo que se fija es que ante la duda se deniega o se declara SIN_DATO, y nunca se supone lo bueno.
 */
class HechosPorHttpTest {
    private static final UUID USUARIO = UUID.randomUUID();
    private static final UUID ORGANIZADOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private HechosPorHttp hechos;

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
        hechos = construir("http://127.0.0.1:" + servidor.getAddress().getPort(), Duration.ofMillis(400));
    }

    @AfterEach
    void bajar() {
        try {
            servidor.stop(0);
        } catch (RuntimeException yaBajado) {
            // el caso de «servicio caido» ya lo bajo
        }
    }

    private static HechosPorHttp construir(String url, Duration lectura) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(300));
        fabrica.setReadTimeout(lectura);
        return new HechosPorHttp(RestClient.builder().requestFactory(fabrica), url, url, url, url, url, url, url, url);
    }

    private String rutaHabilitacion() {
        return "/organizadores/usuarios/" + USUARIO + "/habilitacion";
    }

    // --- correcto ---

    @Test
    void organizadorHabilitadoSeResuelveDesdeLaRespuestaDelServicioDeOrganizadores() {
        respuestas.put(
                rutaHabilitacion(),
                Respuesta.ok("{\"usuarioId\":\"%s\",\"organizadorId\":\"%s\",\"habilitado\":true}"
                        .formatted(USUARIO, ORGANIZADOR)));
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).contains(ORGANIZADOR);
    }

    @Test
    void nivelDeKycYMorososSeLeenDeSusServicios() {
        respuestas.put("/usuarios/" + USUARIO + "/kyc", Respuesta.ok("{\"nivel\":\"INTERMEDIO\"}"));
        respuestas.put("/aportes/grupos/" + GRUPO + "/morosos", Respuesta.ok("{\"morosos\":1}"));
        assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("INTERMEDIO");
        assertThat(hechos.morososDelGrupo(GRUPO)).isEqualTo(1);
    }

    // --- limite ---

    @Test
    void habilitadoFalsoYRespuestaDeOtraPersonaNoHabilitan() {
        respuestas.put(
                rutaHabilitacion(),
                Respuesta.ok("{\"usuarioId\":\"%s\",\"organizadorId\":\"%s\",\"habilitado\":false}"
                        .formatted(USUARIO, ORGANIZADOR)));
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();

        // Una respuesta que habla de OTRO usuario no vale para este, aunque diga habilitado=true.
        respuestas.put(
                rutaHabilitacion(),
                Respuesta.ok("{\"usuarioId\":\"%s\",\"organizadorId\":\"%s\",\"habilitado\":true}"
                        .formatted(UUID.randomUUID(), ORGANIZADOR)));
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();
    }

    @Test
    void sinRecursoEnElOtroServicioSeDeniegaOSeDeclaraSinDato() {
        // 404: nadie sabe nada de esta persona o grupo.
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();
        assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
        assertThat(hechos.morososDelGrupo(GRUPO)).isEqualTo(HechosDeOtrosServicios.SIN_DATO_DE_MOROSOS);
        var reputacion = hechos.reputacion(USUARIO);
        assertThat(reputacion.tieneHistorial()).isFalse();
        assertThat(hechos.restriccion(USUARIO).vigente()).isTrue();
    }

    // --- invalido ---

    @Test
    void servicioCaidoOLentoNuncaSeInterpretaComoRespuestaBuena() {
        respuestas.put(
                rutaHabilitacion(),
                new Respuesta(
                        200,
                        "{\"usuarioId\":\"%s\",\"organizadorId\":\"%s\",\"habilitado\":true}"
                                .formatted(USUARIO, ORGANIZADOR),
                        1500));
        respuestas.put("/usuarios/" + USUARIO + "/kyc", new Respuesta(200, "{\"nivel\":\"COMPLETO\"}", 1500));
        // Timeout de lectura: la respuesta tardia no cuenta, aunque fuera afirmativa.
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();
        assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");

        // Conexion rechazada: el servicio no esta.
        servidor.stop(0);
        assertThat(hechos.nivelDeKyc(USUARIO)).isEqualTo("NINGUNO");
        assertThat(hechos.morososDelGrupo(GRUPO)).isEqualTo(HechosDeOtrosServicios.SIN_DATO_DE_MOROSOS);
        assertThat(hechos.organizadorHabilitadoDelUsuario(USUARIO)).isEmpty();
    }

    @Test
    void errorDelOtroServicioSePropagaYNoSeTomaPorPermiso() {
        respuestas.put(rutaHabilitacion(), Respuesta.estado(500));
        respuestas.put("/usuarios/" + USUARIO + "/kyc", Respuesta.estado(503));
        assertThatThrownBy(() -> hechos.organizadorHabilitadoDelUsuario(USUARIO))
                .isInstanceOf(ErrorDeDominio.class);
        assertThatThrownBy(() -> hechos.nivelDeKyc(USUARIO)).isInstanceOf(ErrorDeDominio.class);
    }

    @Test
    void cuerpoIlegibleNoSeInterpretaComoPermiso() {
        respuestas.put(rutaHabilitacion(), Respuesta.ok("esto no es json"));
        respuestas.put("/usuarios/" + USUARIO + "/kyc", Respuesta.ok("<html>proxy</html>"));
        assertThatThrownBy(() -> hechos.organizadorHabilitadoDelUsuario(USUARIO))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> hechos.nivelDeKyc(USUARIO)).isInstanceOf(RuntimeException.class);
    }
}
