package bo.aportaya.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bo.aportaya.identidad.infraestructura.SesionesRevocadasRepositorio.SesionRevocada;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.RevocacionDeSesion;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

/**
 * GU-15 · identidad publica cada sesion cerrada una vez, directo al tema. Kafka es un doble (regla 65) y la tabla
 * {@code sesion} una lista que respeta el contrato de {@link SesionesRevocadasRepositorio#despuesDe}.
 */
class VigiaDeRevocacionesTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T15:05:00Z");
    private static final Duration RECORDAR = Duration.ofMinutes(15);

    private final List<SesionRevocada> tabla = new ArrayList<>();
    private int lecturas;

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);

    private List<SesionRevocada> leer(OffsetDateTime desde, UUID despuesDe, int porTanda) {
        lecturas++;
        // Por texto: PostgreSQL ordena los uuid sin signo, y UUID.compareTo de Java compara con signo.
        Comparator<SesionRevocada> orden = Comparator.comparing(SesionRevocada::revocadaEn)
                .thenComparing(s -> s.sesionId().toString());
        return tabla.stream()
                .filter(s -> !s.revocadaEn().isBefore(desde))
                .filter(s ->
                        s.revocadaEn().isAfter(desde) || s.sesionId().toString().compareTo(despuesDe.toString()) > 0)
                .sorted(orden)
                .limit(porTanda)
                .toList();
    }

    private VigiaDeRevocaciones vigia(int porTanda) {
        return new VigiaDeRevocaciones(
                this::leer,
                porTanda,
                kafka,
                Reloj.fijo(AHORA),
                Duration.ofMillis(250),
                Duration.ofMinutes(2),
                RECORDAR,
                Duration.ofSeconds(1));
    }

    private SesionRevocada revocada(long segundosAtras) {
        var fila = new SesionRevocada(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AHORA.minusSeconds(segundosAtras).atOffset(ZoneOffset.UTC));
        tabla.add(fila);
        return fila;
    }

    private void kafkaResponde() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
    }

    @Test
    @DisplayName(
            "correcto: una sesion cerrada se publica en el tema, con clave = sesion y vencimiento = cierre + 15 min")
    void publicaLaRevocacion() {
        kafkaResponde();
        var fila = revocada(3);

        assertThat(vigia(500).vigilar()).isEqualTo(1);

        var valor = ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(RevocacionDeSesion.TEMA), eq(fila.sesionId().toString()), valor.capture());
        var publicada = RevocacionDeSesion.desdeJson(valor.getValue());
        assertThat(publicada.usuarioId()).isEqualTo(fila.usuarioId());
        assertThat(publicada.venceEn()).isEqualTo(fila.revocadaEn().toInstant().plus(RECORDAR));
    }

    @Test
    @DisplayName("limite: la vuelta siguiente repasa el margen pero no republica lo que ya salio; lo nuevo si")
    void noRepite() {
        kafkaResponde();
        revocada(30);
        var vigia = vigia(500);
        vigia.vigilar();

        assertThat(vigia.vigilar()).isZero();
        revocada(1);
        assertThat(vigia.vigilar()).isEqualTo(1);

        verify(kafka, times(2)).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("limite: una revocacion masiva mayor que la tanda se recorre entera en la misma vuelta")
    void revocacionMasiva() {
        kafkaResponde();
        OffsetDateTime mismoInstante = AHORA.minusSeconds(5).atOffset(ZoneOffset.UTC);
        for (int i = 0; i < 7; i++) {
            tabla.add(new SesionRevocada(UUID.randomUUID(), UUID.randomUUID(), mismoInstante));
        }

        assertThat(vigia(3).vigilar()).isEqualTo(7);
        assertThat(lecturas).isEqualTo(3);
    }

    @Test
    @DisplayName("limite: lo revocado hace mas de 15 minutos no se publica (ningun token suyo puede seguir vivo)")
    void viejaNoSePublica() {
        revocada(RECORDAR.toSeconds() + 1);

        assertThat(vigia(500).vigilar()).isZero();
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("invalido: si Kafka falla no se marca como publicada y la vuelta siguiente la reintenta")
    void kafkaCaidoReintenta() {
        var fila = revocada(2);
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caido")))
                .thenReturn(CompletableFuture.<SendResult<String, String>>completedFuture(null));
        var vigia = vigia(500);

        assertThat(vigia.vigilar()).isZero();
        assertThat(vigia.vigilar()).isEqualTo(1);
        verify(kafka, times(2))
                .send(eq(RevocacionDeSesion.TEMA), eq(fila.sesionId().toString()), anyString());
    }

    @Test
    @DisplayName("ciclo de vida: arranca su propio hilo y se detiene")
    void cicloDeVida() {
        var vigia = vigia(500);
        vigia.start();
        assertThat(vigia.isRunning()).isTrue();
        vigia.stop();
        assertThat(vigia.isRunning()).isFalse();
    }
}
