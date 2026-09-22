package bo.aportaya.plataforma.mensajeria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Lee el outbox y publica DESPUES del {@code COMMIT}. Es **al menos una vez**, y por
 * eso todo consumidor es idempotente.
 *
 * <p>{@code FOR UPDATE SKIP LOCKED} es lo que permite que varias replicas releven a
 * la vez sin pisarse ni bloquearse: la que llega segunda salta las filas tomadas en
 * vez de esperarlas.
 *
 * <p><b>Tomar, publicar, marcar — tres pasos, dos transacciones cortas (H2.S3.M3,
 * ADR-018).</b> {@code relevar()} NO lleva {@code @Transactional}: la primera
 * transaccion toma una tanda ({@code UPDATE ... RETURNING}, corta, commitea sola), el
 * {@code kafka.send().get(timeout)} corre SIN ninguna transaccion de PostgreSQL
 * abierta, y una segunda transaccion corta marca el resultado. Antes, todo el metodo
 * estaba en una sola transaccion que envolvia tambien la llamada de red: una
 * PostgreSQL con {@code idle in transaction} mientras Kafka tarda es exactamente el
 * defecto que este refactor elimina.
 *
 * <p>Con Kafka caido no se pierde nada: los eventos se acumulan en {@code PENDIENTE}
 * y la metrica {@code aportaya.outbox.edad_mas_viejo_segundos} lo muestra antes de que
 * alguien pregunte.
 */
public class Relevo {

    private static final Logger BITACORA = LoggerFactory.getLogger(Relevo.class);
    private static final int POR_TANDA = 100;
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Identifica QUE instancia tomo una fila — para diagnosticar un {@code TOMADO} huerfano. */
    private final String identidad = "relevo-" + UUID.randomUUID();

    private final String esquema;
    private final DSLContext dsl;
    private final KafkaTemplate<String, String> kafka;
    private final int intentosMaximos;
    private final Duration backoffBase;
    private final Duration backoffTope;
    private final Duration timeoutPublicacion;

    private final AtomicLong edadDelMasViejo = new AtomicLong();
    private final Counter publicacionesOk;
    private final Counter publicacionesError;

    public Relevo(
            String esquema,
            DSLContext dsl,
            KafkaTemplate<String, String> kafka,
            MeterRegistry metricas,
            @Value("${aportaya.outbox.intentos-maximos:10}") int intentosMaximos,
            @Value("${aportaya.outbox.backoff-base:PT1S}") Duration backoffBase,
            @Value("${aportaya.outbox.backoff-tope:PT5M}") Duration backoffTope,
            @Value("${aportaya.outbox.timeout-publicacion:PT10S}") Duration timeoutPublicacion) {
        this.esquema = Objects.requireNonNull(esquema, "esquema");
        this.dsl = Objects.requireNonNull(dsl, "dsl");
        this.kafka = Objects.requireNonNull(kafka, "kafka");
        this.intentosMaximos = intentosMaximos;
        this.backoffBase = Objects.requireNonNull(backoffBase, "backoffBase");
        this.backoffTope = Objects.requireNonNull(backoffTope, "backoffTope");
        this.timeoutPublicacion = Objects.requireNonNull(timeoutPublicacion, "timeoutPublicacion");
        metricas.gauge("aportaya.outbox.edad_mas_viejo_segundos", edadDelMasViejo);
        metricas.gauge("aportaya.outbox.pendientes", this, r -> r.contarPorEstado("PENDIENTE")
                .doubleValue());
        metricas.gauge("aportaya.outbox.fallidos", this, r -> r.contarPorEstado("FALLIDO")
                .doubleValue());
        this.publicacionesOk = Counter.builder("aportaya.outbox.publicaciones_total")
                .tag("resultado", "ok")
                .register(metricas);
        this.publicacionesError = Counter.builder("aportaya.outbox.publicaciones_total")
                .tag("resultado", "error")
                .register(metricas);
    }

    @Scheduled(fixedDelayString = "${aportaya.outbox.intervalo:PT1S}")
    @SchedulerLock(name = "outbox.relevo", lockAtMostFor = "PT30S")
    public void relevar() {
        List<Record> tomados = tomarTanda();
        medirEdad(tomados);
        tomados.forEach(this::publicarYMarcar);
    }

