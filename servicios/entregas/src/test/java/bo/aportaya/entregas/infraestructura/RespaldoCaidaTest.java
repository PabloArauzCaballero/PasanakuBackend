package bo.aportaya.entregas.infraestructura;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import bo.aportaya.plataforma.pruebas.Espera;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Regla 98.2.3, 98.8.3 y 98.8.4 · la caida de `garantia` ejercitada de verdad contra {@link RespaldoPorHttp}.
 * FALLBACK declarado: vacio («no se sabe si cubrio, se reintenta»); con el circuito abierto NO sale ninguna peticion y,
 * al volver, el reintento usa la MISMA clave de idempotencia del turno: una sola cobertura, no dos.
 */
class RespaldoCaidaTest {

    private static final UUID GRUPO = UUID.fromString("f6200000-0000-4000-8000-000000000002");
    private static final UUID PERIODO = UUID.fromString("f6200000-0000-4000-8000-000000000001");
    private static final UUID TURNO = UUID.fromString("f6200000-0000-4000-8000-000000000003");
    private static final int VENTANA = 10;
    private static final long ESPERA_MS = 400;
    private static final String COBERTURA =
            """
            {"resultado":"APLICADA","coberturaId":"f6200000-0000-4000-8000-000000000004","reservaId":null,
             "cubierto":{"monto":"1000.00","moneda":"BOB"},"sinCubrir":{"monto":"0.00","moneda":"BOB"},
             "disponibleDespues":{"monto":"7000.00","moneda":"BOB"},"exposicionDespues":{"monto":"1000.00","moneda":"BOB"},
             "esNueva":true}
            """;

    private static RestClient.Builder conTiempos() {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return RestClient.builder().requestFactory(fabrica);
    }

    private static RespaldoPorHttp contra(String url, int ventana) {
        return new RespaldoPorHttp(
                conTiempos(), url, new ConfiguracionDeCircuito(ventana, 50, Duration.ofMillis(ESPERA_MS), 3));
    }

    private static Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName(
            "la caida de garantia abre el circuito; abierto NO llama aunque garantia ya volvio, y el circuito se recupera solo")
    void caidaAbreYSeRecupera() {
        try (var garantia = new DependidoDeEnsayo().responder(200, COBERTURA)) {
            var adaptador = contra(garantia.urlBase(), VENTANA);
            garantia.caer();

            for (int i = 0; i < VENTANA; i++) {
                assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")))
                        .as("llamada %s con garantia caida", i)
                        .isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);

            garantia.levantar();
            assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isEmpty();
            assertThat(garantia.peticionesRecibidas())
                    .as("circuito abierto: no sale peticion")
                    .isZero();

            Espera.hasta(
                    Duration.ofSeconds(3),
                    Duration.ofMillis(50),
                    "primera llamada de prueba tras la espera",
                    () -> adaptador
                            .cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))
                            .isPresent());
            assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isPresent();
            assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isPresent();
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
        }
    }

    private static HttpServer servidorQueAnotaLaClave(int puerto, List<String> claves) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 0);
        servidor.createContext("/", intercambio -> {
            claves.add(intercambio.getRequestHeaders().getFirst("Idempotency-Key"));
            byte[] bytes = COBERTURA.getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, bytes.length);
            intercambio.getResponseBody().write(bytes);
            intercambio.close();
        });
        servidor.start();
        return servidor;
    }

    @Test
    @DisplayName("la caida no duplica la cobertura: todos los reintentos del turno llevan la MISMA Idempotency-Key")
    void laCaidaNoDuplicaLaCobertura() throws IOException {
        List<String> claves = new CopyOnWriteArrayList<>();
        HttpServer garantia = servidorQueAnotaLaClave(0, claves);
        int puerto = garantia.getAddress().getPort();
        HttpServer vuelto = null;
        try {
            var adaptador = contra("http://127.0.0.1:" + puerto, 4);

            assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isPresent();
            garantia.stop(0);
            for (int i = 0; i < 4; i++) {
                assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00")))
                        .isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
            // Con el circuito abierto el reintento no sale: nada que garantia pueda registrar dos veces.
            assertThat(adaptador.cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))).isEmpty();
            assertThat(claves).hasSize(1);

            // Vuelve en el MISMO puerto (lo que hace un orquestador) y se reintenta tras la espera.
            vuelto = servidorQueAnotaLaClave(puerto, claves);
            Espera.hasta(Duration.ofSeconds(3), Duration.ofMillis(50), "el reintento llega a garantia", () -> adaptador
                    .cubrir(GRUPO, PERIODO, TURNO, bob("1000.00"))
                    .isPresent());

            String claveDelTurno = UUID.nameUUIDFromBytes(
                            ("cobertura-del-turno:" + TURNO).getBytes(StandardCharsets.UTF_8))
                    .toString();
            assertThat(claves).hasSizeGreaterThanOrEqualTo(2).containsOnly(claveDelTurno);
        } finally {
            garantia.stop(0);
            if (vuelto != null) {
                vuelto.stop(0);
            }
        }
    }
}
