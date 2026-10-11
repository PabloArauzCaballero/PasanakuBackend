package bo.aportaya.aportes.infraestructura.clientes;

import static bo.aportaya.aportes.infraestructura.clientes.CalendarioDelPeriodoPorHttpTest.contra;
import static bo.aportaya.aportes.infraestructura.clientes.CalendarioDelPeriodoPorHttpTest.dorado;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.pruebas.DependidoDeEnsayo;
import bo.aportaya.plataforma.pruebas.Espera;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regla 98.2.3 y 98.8.4 · la caida de `grupos` ejercitada de verdad (el doble {@link DependidoDeEnsayo} se cae,
 * vuelve y cuenta las peticiones que le llegan): despues de una ventana de fallos el cliente deja de llamar, aunque
 * el dependido ya haya vuelto, y se recupera solo tras la espera. FALLBACK declarado: vacio, y el caso de uso rechaza
 * con {@code PERIODO_NO_CONFIRMADO} (para dinero no se genera nada).
 */
class CalendarioDelPeriodoCaidaTest {

    private static final UUID PERIODO = UUID.fromString("c8100000-0000-4000-8000-000000000001");
    private static final int VENTANA = 10;
    private static final long ESPERA_MS = 400;

    @Test
    @DisplayName(
            "la caida de grupos abre el circuito; con el circuito abierto NO se llama aunque el dependido ya volvio")
    void caidaAbreElCircuitoYNoLlama() {
        try (var grupos = new DependidoDeEnsayo().responder(200, dorado())) {
            var adaptador = contra(grupos, VENTANA, ESPERA_MS);
            grupos.caer();

            for (int i = 0; i < VENTANA; i++) {
                assertThat(adaptador.deLosCupos(PERIODO))
                        .as("llamada %s con grupos caido", i)
                        .isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);

            // El dependido vuelve, pero el circuito sigue abierto: la peticion no sale.
            grupos.levantar();
            assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
            assertThat(grupos.peticionesRecibidas()).isZero();
        }
    }

    @Test
    @DisplayName("recuperacion: tras la espera pasan llamadas de prueba y, si responden bien, el circuito cierra")
    void seRecuperaSolo() {
        try (var grupos = new DependidoDeEnsayo().responder(200, dorado())) {
            var adaptador = contra(grupos, VENTANA, ESPERA_MS);
            grupos.caer();
            for (int i = 0; i < VENTANA; i++) {
                adaptador.deLosCupos(PERIODO);
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
            grupos.levantar();

            Espera.hasta(
                    Duration.ofSeconds(3), Duration.ofMillis(50), "primera llamada de prueba tras la espera", () -> {
                        return adaptador.deLosCupos(PERIODO).isPresent();
                    });
            // Las otras dos llamadas de prueba confirman la recuperacion y el circuito cierra.
            assertThat(adaptador.deLosCupos(PERIODO)).isPresent();
            assertThat(adaptador.deLosCupos(PERIODO)).isPresent();

            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
            assertThat(grupos.peticionesRecibidas()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("5xx cuentan como caida: una ventana de 500 abre el circuito")
    void cincoXXAbren() {
        try (var grupos = new DependidoDeEnsayo().responder(500, "{}")) {
            var adaptador = contra(grupos, VENTANA, ESPERA_MS);
            for (int i = 0; i < VENTANA; i++) {
                assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
            }

            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
            assertThat(grupos.peticionesRecibidas()).isEqualTo(VENTANA);
            adaptador.deLosCupos(PERIODO);
            assertThat(grupos.peticionesRecibidas())
                    .as("con el circuito abierto no sale otra peticion")
                    .isEqualTo(VENTANA);
        }
    }

    @Test
    @DisplayName("un cuerpo ilegible tambien cuenta como caida del dependido")
    void cuerpoIlegibleAbre() {
        try (var grupos = new DependidoDeEnsayo().responder(200, "esto no es json")) {
            var adaptador = contra(grupos, VENTANA, ESPERA_MS);
            for (int i = 0; i < VENTANA; i++) {
                adaptador.deLosCupos(PERIODO);
            }

            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);
        }
    }

    @Test
    @DisplayName("un 404 o un 422 NO es una caida: tres ventanas de respuestas de negocio no abren el circuito")
    void cuatroXXNoAbren() {
        try (var grupos = new DependidoDeEnsayo().responder(422, "{\"codigo\":\"AP-CU21-08\"}")) {
            var adaptador = contra(grupos, VENTANA, ESPERA_MS);
            for (int i = 0; i < 3 * VENTANA; i++) {
                assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
            }

            assertThat(adaptador.estadoDelCircuito()).isEqualTo(CLOSED);
            assertThat(grupos.peticionesRecibidas()).isEqualTo(3 * VENTANA);
        }
    }

    @Test
    @DisplayName("grupos lento (mas que el tiempo maximo) tambien abre el circuito y luego falla al instante")
    void lentoAbreYLuegoFallaAlInstante() {
        try (var grupos = new DependidoDeEnsayo().tardar(600).responder(200, dorado())) {
            // Ventana corta: cada llamada espera el tiempo maximo (300 ms).
            var adaptador = contra(grupos, 4, ESPERA_MS);
            for (int i = 0; i < 4; i++) {
                assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
            }
            assertThat(adaptador.estadoDelCircuito()).isEqualTo(OPEN);

            long antes = System.nanoTime();
            assertThat(adaptador.deLosCupos(PERIODO)).isEmpty();
            assertThat((System.nanoTime() - antes) / 1_000_000)
                    .as("con el circuito abierto no espera la red")
                    .isLessThan(100);
        }
    }
}
