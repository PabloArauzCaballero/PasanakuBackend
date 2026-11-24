package bo.aportaya.plataforma.web.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import bo.aportaya.plataforma.dominio.Reloj;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GU-15 · la lista se reconstruye desde el tema: al arrancar, despues de un reinicio, y en vivo. El tema es un
 * {@link MockConsumer} de kafka-clients (doble declarado, regla 65): la integracion con un broker real queda para el
 * ensayo {@code e2eTest}.
 */
class LectorDeRevocacionesTest {

    private static final Instant AHORA = Instant.parse("2026-11-24T15:05:00Z");
    private static final TopicPartition P0 = new TopicPartition(RevocacionDeSesion.TEMA, 0);
    private static final TopicPartition P1 = new TopicPartition(RevocacionDeSesion.TEMA, 1);

    private final SimpleMeterRegistry metricas = new SimpleMeterRegistry();

    private static MockConsumer<String, String> temaConDosParticiones(long finP0, long finP1) {
        var consumidor = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
        Node nodo = new Node(0, "localhost", 9092);
        consumidor.updatePartitions(
                RevocacionDeSesion.TEMA,
                List.of(
                        new PartitionInfo(RevocacionDeSesion.TEMA, 0, nodo, new Node[0], new Node[0]),
                        new PartitionInfo(RevocacionDeSesion.TEMA, 1, nodo, new Node[0], new Node[0])));
        consumidor.updateBeginningOffsets(Map.of(P0, 0L, P1, 0L));
        consumidor.updateEndOffsets(Map.of(P0, finP0, P1, finP1));
        return consumidor;
    }

    private LectorDeRevocaciones lector(MockConsumer<String, String> tema, ListaDeRevocaciones lista) {
        return new LectorDeRevocaciones(() -> tema, lista, Reloj.fijo(AHORA), metricas, Duration.ofSeconds(30));
    }

    private static RevocacionDeSesion revocacion(UUID sesion, Instant revocadaEn) {
        return new RevocacionDeSesion(sesion, UUID.randomUUID(), revocadaEn, revocadaEn.plus(Duration.ofMinutes(15)));
    }

    private static ConsumerRecord<String, String> registro(TopicPartition tp, long offset, RevocacionDeSesion r) {
        return new ConsumerRecord<>(tp.topic(), tp.partition(), offset, r.clave(), r.aJson());
    }

    @Test
    @DisplayName("rehidratacion: lo que ya estaba en el tema entra a la lista y recien al llegar al final queda al dia")
    void rehidrataDesdeElPrincipio() {
        var tema = temaConDosParticiones(2, 1);
        var lista = new ListaDeRevocaciones();
        var lector = lector(tema, lista);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();

        assertThat(lector.asignar(tema)).isTrue();
        assertThat(lista.estaAlDia()).isFalse();
        tema.addRecord(registro(P0, 0, revocacion(a, AHORA.minusSeconds(60))));
        tema.addRecord(registro(P1, 0, revocacion(b, AHORA.minusSeconds(30))));
        lector.pasada(tema);

        assertThat(lista.estaAlDia()).as("falta el offset 1 de la particion 0").isFalse();

        tema.addRecord(registro(P0, 1, revocacion(c, AHORA.minusSeconds(10))));
        lector.pasada(tema);

        assertThat(lista.estaAlDia()).isTrue();
        assertThat(lista.estaRevocada(a, AHORA)).isTrue();
        assertThat(lista.estaRevocada(b, AHORA)).isTrue();
        assertThat(lista.estaRevocada(c, AHORA)).isTrue();
        assertThat(metricas.find("revocacion_propagacion_segundos").summary().count())
                .as("lo rehidratado es historia, no propagacion")
                .isZero();
    }

    @Test
    @DisplayName("reinicio: un proceso nuevo, con una lista vacia, reconstruye exactamente las mismas revocaciones")
    void reconstruyeTrasReinicio() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        var antes = new ListaDeRevocaciones();
        var temaAntes = temaConDosParticiones(1, 1);
        var lectorAntes = lector(temaAntes, antes);
        lectorAntes.asignar(temaAntes);
        temaAntes.addRecord(registro(P0, 0, revocacion(a, AHORA.minusSeconds(60))));
        temaAntes.addRecord(registro(P1, 0, revocacion(b, AHORA.minusSeconds(60))));
        lectorAntes.pasada(temaAntes);

