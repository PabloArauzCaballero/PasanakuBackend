package bo.aportaya.nucleofinanciero.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.nucleofinanciero.dominio.puertos.CotizadorDeComision;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import bo.aportaya.plataforma.pruebas.BaseDePrueba;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * H4.S2.M4 · {@code @Retry}/{@code @CircuitBreaker} de verdad sobre {@code CotizadorPorHttp},
 * no solo declarados.
 *
 * <p>Arranca el contexto COMPLETO (mismo patron que {@code ArranqueTest}: es la unica
 * forma de que el proxy AOP de resilience4j este de verdad puesto — un
 * {@code new CotizadorPorHttp(...)} a mano, como usa el resto de las pruebas de este
 * modulo, NO pasa por el proxy y no probaria nada) contra un {@code HttpServer} del
 * JDK que hace de {@code tarifas}, igual que {@code EmisorDeEvidenciaDePrueba} hace de
 * JWKS para H2 — mismo patron, otra dependencia externa.
 */
// WebEnvironment.RANDOM_PORT, no NONE: TodoEndpointDecideSuAcceso (comun-web) exige un
// RequestMappingHandlerMapping para arrancar — mismo motivo que ArranqueProduccionTest.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CotizadorResilienciaTest {

    private static HttpServer servidor;
    private static final AtomicInteger LLAMADAS = new AtomicInteger();

    /** Cuantas veces seguidas responder 500 antes de responder 200 (o siempre, si -1). */
    private static volatile int fallosAntesDeOk = -1;

    @BeforeAll
    static void levantarTarifasFalso() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/comisiones/cotizaciones", exchange -> {
            int intento = LLAMADAS.incrementAndGet();
            byte[] cuerpo;
            int status;
            if (fallosAntesDeOk < 0 || intento <= fallosAntesDeOk) {
                status = 503;
                cuerpo = "{\"codigo\":\"AP-CU30-99\"}".getBytes(StandardCharsets.UTF_8);
            } else {
                status = 200;
                cuerpo =
                        """
                        {"montoTotal":{"monto":"12.50","moneda":"BOB"}}
                        """
                                .getBytes(StandardCharsets.UTF_8);
            }
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, cuerpo.length);
            try (OutputStream salida = exchange.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.start();
    }

    @AfterAll
    static void pararTarifasFalso() {
        servidor.stop(0);
    }

    @DynamicPropertySource
    static void configuracion(DynamicPropertyRegistry registro) {
        var contenedor = BaseDePrueba.contenedor();
        registro.add("spring.datasource.url", contenedor::getJdbcUrl);
        registro.add("spring.datasource.username", contenedor::getUsername);
        registro.add("spring.datasource.password", contenedor::getPassword);
        registro.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registro.add("aportaya.jwt.jwks-uri", () -> "http://identidad:8080/.well-known/jwks.json");
        registro.add("SEGURIDAD_PIMIENTA", () -> "pimienta-de-prueba");
        registro.add("CUENTA_PUENTE_CUSTODIA", () -> "00000000-0000-0000-0000-0000000000c0");
        registro.add("aportaya.servicios.tarifas", () -> "http://127.0.0.1:" + puerto());
    }

    private static int puerto() {
        // DynamicPropertySource corre ANTES de @BeforeAll: el servidor todavia no
        // existe en ese momento. Se resuelve perezoso, en el momento en que Spring
        // realmente LEE la property (al construir CotizadorPorHttp), no antes.
        return servidor != null ? servidor.getAddress().getPort() : 0;
    }

    @Autowired
    private CotizadorDeComision cotizador;

    @Autowired
    private CircuitBreakerRegistry cortacircuitos;

    @BeforeEach
    void reiniciarElCortacircuitos() {
        // Los dos @Test de esta clase comparten el MISMO contexto de Spring (cache de
        // @SpringBootTest) y, con el, la MISMA instancia de CircuitBreaker "cotizador"
        // — sin este reinicio, los 3 fallos de un test cuentan para la ventana
        // deslizante del otro, y cual corre primero (JUnit no lo garantiza) decide si
        // el circuito ya esta a medio abrir cuando el segundo test empieza.
        cortacircuitos.circuitBreaker("cotizador").reset();
    }

    private Dinero bob(String monto) {
        return Dinero.de(monto, Moneda.BOB);
    }

    @Test
    @DisplayName("tarifas cae SIEMPRE: reintenta 3 veces en total y despues cede — nunca cuelga al retiro")
    void reintentaYCedeSiTarifasNoLevanta() {
        LLAMADAS.set(0);
        fallosAntesDeOk = Integer.MAX_VALUE;

        Optional<Dinero> resultado =
                cotizador.costoDe("RETIRO", UUID.randomUUID(), bob("100.00"), "cotiz-resiliencia-1");

        assertThat(resultado).isEmpty();
        // max-attempts: 3 en application.yml — el intento original + 2 reintentos, ni
        // uno mas: un cliente que reintenta sin limite es la misma caida disfrazada.
        assertThat(LLAMADAS.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("tarifas falla dos veces y la tercera contesta: el reintento RECUPERA la cotizacion")
    void seRecuperaAlTercerIntento() {
        LLAMADAS.set(0);
        fallosAntesDeOk = 2;

        Optional<Dinero> resultado =
                cotizador.costoDe("RETIRO", UUID.randomUUID(), bob("100.00"), "cotiz-resiliencia-2");

        assertThat(resultado).isPresent();
        assertThat(resultado.get()).isEqualByComparingTo(bob("12.50"));
        assertThat(LLAMADAS.get()).isEqualTo(3);
    }
}
