package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code corrida_trabajo}: una fila por operacion programable y clave de idempotencia
 * ({@code R-INV-10}). Sirve a dos cosas a la vez: que repetir una operacion con la misma clave
 * no la corra dos veces, y que se vea cuando corrio cada trabajo y como termino.
 *
 * <p>Reservar es un {@code INSERT ... ON CONFLICT DO NOTHING} contra el indice unico de
 * (operacion, clave): entre dos pedidos simultaneos solo uno gana, y no depende de un
 * {@code SELECT} previo. El otro lee lo que dejo el ganador.
 */
@Component
public class CorridaRepositorio {

    public record Corrida(
            UUID id,
            String operacion,
            String clave,
            String hashSolicitud,
            String origen,
            String estado,
            String resultado,
            String error,
            OffsetDateTime iniciadaEn,
            OffsetDateTime terminadaEn) {}

    /** Que paso al reservar: quien llama decide segun esto. */
    public enum Reserva {
        /** La reserva es nueva, o era de una corrida fallida o caida que se retoma: hay que correr. */
        CORRER,
        /** Ya termino bien con este mismo contenido: se devuelve lo guardado. */
        REPETIDA,
        /** Otra corrida con esta clave sigue en curso y no esta vencida. */
        EN_PROCESO,
        /** La clave ya se uso con otro contenido. */
        CONTENIDO_DISTINTO
    }

    public Reserva reservar(
            DSLContext dsl,
            String operacion,
            String clave,
            String hash,
            String origen,
            OffsetDateTime ahora,
            Duration caducidadEnCurso) {
        int filas = dsl.insertInto(t("corrida_trabajo"))
                .set(f("id", UUID.class), UUID.randomUUID())
                .set(f("operacion", String.class), operacion)
                .set(f("clave_idempotencia", String.class), clave)
                .set(f("hash_solicitud", String.class), hash)
                .set(f("origen", String.class), origen)
                .set(f("estado", String.class), "EN_CURSO")
                .set(f("iniciada_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .execute();
        if (filas == 1) {
            return Reserva.CORRER;
        }
        // Se bloquea la fila: dos retomas simultaneas de una corrida fallida no corren las dos.
        Corrida previa = dsl.selectFrom(t("corrida_trabajo"))
                .where(f("operacion", String.class).eq(operacion))
                .and(f("clave_idempotencia", String.class).eq(clave))
                .forUpdate()
                .fetchOne(this::corrida);
        if (!previa.hashSolicitud().equals(hash)) {
            return Reserva.CONTENIDO_DISTINTO;
        }
        return switch (previa.estado()) {
            case "OK", "PARCIAL" -> Reserva.REPETIDA;
            case "FALLIDA" -> retomar(dsl, previa, origen, ahora);
            default ->
                previa.iniciadaEn().plus(caducidadEnCurso).isBefore(ahora)
                        ? retomar(dsl, previa, origen, ahora)
                        : Reserva.EN_PROCESO;
        };
    }

    private Reserva retomar(DSLContext dsl, Corrida previa, String origen, OffsetDateTime ahora) {
        dsl.update(t("corrida_trabajo"))
                .set(f("estado", String.class), "EN_CURSO")
                .set(f("origen", String.class), origen)
                .set(f("iniciada_en", OffsetDateTime.class), ahora)
                .set(f("terminada_en", OffsetDateTime.class), (OffsetDateTime) null)
                .set(f("error", String.class), (String) null)
                .set(f("resultado", String.class), (String) null)
                .where(f("id", UUID.class).eq(previa.id()))
                .execute();
        return Reserva.CORRER;
    }

    public Optional<Corrida> porClave(DSLContext dsl, String operacion, String clave) {
        return dsl.selectFrom(t("corrida_trabajo"))
                .where(f("operacion", String.class).eq(operacion))
                .and(f("clave_idempotencia", String.class).eq(clave))
                .fetchOptional(this::corrida);
    }

    public void terminar(
            DSLContext dsl,
            String operacion,
            String clave,
            String estado,
            String resultado,
            String error,
            OffsetDateTime ahora) {
        dsl.update(t("corrida_trabajo"))
                .set(f("estado", String.class), estado)
                .set(f("resultado", String.class), resultado)
                .set(f("error", String.class), error)
                .set(f("terminada_en", OffsetDateTime.class), ahora)
                .where(f("operacion", String.class).eq(operacion))
                .and(f("clave_idempotencia", String.class).eq(clave))
                .execute();
    }

    /** Las corridas mas recientes, de la mas nueva a la mas vieja; {@code operacion} nula = todas. */
    public List<Corrida> recientes(DSLContext dsl, String operacion, int limite) {
        Condition filtro = operacion == null
                ? DSL.trueCondition()
                : f("operacion", String.class).eq(operacion);
        var filtrada = dsl.selectFrom(t("corrida_trabajo"))
                .where(filtro)
                .orderBy(f("iniciada_en", OffsetDateTime.class).desc(), f("id", UUID.class));
        return filtrada.limit(limite).fetch(this::corrida);
    }

    private Corrida corrida(Record r) {
        return new Corrida(
                r.get("id", UUID.class),
                r.get("operacion", String.class),
                r.get("clave_idempotencia", String.class),
                r.get("hash_solicitud", String.class),
                r.get("origen", String.class),
                r.get("estado", String.class),
                r.get("resultado", String.class),
                r.get("error", String.class),
                r.get("iniciada_en", OffsetDateTime.class),
                r.get("terminada_en", OffsetDateTime.class));
    }
}
