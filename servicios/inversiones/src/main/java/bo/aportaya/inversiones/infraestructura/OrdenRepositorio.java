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
 * {@code consentimiento_inversion} y {@code orden_inversion}.
 *
 * <p>Los cambios de estado son {@code UPDATE ... WHERE estado IN (...)}: la precondicion
 * va en la escritura, asi que dos actores que avanzan la misma orden a la vez no la
 * pisan, solo uno gana y el otro lo ve en el conteo de filas.
 */
@Component
public class OrdenRepositorio {

    public record Orden(
            UUID id,
            UUID usuarioId,
            UUID productoId,
            UUID versionId,
            UUID consentimientoId,
            UUID cuentaId,
            BigDecimal monto,
            String moneda,
            String estado,
            String hashSolicitud,
            String transaccionExterna,
            LocalDate fechaValor,
            String motivoRechazo) {}

    public record Consentimiento(UUID id, UUID versionId, String textoHash, String hashSolicitud) {}

    /** El consentimiento se inserta primero: su UNIQUE (titular, clave) es lo que decide la carrera. */
    public boolean insertarConsentimiento(
            DSLContext dsl,
            UUID id,
            UUID usuarioId,
            UUID versionId,
            String textoHash,
            String clave,
            String hashSolicitud,
            OffsetDateTime ahora) {
        return dsl.insertInto(t("consentimiento_inversion"))
                        .set(f("id", UUID.class), id)
                        .set(f("usuario_id", UUID.class), usuarioId)
                        .set(f("version_condiciones_id", UUID.class), versionId)
                        .set(f("texto_hash", String.class), textoHash)
                        .set(f("clave_idempotencia", String.class), clave)
                        .set(f("hash_solicitud", String.class), hashSolicitud)
                        .set(f("aceptado_en", OffsetDateTime.class), ahora)
                        .onConflictDoNothing()
                        .execute()
                == 1;
    }

    public Optional<Consentimiento> consentimientoPorClave(DSLContext dsl, UUID usuarioId, String clave) {
        return dsl.selectFrom(t("consentimiento_inversion"))
                .where(f("usuario_id", UUID.class)
                        .eq(usuarioId)
                        .and(f("clave_idempotencia", String.class).eq(clave)))
                .fetchOptional(r -> new Consentimiento(
                        r.get("id", UUID.class),
                        r.get("version_condiciones_id", UUID.class),
                        r.get("texto_hash", String.class),
                        r.get("hash_solicitud", String.class)));
    }

    public void insertarOrden(
            DSLContext dsl,
            UUID id,
            UUID usuarioId,
            UUID productoId,
            UUID versionId,
            UUID consentimientoId,
            UUID cuentaId,
            BigDecimal monto,
            String moneda,
            String clave,
            String hashSolicitud,
            OffsetDateTime ahora) {
        dsl.insertInto(t("orden_inversion"))
                .set(f("id", UUID.class), id)
                .set(f("usuario_id", UUID.class), usuarioId)
                .set(f("producto_inversion_id", UUID.class), productoId)
                .set(f("version_condiciones_id", UUID.class), versionId)
                .set(f("consentimiento_inversion_id", UUID.class), consentimientoId)
                .set(f("cuenta_billetera_id", UUID.class), cuentaId)
                .set(f("monto", BigDecimal.class), monto)
                .set(f("moneda", String.class), moneda)
                .set(f("estado", String.class), "CREADA")
                .set(f("clave_idempotencia", String.class), clave)
                .set(f("hash_solicitud", String.class), hashSolicitud)
                .set(f("version", Integer.class), 0)
                .set(f("creada_en", OffsetDateTime.class), ahora)
                .set(f("actualizada_en", OffsetDateTime.class), ahora)
                .execute();
    }

    public Optional<Orden> porId(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("orden_inversion"))
                .where(f("id", UUID.class).eq(id))
                .fetchOptional(this::orden);
    }

    public Optional<Orden> porIdBloqueando(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("orden_inversion"))
                .where(f("id", UUID.class).eq(id))
                .forUpdate()
                .fetchOptional(this::orden);
    }

    public Optional<Orden> porClave(DSLContext dsl, UUID usuarioId, String clave) {
        return dsl.selectFrom(t("orden_inversion"))
                .where(f("usuario_id", UUID.class)
                        .eq(usuarioId)
                        .and(f("clave_idempotencia", String.class).eq(clave)))
                .fetchOptional(this::orden);
    }

    public List<Orden> enEstados(DSLContext dsl, Collection<String> estados, int limite) {
        return dsl.selectFrom(t("orden_inversion"))
                .where(f("estado", String.class).in(estados))
                .orderBy(f("creada_en", OffsetDateTime.class))
                .limit(limite)
                .fetch(this::orden);
    }

    /** @return {@code true} si esta transicion la hizo este actor. */
    public boolean pasar(DSLContext dsl, UUID id, Collection<String> desde, String hacia, OffsetDateTime ahora) {
        return dsl.update(t("orden_inversion"))
                        .set(f("estado", String.class), hacia)
                        .set(
                                f("version", Integer.class),
                                f("version", Integer.class).plus(1))
                        .set(f("actualizada_en", OffsetDateTime.class), ahora)
                        .where(f("id", UUID.class)
                                .eq(id)
                                .and(f("estado", String.class).in(desde)))
                        .execute()
                == 1;
    }

    public boolean confirmar(
            DSLContext dsl,
            UUID id,
            Collection<String> desde,
            String transaccionExterna,
            LocalDate fechaValor,
            OffsetDateTime ahora) {
        return dsl.update(t("orden_inversion"))
                        .set(f("estado", String.class), "CONFIRMADA")
                        .set(f("transaccion_externa", String.class), transaccionExterna)
                        .set(f("fecha_valor", LocalDate.class), fechaValor)
                        .set(
                                f("version", Integer.class),
                                f("version", Integer.class).plus(1))
                        .set(f("actualizada_en", OffsetDateTime.class), ahora)
                        .where(f("id", UUID.class)
                                .eq(id)
                                .and(f("estado", String.class).in(desde)))
                        .execute()
                == 1;
    }

    public boolean rechazar(DSLContext dsl, UUID id, Collection<String> desde, String motivo, OffsetDateTime ahora) {
        return dsl.update(t("orden_inversion"))
                        .set(f("estado", String.class), "RECHAZADA")
                        .set(f("motivo_rechazo", String.class), motivo)
                        .set(
                                f("version", Integer.class),
                                f("version", Integer.class).plus(1))
                        .set(f("actualizada_en", OffsetDateTime.class), ahora)
                        .where(f("id", UUID.class)
                                .eq(id)
                                .and(f("estado", String.class).in(desde)))
                        .execute()
                == 1;
    }

    private Orden orden(Record r) {
        return new Orden(
                r.get("id", UUID.class),
                r.get("usuario_id", UUID.class),
                r.get("producto_inversion_id", UUID.class),
                r.get("version_condiciones_id", UUID.class),
                r.get("consentimiento_inversion_id", UUID.class),
                r.get("cuenta_billetera_id", UUID.class),
                r.get("monto", BigDecimal.class),
                r.get("moneda", String.class),
                r.get("estado", String.class),
                r.get("hash_solicitud", String.class),
                r.get("transaccion_externa", String.class),
                r.get("fecha_valor", LocalDate.class),
                r.get("motivo_rechazo", String.class));
    }
}
