package bo.aportaya.identidad.infraestructura;

import bo.aportaya.identidad.infraestructura.SesionesRevocadasRepositorio.SesionRevocada;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.web.seguridad.RevocacionDeSesion;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * La unica publicacion del sistema que no pasa por el outbox (ADR-030 §1, GU-15): mira {@code sesion.revocada_en} y
 * publica cada sesion recien cerrada, directo, en {@value RevocacionDeSesion#TEMA}.
 *
 * <p><b>Por que se mira la columna y no se engancha cada cierre.</b> Las sesiones se revocan desde al menos seis
 * lugares —cerrar sesion, renovar con cuenta no habilitada o por inactividad, revocar un rol, rotar roles, y el trigger
 * de reuso de refresco ({@code R-SEG-09}), que ni siquiera es Java—. Engancharlos uno por uno deja afuera al proximo que
 * alguien escriba. La columna es la verdad, y el que la lea los ve a todos.
 *
 * <p><b>El margen.</b> {@code revocada_en} es la hora en que la transaccion la escribio, no la del commit: una
 * transaccion lenta puede hacerse visible con una marca ya pasada. Por eso cada vuelta vuelve a mirar {@code margen}
 * hacia atras y recuerda lo que ya publico para no repetirlo. Repetir tampoco romperia nada: el tema es compactado y la
 * lista de cada servicio es idempotente.
 *
 * <p><b>Si Kafka falla</b> la marca no avanza y la vuelta siguiente reintenta: nada se pierde, se demora. Varias replicas
 * de identidad publican cada una lo suyo (duplicados inocuos); no se toma ningun lock para no meter otra espera.
 *
 * <p><b>Su propio hilo.</b> El planificador de Spring tiene un hilo y lo comparte con el relevo del outbox, que puede
 * esperar hasta diez segundos a Kafka. La revocacion no puede quedar detras de eso.
 */
public class VigiaDeRevocaciones implements SmartLifecycle {

    private static final Logger BITACORA = LoggerFactory.getLogger(VigiaDeRevocaciones.class);

    /** Una pagina de sesiones revocadas despues de {@code (desde, despuesDe)}; la implementa el repositorio. */
    @FunctionalInterface
    public interface Lectura {
        List<SesionRevocada> leer(OffsetDateTime desde, UUID despuesDe, int porTanda);
    }

    private static final UUID PRIMERO = new UUID(0L, 0L);

    private final Lectura lector;
    private final int porTanda;
    private final KafkaTemplate<String, String> kafka;
    private final Reloj reloj;
    private final Duration intervalo;
    private final Duration margen;
    private final Duration recordar;
    private final Duration esperaDeKafka;

    /** Sesiones ya publicadas, con su {@code revocada_en}: lo que todavia cae dentro del margen. */
    private final Map<UUID, OffsetDateTime> publicadas = new ConcurrentHashMap<>();

    private OffsetDateTime marca;
    private volatile boolean corriendo;
    private Thread hilo;

    /**
     * @param recordar cuanto vive una revocacion en la lista de los demas: el tope de vida de un token tras revocar
     *     ({@link EmisorDeAcceso#TOPE_TRAS_REVOCAR}). Al arrancar se republica esa ventana entera.
     */
    public VigiaDeRevocaciones(
            Lectura lector,
            int porTanda,
            KafkaTemplate<String, String> kafka,
            Reloj reloj,
            Duration intervalo,
            Duration margen,
            Duration recordar,
            Duration esperaDeKafka) {
        this.lector = lector;
        this.porTanda = porTanda;
        this.kafka = kafka;
        this.reloj = reloj;
        this.intervalo = intervalo;
        this.margen = margen;
        this.recordar = recordar;
        this.esperaDeKafka = esperaDeKafka;
    }

    @Override
    public synchronized void start() {
        if (corriendo) {
            return;
        }
        corriendo = true;
        hilo = Thread.ofPlatform().daemon().name("vigia-de-revocaciones").start(this::correr);
    }

    @Override
    public synchronized void stop() {
        corriendo = false;
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return corriendo;
    }

    private void correr() {
        while (corriendo) {
            try {
                vigilar();
            } catch (RuntimeException caido) {
                // La base no responde: se reintenta en la vuelta siguiente, con la marca donde estaba.
                BITACORA.warn(
                        "vigia de revocaciones: {} — se reintenta",
                        caido.getClass().getSimpleName());
            }
            try {
                Thread.sleep(intervalo);
            } catch (InterruptedException interrumpido) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Una vuelta: lee desde la marca menos el margen, publica lo nuevo y avanza la marca hasta lo ultimo publicado.
     *
     * @return cuantas revocaciones publico en esta vuelta
     */
    int vigilar() {
        Instant ahora = reloj.ahora();
        OffsetDateTime desde = marca == null ? ahora.minus(recordar).atOffset(ZoneOffset.UTC) : marca.minus(margen);
        OffsetDateTime cursorEn = desde;
        UUID cursorId = PRIMERO;
        int publicadasAhora = 0;
        List<SesionRevocada> pagina;
        do {
            pagina = lector.leer(cursorEn, cursorId, porTanda);
            for (SesionRevocada fila : pagina) {
                if (!publicadas.containsKey(fila.sesionId())) {
                    Instant revocada = fila.revocadaEn().toInstant();
                    Instant vence = revocada.plus(recordar);
                    if (vence.isAfter(ahora)) {
                        if (!publicar(new RevocacionDeSesion(fila.sesionId(), fila.usuarioId(), revocada, vence))) {
                            return publicadasAhora; // Kafka no respondio: la marca queda donde estaba
                        }
                        publicadasAhora++;
                    }
                    publicadas.put(fila.sesionId(), fila.revocadaEn());
                }
                if (marca == null || fila.revocadaEn().isAfter(marca)) {
                    marca = fila.revocadaEn();
                }
                cursorEn = fila.revocadaEn();
                cursorId = fila.sesionId();
            }
        } while (pagina.size() >= porTanda);
        if (marca == null) {
            marca = desde;
        }
        OffsetDateTime olvidar = marca.minus(margen);
        publicadas.values().removeIf(revocadaEn -> revocadaEn.isBefore(olvidar));
        return publicadasAhora;
    }

    private boolean publicar(RevocacionDeSesion revocacion) {
        try {
            kafka.send(RevocacionDeSesion.TEMA, revocacion.clave(), revocacion.aJson())
                    .get(esperaDeKafka.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | TimeoutException fallo) {
            BITACORA.warn(
                    "no se pudo publicar la revocacion de la sesion {}: {}",
                    revocacion.sesionId(),
                    fallo.getClass().getSimpleName());
            return false;
        }
    }
}
