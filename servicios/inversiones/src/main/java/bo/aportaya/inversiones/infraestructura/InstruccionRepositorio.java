package bo.aportaya.inversiones.infraestructura;

import static bo.aportaya.inversiones.infraestructura.Sql.f;
import static bo.aportaya.inversiones.infraestructura.Sql.t;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * {@code instruccion_libro}: la intencion de mover plata en el libro, persistida ANTES de
 * pedirselo a {@code nucleo-financiero}.
 *
 * <p>La clave es determinista ({@code TIPO:origen}), asi que repetir la creacion no crea
 * una segunda instruccion y repetir la aplicacion no mueve dos veces: el libro tambien
 * recibe esa clave.
 */
@Component
public class InstruccionRepositorio {

    public record Instruccion(
            UUID id,
            String origenTipo,
            UUID origenId,
            String tipo,
            UUID usuarioId,
            UUID cuentaId,
            BigDecimal monto,
            String moneda,
            String clave,
            String estado,
            UUID referenciaLibro,
            int intentos) {}

    public UUID crear(
            DSLContext dsl,
            String origenTipo,
            UUID origenId,
            String tipo,
            UUID usuarioId,
            UUID cuentaId,
            BigDecimal monto,
            String moneda,
            OffsetDateTime ahora) {
        String clave = tipo + ":" + origenId;
        dsl.insertInto(t("instruccion_libro"))
                .set(f("id", UUID.class), UUID.randomUUID())
                .set(f("origen_tipo", String.class), origenTipo)
                .set(f("origen_id", UUID.class), origenId)
                .set(f("tipo", String.class), tipo)
                .set(f("usuario_id", UUID.class), usuarioId)
                .set(f("cuenta_billetera_id", UUID.class), cuentaId)
                .set(f("monto", BigDecimal.class), monto)
                .set(f("moneda", String.class), moneda)
                .set(f("clave_idempotencia", String.class), clave)
                .set(f("estado", String.class), "PENDIENTE")
                .set(f("intentos", Integer.class), 0)
                .set(f("creada_en", OffsetDateTime.class), ahora)
                .onConflictDoNothing()
                .execute();
        return dsl.select(f("id", UUID.class))
                .from(t("instruccion_libro"))
                .where(f("clave_idempotencia", String.class).eq(clave))
                .fetchOne(0, UUID.class);
    }

    public Optional<Instruccion> porId(DSLContext dsl, UUID id) {
        return dsl.selectFrom(t("instruccion_libro"))
                .where(f("id", UUID.class).eq(id))
                .fetchOptional(this::instruccion);
    }

    public Optional<Instruccion> deOrigen(DSLContext dsl, UUID origenId, String tipo) {
        return dsl.selectFrom(t("instruccion_libro"))
                .where(f("clave_idempotencia", String.class).eq(tipo + ":" + origenId))
                .fetchOptional(this::instruccion);
    }

    public List<Instruccion> pendientes(DSLContext dsl, int limite) {
        return dsl.selectFrom(t("instruccion_libro"))
                .where(f("estado", String.class).eq("PENDIENTE"))
                .orderBy(f("creada_en", OffsetDateTime.class))
                .limit(limite)
                .fetch(this::instruccion);
    }

    public boolean aplicada(DSLContext dsl, UUID id, UUID referenciaLibro, OffsetDateTime ahora) {
        return dsl.update(t("instruccion_libro"))
                        .set(f("estado", String.class), "APLICADA")
                        .set(f("referencia_libro", UUID.class), referenciaLibro)
                        .set(f("aplicada_en", OffsetDateTime.class), ahora)
                        .where(f("id", UUID.class)
                                .eq(id)
                                .and(f("estado", String.class).eq("PENDIENTE")))
                        .execute()
                == 1;
    }

    public void fallida(DSLContext dsl, UUID id, String codigo) {
        dsl.update(t("instruccion_libro"))
                .set(f("estado", String.class), "FALLIDA")
                .set(f("ultimo_error", String.class), codigo)
                .where(f("id", UUID.class).eq(id).and(f("estado", String.class).eq("PENDIENTE")))
                .execute();
    }

    public void registrarIntento(DSLContext dsl, UUID id, String codigo) {
        dsl.update(t("instruccion_libro"))
                .set(f("intentos", Integer.class), f("intentos", Integer.class).plus(1))
                .set(f("ultimo_error", String.class), codigo)
                .where(f("id", UUID.class).eq(id).and(f("estado", String.class).eq("PENDIENTE")))
                .execute();
    }

    private Instruccion instruccion(Record r) {
        return new Instruccion(
                r.get("id", UUID.class),
                r.get("origen_tipo", String.class),
                r.get("origen_id", UUID.class),
                r.get("tipo", String.class),
                r.get("usuario_id", UUID.class),
                r.get("cuenta_billetera_id", UUID.class),
                r.get("monto", BigDecimal.class),
                r.get("moneda", String.class),
                r.get("clave_idempotencia", String.class),
                r.get("estado", String.class),
                r.get("referencia_libro", UUID.class),
                r.get("intentos", Integer.class));
    }
}
