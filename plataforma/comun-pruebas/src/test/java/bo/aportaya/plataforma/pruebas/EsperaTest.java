package bo.aportaya.plataforma.pruebas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Espera} la usan las pruebas que esperan a Kafka o al relevo: si el poll mintiera
 * (volver antes de tiempo, o agotar el plazo sin decirlo), esas pruebas pasarian en verde
 * sin haber visto nada. Por eso el helper tiene su propia prueba, en los tres niveles:
 * la condicion ya cumplida, la que se cumple despues de varios intentos, y la que nunca
 * se cumple o se interrumpe.
 */
class EsperaTest {

    private static final Duration INTERVALO = Duration.ofMillis(5);

    @AfterEach
    void limpiarInterrupcion() {
        Thread.interrupted();
    }

    @Test
    @DisplayName("condicion ya cumplida: vuelve en el primer intento")
    void condicionCumplidaVuelveEnElPrimerIntento() {
        AtomicInteger intentos = new AtomicInteger();

        Espera.hasta(Duration.ofSeconds(1), INTERVALO, "inmediata", () -> intentos.incrementAndGet() > 0);

        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("condicion que se cumple al tercer intento: reintenta y vuelve sin agotar el plazo")
    void condicionTardiaReintentaHastaCumplirse() {
        AtomicInteger intentos = new AtomicInteger();

        Espera.hasta(Duration.ofSeconds(2), INTERVALO, "tardia", () -> intentos.incrementAndGet() >= 3);

        assertThat(intentos).hasValue(3);
    }

    @Test
    @DisplayName("condicion que nunca se cumple: timeout con el motivo, nunca un verde mudo")
    void condicionQueNuncaSeCumpleLanzaConElMotivo() {
        assertThatThrownBy(() -> Espera.hasta(Duration.ofMillis(30), INTERVALO, "fila PUBLICADO", () -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("timeout esperando: fila PUBLICADO");
    }

    @Test
    @DisplayName("plazo ya vencido: igual evalua la condicion una ultima vez antes de fallar")
    void plazoVencidoEvaluaUnaUltimaVez() {
        AtomicInteger intentos = new AtomicInteger();

        Espera.hasta(Duration.ZERO, INTERVALO, "ultima oportunidad", () -> intentos.incrementAndGet() > 0);

        assertThat(intentos).hasValue(1);
    }

    @Test
    @DisplayName("hilo interrumpido: corta con el motivo y conserva la marca de interrupcion")
    void interrupcionCortaYConservaLaMarca() {
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> Espera.hasta(Duration.ofSeconds(1), INTERVALO, "relevo", () -> false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("espera interrumpida: relevo")
                .hasCauseInstanceOf(InterruptedException.class);
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
