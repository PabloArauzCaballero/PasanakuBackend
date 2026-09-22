package bo.aportaya.plataforma.mensajeria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;

/**
 * H2.S3 — tomar-publicar-marcar en transacciones cortas, backoff con jitter, {@code
 * FALLIDO} como DLQ logica (Q-03), recuperacion de un {@code TOMADO} huerfano.
 *
 * <p>Kafka va con un doble (Mockito) y no con un broker real: lo que esta prueba fija
 * es la maquina de estados de {@link Relevo} contra PostgreSQL real, no el transporte
 * — eso lo prueba {@code OutboxE2ETest} con Kafka de Testcontainers (H2.S2).
 */
class RelevoRepositorioTest extends BaseDePruebaMensajeria {

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafkaQueSiempreLlega() {
        KafkaTemplate<String, String> k = mock(KafkaTemplate.class);
        when(k.send(any(Message.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        return k;
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafkaQueSiempreFalla() {
        KafkaTemplate<String, String> k = mock(KafkaTemplate.class);
        CompletableFuture<SendResult<String, String>> futuro = new CompletableFuture<>();
        futuro.completeExceptionally(new RuntimeException("kafka no responde (prueba)"));
        when(k.send(any(Message.class))).thenReturn(futuro);
        return k;
    }

    private Relevo relevoCon(KafkaTemplate<String, String> kafka, int intentosMaximos, Duration backoffTope) {
        return relevoCon(kafka, intentosMaximos, backoffTope, Duration.ofMillis(10));
    }

    private Relevo relevoCon(
            KafkaTemplate<String, String> kafka, int intentosMaximos, Duration backoffTope, Duration backoffBase) {
        return new Relevo(
                ESQUEMA,
                dsl,
                kafka,
                new SimpleMeterRegistry(),
                intentosMaximos,
                backoffBase,
                backoffTope,
                Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("tomar-publicar-marcar: un PENDIENTE se publica y queda PUBLICADO")
    void tomaPublicaYMarca() {
        UUID id = insertarPendiente();
        relevoCon(kafkaQueSiempreLlega(), 10, Duration.ofMinutes(5)).relevar();

        var fila = filaDe(id);
        assertThat(fila.get("estado", String.class)).isEqualTo("PUBLICADO");
        assertThat(fila.get("publicado_en")).isNotNull();
        assertThat(fila.get("intentos", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("fallo: intentos++, ultimo_error, proximo_intento_en con backoff")
    void falloIncrementaIntentosYAgendaBackoff() {
        UUID id = insertarPendiente();
        // backoff-base generoso (10s) y no los 10ms de otras pruebas: aca lo que se fija es
        // que proximo_intento_en quede en el futuro, y con un margen chico el "futuro" ya
        // paso para cuando la asercion corre, en una maquina bajo carga.
        OffsetDateTime antesDeRelevar = OffsetDateTime.now();
        relevoCon(kafkaQueSiempreFalla(), 10, Duration.ofMinutes(5), Duration.ofSeconds(10))
                .relevar();

        var fila = filaDe(id);
        assertThat(fila.get("estado", String.class)).isEqualTo("PENDIENTE");
        assertThat(fila.get("intentos", Integer.class)).isEqualTo(1);
        assertThat(fila.get("ultimo_error", String.class)).contains("kafka no responde");
        assertThat(fila.get("proximo_intento_en", OffsetDateTime.class)).isAfter(antesDeRelevar);
    }

    @Test
    @DisplayName("tras intentos-maximos, FALLIDO — DLQ logica en la misma tabla (Q-03)")
    void falloRepetidoLlegaAFallido() {
        UUID id = insertarPendiente();
        Relevo relevo = relevoCon(kafkaQueSiempreFalla(), 2, Duration.ofMillis(1));

        relevo.relevar();
        esperarProximoIntento();
        relevo.relevar();

        var fila = filaDe(id);
        assertThat(fila.get("estado", String.class)).isEqualTo("FALLIDO");
        assertThat(fila.get("intentos", Integer.class)).isEqualTo(2);
        assertThat(fila.get("proximo_intento_en")).isNull();
    }

    @Test
    @DisplayName("un TOMADO mas viejo que backoff-tope es huerfano: se retoma y publica")
    void tomadoHuerfanoSeRecupera() {
        UUID id = insertarTomadoHace(Duration.ofMinutes(10));
        relevoCon(kafkaQueSiempreLlega(), 10, Duration.ofMinutes(5)).relevar();

        assertThat(filaDe(id).get("estado", String.class)).isEqualTo("PUBLICADO");
    }

    @Test
    @DisplayName("dos relevos a la vez no publican el mismo evento dos veces (SKIP LOCKED)")
    void dosRelevosNoPublicanDosVeces() throws InterruptedException {
        UUID id = insertarPendiente();
        var contadorA = new java.util.concurrent.atomic.AtomicInteger();
        var contadorB = new java.util.concurrent.atomic.AtomicInteger();
        Relevo relevoA = relevoCon(kafkaContando(contadorA), 10, Duration.ofMinutes(5));
        Relevo relevoB = relevoCon(kafkaContando(contadorB), 10, Duration.ofMinutes(5));

        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(relevoA::relevar);
            var f2 = pool.submit(relevoB::relevar);
            f1.get();
            f2.get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException(e);
        } finally {
            pool.shutdown();
        }

        assertThat(contadorA.get() + contadorB.get())
                .as("el evento se publico una sola vez, entre los dos relevos")
                .isEqualTo(1);
        assertThat(filaDe(id).get("estado", String.class)).isEqualTo("PUBLICADO");
    }

    @SuppressWarnings("unchecked")
    private KafkaTemplate<String, String> kafkaContando(java.util.concurrent.atomic.AtomicInteger contador) {
        KafkaTemplate<String, String> k = mock(KafkaTemplate.class);
        when(k.send(any(Message.class))).thenAnswer(inv -> {
            contador.incrementAndGet();
            return CompletableFuture.completedFuture(mock(SendResult.class));
        });
        return k;
    }

    private void esperarProximoIntento() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
