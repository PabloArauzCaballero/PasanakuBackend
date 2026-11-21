package bo.aportaya.nucleofinanciero.infraestructura;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code solicitud_reverso}: el primer acto de un reverso con cuatro ojos reales (P-13, R-BIL-27).
 *
 * <p>Quien pide queda guardado aca, tomado de su token; quien autoriza lo escribe la segunda
 * peticion, con el suyo. La precondicion de estado va en el {@code UPDATE} (regla 96.3.2): si
 * otra autorizacion gano la carrera, la segunda no cambia ninguna fila.
 */
@Component
public class SolicitudReversoRepositorio {

    private static final Table<Record> TABLA = DSL.table(DSL.name("nucleo_financiero", "solicitud_reverso"));

    /**
     * Inserta la solicitud. Devuelve vacio si choca con una unicidad: la misma clave (un reintento) o
     * ya hay otra solicitud viva para esa transaccion ({@code uq_solicitud_reverso_viva}).
     */
    public Optional<UUID> crear(
            DSLContext dsl,
            UUID transaccionOriginalId,
            UUID solicitadaPor,
            String claveIdempotencia,
            String tipo,
            String motivo,
            OffsetDateTime ahora) {
        return dsl.insertInto(TABLA)
                .set(DSL.field("transaccion_original_id", UUID.class), transaccionOriginalId)
                .set(DSL.field("solicitada_por", UUID.class), solicitadaPor)
                .set(DSL.field("clave_idempotencia", String.class), claveIdempotencia)
                .set(DSL.field("tipo", String.class), tipo)
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("estado", String.class), "SOLICITADA")
                .set(DSL.field("solicitada_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .returningResult(DSL.field("id", UUID.class))
                .fetchOptional()
                .map(r -> r.value1());
    }

    public Optional<Solicitud> porClave(DSLContext dsl, String claveIdempotencia) {
        return dsl.select(columnas())
                .from(TABLA)
                .where(DSL.field("clave_idempotencia", String.class).eq(claveIdempotencia))
                .fetchOptional(SolicitudReversoRepositorio::mapear);
    }

    public Optional<Solicitud> ver(DSLContext dsl, UUID solicitudId) {
        return dsl.select(columnas())
                .from(TABLA)
                .where(DSL.field("id", UUID.class).eq(solicitudId))
                .fetchOptional(SolicitudReversoRepositorio::mapear);
    }

    /** La solicitud bloqueada para resolverla: dos autorizaciones simultaneas no la ejecutan dos veces. */
    public Optional<Solicitud> bloquear(DSLContext dsl, UUID solicitudId) {
        return dsl.select(columnas())
                .from(TABLA)
                .where(DSL.field("id", UUID.class).eq(solicitudId))
                .forUpdate()
                .fetchOptional(SolicitudReversoRepositorio::mapear);
    }

    /** {@code SOLICITADA -> EJECUTADA}, solo si sigue pendiente. */
    public boolean marcarEjecutada(
            DSLContext dsl, UUID solicitudId, UUID autorizadaPor, UUID reversoTransaccionId, OffsetDateTime ahora) {
        return dsl.update(TABLA)
                        .set(DSL.field("estado", String.class), "EJECUTADA")
                        .set(DSL.field("autorizada_por", UUID.class), autorizadaPor)
                        .set(DSL.field("reverso_transaccion_id", UUID.class), reversoTransaccionId)
                        .set(DSL.field("resuelta_en", OffsetDateTime.class), ahora)
                        .where(DSL.field("id", UUID.class).eq(solicitudId))
                        .and(DSL.field("estado", String.class).eq("SOLICITADA"))
                        .execute()
                == 1;
    }

    private static org.jooq.SelectFieldOrAsterisk[] columnas() {
        return new org.jooq.SelectFieldOrAsterisk[] {
            DSL.field("id", UUID.class),
            DSL.field("transaccion_original_id", UUID.class),
            DSL.field("solicitada_por", UUID.class),
            DSL.field("tipo", String.class),
            DSL.field("motivo", String.class),
            DSL.field("estado", String.class)
        };
    }

    private static Solicitud mapear(Record r) {
        return new Solicitud(
                r.get("id", UUID.class),
                r.get("transaccion_original_id", UUID.class),
                r.get("solicitada_por", UUID.class),
                r.get("tipo", String.class),
                r.get("motivo", String.class),
                r.get("estado", String.class));
    }

    public record Solicitud(
            UUID id, UUID transaccionOriginalId, UUID solicitadaPor, String tipo, String motivo, String estado) {

        public boolean pendiente() {
            return "SOLICITADA".equals(estado);
        }
    }
}