    /**
     * Tx1, corta: toma una tanda de PENDIENTE listos para intentar, sin tocar Kafka. Un
     * {@code TOMADO} mas viejo que {@code backoff-tope} es un huerfano — el relevo que lo tomo
     * murio entre las dos transacciones cortas, sin marcar nada — y se retoma igual que un
     * PENDIENTE (H2.S3, "TOMADO huerfano se recupera").
     */
    private List<Record> tomarTanda() {
        var tabla = DSL.table(DSL.name(esquema, "evento_dominio"));
        OffsetDateTime umbralHuerfano = OffsetDateTime.now().minus(backoffTope);
        return dsl.transactionResult(cfg -> {
            DSLContext tx = DSL.using(cfg);
            var subconsulta = tx.select(DSL.field("id"))
                    .from(tabla)
                    .where(DSL.field("estado")
                            .eq("PENDIENTE")
                            .and(DSL.field("proximo_intento_en")
                                    .isNull()
                                    .or(DSL.field("proximo_intento_en", OffsetDateTime.class)
                                            .le(DSL.field("now()", OffsetDateTime.class)))))
                    .or(DSL.field("estado")
                            .eq("TOMADO")
                            .and(DSL.field("tomado_en", OffsetDateTime.class).lt(umbralHuerfano)))
                    .orderBy(DSL.field("ocurrido_en").asc())
                    .limit(POR_TANDA)
                    .forUpdate()
                    .skipLocked();
            return tx.update(tabla)
                    .set(DSL.field("estado", String.class), "TOMADO")
                    .set(DSL.field("tomado_en", OffsetDateTime.class), DSL.field("now()", OffsetDateTime.class))
                    .set(DSL.field("tomado_por", String.class), identidad)
                    .where(DSL.field("id").in(subconsulta))
                    .returning(
                            DSL.field("id"),
                            DSL.field("tipo"),
                            DSL.field("agregado_id"),
                            DSL.field("payload"),
                            DSL.field("metadatos"),
                            DSL.field("correlation_id"),
                            DSL.field("causation_id"),
                            DSL.field("ocurrido_en"),
                            DSL.field("intentos"))
                    .fetch();
        });
    }

    /** Kafka, fuera de cualquier transaccion de PostgreSQL; despues, tx2 corta para marcar. */
    private void publicarYMarcar(Record evento) {
        UUID id = (UUID) evento.get("id");
        String tipo = String.valueOf(evento.get("tipo"));
        try {
            kafka.send(mensaje(evento)).get(timeoutPublicacion.toMillis(), TimeUnit.MILLISECONDS);
            marcarPublicado(id);
            publicacionesOk.increment();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("relevo interrumpido", e);
        } catch (ExecutionException | TimeoutException e) {
            publicacionesError.increment();
            marcarFallo(evento, causaDe(e));
        }
    }

    private Throwable causaDe(Exception e) {
        return e.getCause() != null ? e.getCause() : e;
    }

    /**
     * El envelope: 8 cabeceras desde las columnas y {@code metadatos} de {@code evento_dominio}
     * (H2.S2.M4, contrato en {@code docs/auditoria-produccion/contratos/evento-kafka.md}).
     */
    private Message<String> mensaje(Record evento) {
        UUID id = (UUID) evento.get("id");
        String tipo = String.valueOf(evento.get("tipo"));
        UUID agregadoId = (UUID) evento.get("agregado_id");
        String payload = String.valueOf(evento.get("payload"));
        UUID correlationId = (UUID) evento.get("correlation_id");
        UUID causationId = (UUID) evento.get("causation_id");
        OffsetDateTime ocurridoEn = evento.get("ocurrido_en", OffsetDateTime.class);
        String metadatos = String.valueOf(evento.get("metadatos"));

        var builder = MessageBuilder.withPayload(payload)
                .setHeader(KafkaHeaders.TOPIC, "aportaya." + tipo)
                // La clave de particion es el agregado: lo de un mismo agregado llega en orden.
                .setHeader(KafkaHeaders.KEY, agregadoId.toString())
                .setHeader("event_id", id.toString())
                .setHeader("type", tipo)
                .setHeader("version", "1")
                .setHeader("occurred_at", ocurridoEn.toInstant().toString())
                .setHeader("producer", esquema)
                .setHeader("correlation_id", correlationId.toString())
                .setHeader("trace_id", trazaDe(metadatos, correlationId));
        if (causationId != null) {
            builder.setHeader("causation_id", causationId.toString());
        }
        return builder.build();
    }

