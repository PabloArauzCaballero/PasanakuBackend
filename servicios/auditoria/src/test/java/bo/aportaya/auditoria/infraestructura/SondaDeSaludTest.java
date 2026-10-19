package bo.aportaya.auditoria.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.auditoria.dominio.SaludDeServicio;
import bo.aportaya.auditoria.dominio.SaludDeServicio.Estado;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * operacion/estado · la sonda contra un servidor HTTP REAL en loopback (no un mock del cliente): tres niveles del
 * contrato del readiness de Spring. Correcto: 200. Limite: un servicio que contesta despues del plazo (no espera de
 * mas y no frena a los otros). Invalido: 503 OUT_OF_SERVICE y un puerto donde no escucha nadie.
 */
class SondaDeSaludTest {

    private static final Duration PLAZO = Duration.ofMillis(400);

    private HttpServer servidor;
    private final CountDownLatch soltarAlLento = new CountDownLatch(1);

    @BeforeEach
    void levantar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/sano/", e -> responder(e, 200));
        servidor.createContext("/fuera/", e -> responder(e, 503));
        servidor.createContext("/lento/", e -> {
            try {
                soltarAlLento.await();
            } catch (InterruptedException ignorada) {
                Thread.currentThread().interrupt();
            }
            responder(e, 200);
        });
        servidor.start();
    }

    @AfterEach
    void bajar() {
        soltarAlLento.countDown();
        servidor.stop(0);
    }

    private static void responder(com.sun.net.httpserver.HttpExchange e, int estado) throws java.io.IOException {
        byte[] cuerpo = "{\"status\":\"x\"}".getBytes();
        e.sendResponseHeaders(estado, cuerpo.length);
        e.getResponseBody().write(cuerpo);
        e.close();
    }

    private SondaDeSalud sonda(String servicios) {
        String plantilla = "http://127.0.0.1:" + servidor.getAddress().getPort() + "/%s/actuator/health/readiness";
        return new SondaDeSalud(servicios, plantilla, PLAZO);
    }

    @Test
    @DisplayName("salud · 200 es ARRIBA con su latencia; 503 es DEGRADADO; en el orden de la configuración")
    void arribaYDegradado() {
        List<SaludDeServicio> r = sonda("sano, fuera").consultar();

        assertThat(r).extracting(SaludDeServicio::servicio).containsExactly("sano", "fuera");
        assertThat(r.get(0).estado()).isEqualTo(Estado.ARRIBA);
        assertThat(r.get(0).latenciaMs()).isPresent();
        assertThat(r.get(1).estado()).isEqualTo(Estado.DEGRADADO);
    }

    @Test
    @DisplayName("salud · el que no contesta a tiempo es CAIDO sin latencia, y no demora a los demás más que el plazo")
    void lentoEsCaido() {
        long inicio = System.nanoTime();
        List<SaludDeServicio> r = sonda("lento,sano").consultar();
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(r.get(0).estado()).isEqualTo(Estado.CAIDO);
        assertThat(r.get(0).latenciaMs()).isEmpty();
        assertThat(r.get(1).estado()).isEqualTo(Estado.ARRIBA);
        // En paralelo: el total es un plazo (+ margen), no la suma.
        assertThat(tardo).isLessThan(PLAZO.multipliedBy(3));
    }

    @Test
    @DisplayName("salud · donde no escucha nadie es CAIDO, no una excepción que tire la pantalla")
    void puertoCerradoEsCaido() throws Exception {
        int libre;
        try (ServerSocket s = new ServerSocket(0)) {
            libre = s.getLocalPort();
        }
        SondaDeSalud cerrada =
                new SondaDeSalud("nadie", "http://127.0.0.1:" + libre + "/%s/actuator/health/readiness", PLAZO);

        assertThat(cerrada.consultar()).singleElement().satisfies(r -> {
            assertThat(r.estado()).isEqualTo(Estado.CAIDO);
            assertThat(r.latenciaMs()).isEmpty();
        });
    }

    @Test
    @DisplayName("salud · sin servicios configurados no pregunta nada")
    void vacia() {
        assertThat(sonda(" ").consultar()).isEmpty();
    }
}
