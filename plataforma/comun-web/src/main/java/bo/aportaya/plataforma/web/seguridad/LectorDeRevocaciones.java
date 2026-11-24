package bo.aportaya.plataforma.web.seguridad;

import bo.aportaya.plataforma.dominio.Reloj;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

/**
 * Mantiene {@link ListaDeRevocaciones} al dia leyendo el tema compactado {@value RevocacionDeSesion#TEMA} (ADR-030 §2).
 *
 * <p><b>Sin grupo de consumo y siempre desde el principio.</b> Cada replica necesita la lista ENTERA, no un reparto de
 * particiones con otras replicas, y no hay nada que confirmar: el estado se reconstruye releyendo. Por eso se asignan
 * todas las particiones a mano ({@code assign}), se arranca en el primer offset y no se guarda ninguno. Un reinicio
 * vuelve a leer lo que queda en el tema —la compactacion y la retencion lo mantienen en la ultima media hora— y la
 * lista queda igual que antes de caerse.
 *
 * <p><b>Cuando esta al dia.</b> Al asignar se anotan los offsets finales; la lista se declara al dia cuando la posicion
 * de cada particion los alcanza. Antes de eso el validador rechaza (fallo del lado seguro, ADR-030 §3). Si el tema no
 * existe —el broker no los crea solo y nadie publico nunca una revocacion— no hay nada que rehidratar: la lista se
 * declara al dia y se vuelve a buscar el tema cada {@code redescubrir}.
 *
 * <p><b>Latencia.</b> {@code poll} devuelve en cuanto el broker tiene un registro (fetch.min.bytes = 1), asi que entre
 * que identidad publica y que esta lista lo sabe pasa lo que tarda la red: milisegundos. La metrica
 * {@code revocacion_propagacion_segundos} mide de la marca en la base a la llegada aca, solo para lo que llega con la
 * lista ya al dia (lo rehidratado al arrancar no es propagacion, es historia).
 */
public class LectorDeRevocaciones implements SmartLifecycle {

    private static final Logger BITACORA = LoggerFactory.getLogger(LectorDeRevocaciones.class);
    private static final Duration ESPERA_DE_POLL = Duration.ofMillis(500);
    private static final Duration ESPERA_DE_METADATOS = Duration.ofSeconds(5);
    private static final Duration PAUSA_TRAS_ERROR = Duration.ofSeconds(2);
    private static final Duration CADA_PURGA = Duration.ofMinutes(1);

    private final Supplier<Consumer<String, String>> fabrica;
    private final ListaDeRevocaciones lista;
    private final Reloj reloj;
    private final Duration redescubrir;
    private final DistributionSummary propagacion;

    private volatile boolean corriendo;
    private volatile Consumer<String, String> consumidor;
    private Thread hilo;
    private Map<TopicPartition, Long> finales = Map.of();
    private Instant ultimaPurga = Instant.EPOCH;

    public LectorDeRevocaciones(
            Supplier<Consumer<String, String>> fabrica,
            ListaDeRevocaciones lista,
            Reloj reloj,
            MeterRegistry metricas,
            Duration redescubrir) {
        this.fabrica = fabrica;
        this.lista = lista;
        this.reloj = reloj;
        this.redescubrir = redescubrir;
        this.propagacion = DistributionSummary.builder("revocacion_propagacion_segundos")
                .description("Del commit de la revocacion en identidad a su llegada a este servicio (ADR-030 §4)")
                .publishPercentiles(0.99)
                .register(metricas);
        metricas.gauge("revocacion_sesiones_recordadas", lista, ListaDeRevocaciones::tamano);
    }

