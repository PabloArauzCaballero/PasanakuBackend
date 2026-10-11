package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * {@code rescate_inversion}. La doble disponibilidad la rechaza la BASE
 * ({@code tg_rescate_inversion_doble_disponibilidad}, R-INV-05) bloqueando la posicion;
 * este repositorio solo traduce esa excepcion para que la aplicacion responda con su codigo.
 */
@Component
public class RescateRepositorio {

    public record Rescate(
            UUID id,
            UUID posicionId,
            UUID usuarioId,
            String tipo,
            BigDecimal cuotas,
            String estado,
            String hashSolicitud,
            LocalDate fechaValor,
            OffsetDateTime liquidaEn,
            String transaccionExterna,
            String motivoRechazo) {}

    /** Falla con {@link DobleDisponibilidad} si las cuotas ya estan comprometidas en otro rescate. */
    public UUID crear(
            DSLContext dsl,
            UUID posicionId,
            UUID usuarioId,
            String tipo,
            BigDecimal cuotas,
            String clave,
            String hashSolicitud,
            LocalDate fechaValor,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        try {
            dsl.insertInto(t("rescate_inversion"))
                    .set(f("id", UUID.class), id)
                    .set(f("posicion_inversion_id", UUID.class), posicionId)
                    .set(f("usuario_id", UUID.class), usuarioId)
                    .set(f("tipo", String.class), tipo)
                    .set(f("cuotas", BigDecimal.class), cuotas)
                    .set(f("estado", String.class), "SOLICITADO")
                    .set(f("clave_idempotencia", String.class), clave)
                    .set(f("hash_solicitud", String.class), hashSolicitud)
                    .set(f("fecha_valor", LocalDate.class), fechaValor)
                    .set(f("version", Integer.class), 0)
                    .set(f("solicitada_en", OffsetDateTime.class), ahora)
                    .set(f("actualizada_en", OffsetDateTime.class), ahora)
                    .execute();
        } catch (org.jooq.exception.DataAccessException e) {
            String mensaje = String.valueOf(e.getMessage());
            if (mensaje.contains("R-INV-05") || mensaje.contains("uq_rescate_inv_dpf_vigente")) {
                throw new DobleDisponibilidad();
            }
            throw e;
        }
        return id;
    }

    public Optional<Rescate> porId(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("rescate_inversion"))
                .where(f("id", UUID.class).eq(id))
                .fetchOptional(this::rescate);
    }

    public Optional<Rescate> porIdBloqueando(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("rescate_inversion"))
                .where(f("id", UUID.class).eq(id))
                .forUpdate()
                .fetchOptional(this::rescate);
    }

    public Optional<Rescate> porClave(DSLContext dsl, UUID usuarioId, String clave) {
        return dsl.selectFrom(t("rescate_inversion"))
                .where(f("usuario_id", UUID.class)
                        .eq(usuarioId)
                        .and(f("clave_idempotencia", String.class).eq(clave)))
                .fetchOptional(this::rescate);
    }

    public List<Rescate> enEstados(DSLContext dsl, Collection<String> estados, int limite) {
        return dsl.selectFrom(t("rescate_inversion"))
                .where(f("estado", String.class).in(estados))
                .orderBy(f("solicitada_en", OffsetDateTime.class))
                .limit(limite)
                .fetch(this::rescate);
    }

    /** Las cuotas de la posicion que todavia no liquido el aliado (para mostrar y para el cuadre). */
    public BigDecimal cuotasEnCurso(DSLContext dsl, UUID posicionId) {
        BigDecimal suma = dsl.select(org.jooq.impl.DSL.sum(f("cuotas", BigDecimal.class)))
                .from(t("rescate_inversion"))
                .where(f("posicion_inversion_id", UUID.class)
                        .eq(posicionId)
                        .and(f("estado", String.class).in("SOLICITADO", "PENDIENTE", "INCIERTO")))
                .fetchOne(0, BigDecimal.class);
        return suma == null ? BigDecimal.ZERO : suma;
    }

    public boolean pasar(
            DSLContext dsl,
            UUID id,
            Collection<String> desde,
            String hacia,
            LocalDate fechaValor,
            OffsetDateTime liquidaEn,
            String transaccionExterna,
            String motivoRechazo,
            OffsetDateTime ahora) {
        var q = dsl.update(t("rescate_inversion"))
                .set(f("estado", String.class), hacia)
                .set(f("version", Integer.class), f("version", Integer.class).plus(1))
                .set(f("actualizada_en", OffsetDateTime.class), ahora);
        if (fechaValor != null) {
            q = q.set(f("fecha_valor", LocalDate.class), fechaValor);
        }
        if (liquidaEn != null) {
            q = q.set(f("liquida_en", OffsetDateTime.class), liquidaEn);
        }
        if (transaccionExterna != null) {
            q = q.set(f("transaccion_externa", String.class), transaccionExterna);
        }
        if (motivoRechazo != null) {
            q = q.set(f("motivo_rechazo", String.class), motivoRechazo);
        }
        return q.where(f("id", UUID.class).eq(id).and(f("estado", String.class).in(desde)))
                        .execute()
                == 1;
    }

    private Rescate rescate(Record r) {
        return new Rescate(
                r.get("id", UUID.class),
                r.get("posicion_inversion_id", UUID.class),
                r.get("usuario_id", UUID.class),
                r.get("tipo", String.class),
                r.get("cuotas", BigDecimal.class),
                r.get("estado", String.class),
                r.get("hash_solicitud", String.class),
                r.get("fecha_valor", LocalDate.class),
                r.get("liquida_en", OffsetDateTime.class),
                r.get("transaccion_externa", String.class),
                r.get("motivo_rechazo", String.class));
    }

    /** Las cuotas (o el deposito) ya estan comprometidas en otro rescate en curso. */
    public static class DobleDisponibilidad extends RuntimeException {
        public DobleDisponibilidad() {
            super("doble disponibilidad");
        }
    }
}