    /**
     * {@code trace_id} sale del MDC de {@code Traza} al momento de publicar — pero el relevo
     * corre en un hilo programado, sin peticion en curso, asi que ahi nunca hay nada. Se lee de
     * {@code metadatos.trace_id} (lo que la peticion original guardo al emitir, H4.S2.M2) y, a
     * falta de eso todavia (el emisor no lo escribe hasta H4), se usa {@code correlation_id}: la
     * cabecera nunca falta, aunque la traza real de punta a punta se cierre recien en H4.
     */
    private String trazaDe(String metadatosJson, UUID correlationId) {
        try {
            JsonNode nodo = JSON.readTree(metadatosJson).get("trace_id");
            if (nodo != null && !nodo.isNull()) {
                return nodo.asText();
            }
        } catch (Exception e) {
            BITACORA.debug("metadatos sin trace_id parseable, se usa correlation_id", e);
        }
        return correlationId.toString();
    }

    private void marcarPublicado(UUID id) {
        dsl.update(DSL.table(DSL.name(esquema, "evento_dominio")))
                .set(DSL.field("estado", String.class), "PUBLICADO")
                .set(DSL.field("publicado_en", OffsetDateTime.class), DSL.field("now()", OffsetDateTime.class))
                .set(DSL.field("intentos", Short.class), DSL.field("intentos + 1", Short.class))
                .where(DSL.field("id").eq(id))
                .execute();
    }

    /** {@code FALLIDO} (DLQ logica, Q-03) tras {@code intentos-maximos}; si no, backoff con jitter. */
    private void marcarFallo(Record evento, Throwable causa) {
        UUID id = (UUID) evento.get("id");
        String tipo = String.valueOf(evento.get("tipo"));
        int intentosPrevios = ((Number) evento.get("intentos")).intValue();
        int intentos = intentosPrevios + 1;
        String mensajeError = String.valueOf(causa.getMessage());
        if (mensajeError.length() > 500) {
            mensajeError = mensajeError.substring(0, 500);
        }

        var actualizacion = dsl.update(DSL.table(DSL.name(esquema, "evento_dominio")))
                .set(DSL.field("intentos", Short.class), (short) intentos)
                .set(DSL.field("ultimo_error", String.class), mensajeError);

        if (intentos >= intentosMaximos) {
            actualizacion
                    .set(DSL.field("estado", String.class), "FALLIDO")
                    .set(DSL.field("proximo_intento_en", OffsetDateTime.class), (OffsetDateTime) null)
                    .where(DSL.field("id").eq(id))
                    .execute();
            BITACORA.error("evento {} ({}) FALLIDO tras {} intentos: {}", id, tipo, intentos, mensajeError, causa);
            return;
        }

        OffsetDateTime proximoIntento = OffsetDateTime.now().plus(backoffConJitter(intentos));
        actualizacion
                .set(DSL.field("estado", String.class), "PENDIENTE")
                .set(DSL.field("proximo_intento_en", OffsetDateTime.class), proximoIntento)
                .where(DSL.field("id").eq(id))
                .execute();
        BITACORA.warn(
                "no se pudo publicar el evento {} ({}), intento {}/{}: reintenta en {}",
                id,
                tipo,
                intentos,
                intentosMaximos,
                proximoIntento,
                causa);
    }

    /** Exponencial desde {@code backoff-base}, tope {@code backoff-tope}, jitter ±20 % (Q-02). */
    private Duration backoffConJitter(int intentos) {
        long baseMs = backoffBase.toMillis();
        long topeMs = backoffTope.toMillis();
        long exponencialMs = baseMs * (1L << Math.min(intentos - 1, 20));
        long acotadoMs = Math.min(exponencialMs, topeMs);
        double factorJitter = 1.0 + (ThreadLocalRandom.current().nextDouble(-0.2, 0.2));
        long conJitterMs = Math.max(1, Math.round(acotadoMs * factorJitter));
        return Duration.ofMillis(Math.min(conJitterMs, topeMs));
    }

    private void medirEdad(List<Record> tomados) {
        // La edad se mide sobre lo que se ACABA de tomar, no sobre todo lo PENDIENTE: es
        // una aproximacion barata (sin otra consulta) que igual detecta un outbox atascado,
        // porque un atasco real deja tandas viejas en cada corrida.
        if (tomados.isEmpty()) {
            edadDelMasViejo.set(0);
            return;
        }
        OffsetDateTime masViejo = tomados.get(0).get("ocurrido_en", OffsetDateTime.class);
        edadDelMasViejo.set(
                Duration.between(masViejo.toInstant(), Instant.now()).toSeconds());
    }

    private Integer contarPorEstado(String estado) {
        return dsl.selectCount()
                .from(DSL.table(DSL.name(esquema, "evento_dominio")))
                .where(DSL.field("estado").eq(estado))
                .fetchOne(0, Integer.class);
    }
}
