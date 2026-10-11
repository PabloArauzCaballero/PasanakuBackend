package bo.aportaya.garantia.infraestructura;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import bo.aportaya.plataforma.pruebas.Espera;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Regla 98.2.3 y 98.8.4 · la caida de `aportes` ejercitada de verdad contra el cliente real {@link RecaudoPorHttp}
 * (el doble {@link DependidoDeEnsayo} se cae, vuelve y cuenta las peticiones que le llegan). FALLBACK declarado: vacio,
 * y la cobertura NO se aplica (AP-CU23-12: sin pozo afirmado no se cubre nada). Plan: evidencia/carril-S2/DEUDA-circuit-breaker-clientes-http.md.
 */
class RecaudoCaidaTest {

    private static final UUID PERIODO = UUID.fromString("e5100000-0000-4000-8000-000000000001");
    private static final int VENTANA = 10;
    private static final long ESPERA_MS = 400;
    private static final String RECAUDO =
            """
            {"periodoId":"e5100000-0000-4000-8000-000000000001","grupoId":"e5100000-0000-4000-8000-000000000002",
             "corteEn":"2026-10-08T12:00:00Z",
             "pozo":{"monto":"6000.00","moneda":"BOB"},"confirmado":{"monto":"5000.00","moneda":"BOB"},
             "cubiertoMutual":{"monto":"0.00","moneda":"BOB"},"faltante":{"monto":"1000.00","moneda":"BOB"},
             "pendientes":[]}
            """;

    private static RecaudoPorHttp contra(DependidoDeEnsayo aportes, int ventana) {
        var fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(300);
        fabrica.setReadTimeout(300);
        return new RecaudoPorHttp(
                RestClient.builder().requestFactory(fabrica),
                aportes.urlBase(),
                new ConfiguracionDeCircuito(ventana, 50, Duration.ofMillis(ESPERA_MS), 3));
    }

    @Test
    @DisplayName("CORRECTO · con aportes vivo responde el recaudo y el circuito sigue cerrado")
    void correcto() {
        try (var aportes = new DependidoDeEnsayo().responder(200, RECAUDO)) {
            var adaptador = contra(aportes, VENTANA);

            assertThat(adaptador.consultar(PERIODO)).isPresent();
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
        }
    }

    @Test
    @DisplayName("LIMITE · la caida abre el circuito tras la ventana; abierto, NO llama aunque aportes ya volvio")
    void caidaAbreYNoLlama() {
        try (var aportes = new DependidoDeEnsayo().responder(200, RECAUDO)) {
            var adaptador = contra(aportes, VENTANA);
            aportes.caer();

            for (int i = 0; i < VENTANA; i++) {
                assertThat(adaptador.consultar(PERIODO))
                        .as("llamada %s con aportes caido", i)
                        .isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);

            aportes.levantar();
            assertThat(adaptador.consultar(PERIODO)).isEmpty();
            assertThat(aportes.peticionesRecibidas())
                    .as("con el circuito abierto no sale ninguna peticion")
                    .isZero();
        }
    }

    @Test
    @DisplayName("LIMITE · tras la espera pasan llamadas de prueba y, si responden bien, el circuito cierra solo")
    void seRecuperaSolo() {
        try (var aportes = new DependidoDeEnsayo().responder(200, RECAUDO)) {
            var adaptador = contra(aportes, VENTANA);
            aportes.caer();
            for (int i = 0; i < VENTANA; i++) {
                adaptador.consultar(PERIODO);
            }
            aportes.levantar();

            Espera.hasta(
                    Duration.ofSeconds(3),
                    Duration.ofMillis(50),
                    "primera llamada de prueba tras la espera",
                    () -> adaptador.consultar(PERIODO).isPresent());
            assertThat(adaptador.consultar(PERIODO)).isPresent();
            assertThat(adaptador.consultar(PERIODO)).isPresent();

            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
        }
    }

    @Test
    @DisplayName("INVALIDO · 5xx y cuerpo ilegible cuentan como caida; un 404 o 403 no abre el circuito")
    void clasificaLosFallos() {
        try (var aportes = new DependidoDeEnsayo().responder(503, "{}")) {
            var adaptador = contra(aportes, VENTANA);
            for (int i = 0; i < VENTANA; i++) {
                assertThat(adaptador.consultar(PERIODO)).isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
        }
        try (var aportes = new DependidoDeEnsayo().responder(200, "no es json")) {
            var adaptador = contra(aportes, VENTANA);
            for (int i = 0; i < VENTANA; i++) {
                adaptador.consultar(PERIODO);
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
        }
        try (var aportes = new DependidoDeEnsayo().responder(404, "{}")) {
            var adaptador = contra(aportes, VENTANA);
            for (int i = 0; i < 3 * VENTANA; i++) {
                assertThat(adaptador.consultar(PERIODO)).isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
            assertThat(aportes.peticionesRecibidas()).isEqualTo(3 * VENTANA);
        }
    }

    @Test
    @DisplayName("INVALIDO · aportes lento abre el circuito y despues falla al instante, sin esperar la red")
    void lentoFallaAlInstante() {
        try (var aportes = new DependidoDeEnsayo().tardar(600).responder(200, RECAUDO)) {
            var adaptador = contra(aportes, 4);
            for (int i = 0; i < 4; i++) {
                assertThat(adaptador.consultar(PERIODO)).isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);

            long antes = System.nanoTime();
            assertThat(adaptador.consultar(PERIODO)).isEmpty();
            assertThat((System.nanoTime() - antes) / 1_000_000).isLessThan(100);
        }
    }
}