        // «Reinicio»: otra lista y otro consumidor, el mismo contenido del tema.
        var despues = new ListaDeRevocaciones();
        var temaDespues = temaConDosParticiones(1, 1);
        var lectorDespues = lector(temaDespues, despues);
        lectorDespues.asignar(temaDespues);
        temaDespues.addRecord(registro(P0, 0, revocacion(a, AHORA.minusSeconds(60))));
        temaDespues.addRecord(registro(P1, 0, revocacion(b, AHORA.minusSeconds(60))));
        assertThat(despues.estaRevocada(a, AHORA)).isFalse();
        lectorDespues.pasada(temaDespues);

        assertThat(despues.estaAlDia()).isTrue();
        assertThat(despues.tamano()).isEqualTo(antes.tamano()).isEqualTo(2);
        assertThat(despues.estaRevocada(a, AHORA)).isTrue();
        assertThat(despues.estaRevocada(b, AHORA)).isTrue();
    }

    @Test
    @DisplayName("en vivo: con la lista al dia, una revocacion nueva entra y se mide su propagacion")
    void enVivoMideLaPropagacion() {
        var tema = temaConDosParticiones(0, 0);
        var lista = new ListaDeRevocaciones();
        var lector = lector(tema, lista);
        lector.asignar(tema);
        assertThat(lista.estaAlDia()).as("tema vacio: al dia sin leer nada").isTrue();

        UUID nueva = UUID.randomUUID();
        tema.addRecord(registro(P1, 0, revocacion(nueva, AHORA.minusMillis(300))));
        lector.pasada(tema);

        assertThat(lista.estaRevocada(nueva, AHORA)).isTrue();
        var propagacion = metricas.find("revocacion_propagacion_segundos").summary();
        assertThat(propagacion.count()).isEqualTo(1);
        assertThat(propagacion.max()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("limite: si el tema no existe no hay nada que rehidratar y la lista queda al dia")
    void temaInexistente() {
        var tema = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
        var lista = new ListaDeRevocaciones();

        assertThat(lector(tema, lista).asignar(tema)).isFalse();
        assertThat(lista.estaAlDia()).isTrue();
    }

    @Test
    @DisplayName("invalido: un valor ilegible y una lapida se saltan sin frenar a los registros que vienen detras")
    void ilegibleNoFrena() {
        var tema = temaConDosParticiones(3, 0);
        var lista = new ListaDeRevocaciones();
        var lector = lector(tema, lista);
        lector.asignar(tema);
        UUID buena = UUID.randomUUID();
        tema.addRecord(new ConsumerRecord<>(RevocacionDeSesion.TEMA, 0, 0L, "x", "{roto"));
        tema.addRecord(new ConsumerRecord<String, String>(RevocacionDeSesion.TEMA, 0, 1L, "y", null));
        tema.addRecord(registro(P0, 2, revocacion(buena, AHORA.minusSeconds(5))));

        lector.pasada(tema);

        assertThat(lista.estaAlDia()).isTrue();
        assertThat(lista.tamano()).isEqualTo(1);
        assertThat(lista.estaRevocada(buena, AHORA)).isTrue();
    }

    @Test
    @DisplayName("ciclo de vida: arranca un hilo, y al detenerse despierta al consumidor y deja de correr")
    void cicloDeVida() throws InterruptedException {
        var tema = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
        var lista = new ListaDeRevocaciones();
        var lector = lector(tema, lista);

        lector.start();
        assertThat(lector.isRunning()).isTrue();
        // Sin tema, el lector lo declara al dia y espera a redescubrir: es la condicion observable de que el hilo
        // corrio.
        long limite = System.nanoTime() + Duration.ofSeconds(3).toNanos();
        while (!lista.estaAlDia() && System.nanoTime() < limite) {
            Thread.onSpinWait();
        }
        lector.stop();

        assertThat(lista.estaAlDia()).isTrue();
        assertThat(lector.isRunning()).isFalse();
    }
}