    @Override
    public synchronized void start() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        hilo = Thread.ofPlatform().daemon().name("lector-de-revocaciones").start(this::leer);
    }

    @Override
    public synchronized void stop() {
        corriendo = false;
        Consumer<String, String> activo = consumidor;
        if (activo != null) {
            activo.wakeup();
        }
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return corriendo;
    }

    private void leer() {
        while (corriendo) {
            try (Consumer<String, String> c = fabrica.get()) {
                consumidor = c;
                while (corriendo) {
                    if (asignar(c)) {
                        leerHastaDetenerse(c);
                    } else {
                        dormir(redescubrir);
                    }
                }
            } catch (WakeupException detenido) {
                return;
            } catch (RuntimeException caido) {
                // Kafka no responde: la lista conserva lo que sabe (y si estaba al dia, sigue al dia — ADR-030 §3).
                BITACORA.warn(
                        "lector de revocaciones: {} — se reintenta",
                        caido.getClass().getSimpleName());
                dormir(PAUSA_TRAS_ERROR);
            } finally {
                consumidor = null;
            }
        }
    }

    private void leerHastaDetenerse(Consumer<String, String> c) {
        while (corriendo) {
            pasada(c);
        }
    }

    /**
     * Asigna todas las particiones del tema, desde el principio, y anota hasta donde hay que leer para estar al dia.
     *
     * @return {@code false} si el tema no existe todavia (la lista queda al dia: no hay nada que rehidratar)
     */
    boolean asignar(Consumer<String, String> c) {
        List<PartitionInfo> particiones = c.partitionsFor(RevocacionDeSesion.TEMA, ESPERA_DE_METADATOS);
        if (particiones == null || particiones.isEmpty()) {
            if (!lista.estaAlDia()) {
                BITACORA.warn("el tema {} no existe: no hay revocaciones que rehidratar", RevocacionDeSesion.TEMA);
            }
            lista.marcarAlDia();
            return false;
        }
        List<TopicPartition> asignadas = particiones.stream()
                .map(p -> new TopicPartition(p.topic(), p.partition()))
                .toList();
        c.assign(asignadas);
        c.seekToBeginning(asignadas);
        finales = c.endOffsets(asignadas, ESPERA_DE_METADATOS);
        marcarSiAlcanzo(c);
        return true;
    }

    /** Una vuelta: lee lo que haya, lo aplica, y declara la lista al dia si alcanzo los offsets finales. */
    void pasada(Consumer<String, String> c) {
        for (ConsumerRecord<String, String> registro : c.poll(ESPERA_DE_POLL)) {
            aplicar(registro);
        }
        marcarSiAlcanzo(c);
        Instant ahora = reloj.ahora();
        if (Duration.between(ultimaPurga, ahora).compareTo(CADA_PURGA) >= 0) {
            lista.purgar(ahora);
            ultimaPurga = ahora;
        }
    }

    private void aplicar(ConsumerRecord<String, String> registro) {
        if (registro.value() == null) {
            return; // lapida de compactacion: la sesion ya vencio y el productor la olvido
        }
        RevocacionDeSesion revocacion;
        try {
            revocacion = RevocacionDeSesion.desdeJson(registro.value());
        } catch (IllegalArgumentException ilegible) {
            // Un mensaje roto no puede frenar a los que vienen detras: se registra (sin el valor) y se sigue.
            BITACORA.error(
                    "revocacion ilegible en {}-{}@{}: {}",
                    registro.topic(),
                    registro.partition(),
                    registro.offset(),
                    ilegible.getMessage());
            return;
        }
        boolean enVivo = lista.estaAlDia();
        lista.registrar(revocacion);
        if (enVivo) {
            Duration demora = Duration.between(revocacion.revocadaEn(), reloj.ahora());
            propagacion.record(Math.max(0, demora.toMillis()) / 1000.0);
        }
    }

    private void marcarSiAlcanzo(Consumer<String, String> c) {
        if (lista.estaAlDia()) {
            return;
        }
        boolean alcanzo = finales.entrySet().stream().allMatch(fin -> c.position(fin.getKey()) >= fin.getValue());
        if (alcanzo) {
            lista.marcarAlDia();
            BITACORA.info("lista de revocaciones al dia: {} sesiones recordadas", lista.tamano());
        }
    }

    private void dormir(Duration pausa) {
        try {
            Thread.sleep(pausa);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            corriendo = false;
        }
    }
}
