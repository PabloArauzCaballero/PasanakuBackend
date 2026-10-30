package bo.aportaya.entregas.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Las pujas de una subasta del cupo ({@code entregas.puja_subasta}). */
@Component
public class PujaRepositorio {

    public record Puja(
            UUID id,
            UUID subastaId,
            UUID postorUsuarioId,
            Dinero monto,
            String estado,
            Optional<UUID> retencionRef,
            int version) {}

    private static org.jooq.Table<?> pujas() {
        return DSL.table(DSL.name("entregas", "puja_subasta"));
    }

    private static <T> org.jooq.Field<T> f(String nombre, Class<T> tipo) {
        return DSL.field(DSL.name(nombre), tipo);
    }

    public UUID crearPuja(
            DSLContext dsl, UUID subastaId, UUID postor, Dinero monto, String clave, OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(pujas())
                .set(f("id", UUID.class), id)
                .set(f("subasta_cupo_id", UUID.class), subastaId)
                .set(f("postor_usuario_id", UUID.class), postor)
                .set(f("moneda", String.class), monto.moneda().name())
                .set(f("monto", BigDecimal.class), monto.monto())
                .set(f("estado", String.class), "PENDIENTE")
                .set(f("clave_idempotencia", String.class), clave)
                .set(f("creada_en", OffsetDateTime.class), ahora)
                .set(f("version", Integer.class), 0)
                .execute();
        return id;
    }

    public Optional<Puja> pujaPorClave(DSLContext dsl, String clave) {
        return leerPuja(dsl, f("clave_idempotencia", String.class).eq(clave));
    }

    public Optional<Puja> puja(DSLContext dsl, UUID id) {
        return leerPuja(dsl, f("id", UUID.class).eq(id));
    }

    /** La puja que hoy manda: a lo sumo una, porque solo se marca VIGENTE con la subasta bloqueada. */
    public Optional<Puja> vigente(DSLContext dsl, UUID subastaId) {
        return leerPuja(
                dsl,
                f("subasta_cupo_id", UUID.class)
                        .eq(subastaId)
                        .and(f("estado", String.class).eq("VIGENTE")));
    }

    /** La ultima puja de esa persona en esa subasta, para mostrarle como va. */
    public Optional<Puja> ultimaDe(DSLContext dsl, UUID subastaId, UUID postor) {
        return dsl.select(f("id", UUID.class))
                .from(pujas())
                .where(f("subasta_cupo_id", UUID.class).eq(subastaId))
                .and(f("postor_usuario_id", UUID.class).eq(postor))
                .orderBy(f("creada_en", OffsetDateTime.class).desc(), f("id", UUID.class))
                .limit(1)
                .fetchOptional(f("id", UUID.class))
                .flatMap(id -> puja(dsl, id));
    }

    /** Las pujas que llegaron a quedar firmes alguna vez (no las rechazadas ni las que no se retuvieron). */
    public int cantidadDePujas(DSLContext dsl, UUID subastaId) {
        return dsl.fetchCount(
                pujas(),
                f("subasta_cupo_id", UUID.class)
                        .eq(subastaId)
                        .and(f("estado", String.class).in("VIGENTE", "SUPERADA", "LIBERADA", "GANADORA")));
    }

    /** Superadas con plata retenida todavia: lo que hay que devolver (si la billetera no respondio, se reintenta). */
    public List<Puja> porDevolver(DSLContext dsl, Optional<UUID> subastaId, int limite) {
        Condition donde = f("estado", String.class)
                .eq("SUPERADA")
                .and(f("retencion_ref", UUID.class).isNotNull());
        if (subastaId.isPresent()) {
            donde = donde.and(f("subasta_cupo_id", UUID.class).eq(subastaId.get()));
        }
        return dsl
                .select(f("id", UUID.class))
                .from(pujas())
                .where(donde)
                .orderBy(f("creada_en", OffsetDateTime.class), f("id", UUID.class))
                .limit(limite)
                .fetch(f("id", UUID.class))
                .stream()
                .map(id -> puja(dsl, id).orElseThrow())
                .toList();
    }

    public boolean moverPuja(DSLContext dsl, Puja p, List<String> desde, String hacia, Optional<UUID> retencionRef) {
        var update = dsl.update(pujas())
                .set(f("estado", String.class), hacia)
                .set(f("version", Integer.class), p.version() + 1);
        if (retencionRef.isPresent()) {
            update = update.set(f("retencion_ref", UUID.class), retencionRef.get());
        }
        return update.where(f("id", UUID.class).eq(p.id()))
                        .and(f("estado", String.class).in(desde))
                        .and(f("version", Integer.class).eq(p.version()))
                        .execute()
                == 1;
    }

    private Optional<Puja> leerPuja(DSLContext dsl, Condition donde) {
        Record fila = dsl.select(
                        f("id", UUID.class),
                        f("subasta_cupo_id", UUID.class),
                        f("postor_usuario_id", UUID.class),
                        f("moneda", String.class),
                        f("monto", BigDecimal.class),
                        f("estado", String.class),
                        f("retencion_ref", UUID.class),
                        f("version", Integer.class))
                .from(pujas())
                .where(donde)
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(r -> new Puja(
                        r.get("id", UUID.class),
                        r.get("subasta_cupo_id", UUID.class),
                        r.get("postor_usuario_id", UUID.class),
                        Dinero.de(r.get("monto", BigDecimal.class), Moneda.valueOf(r.get("moneda", String.class))),
                        r.get("estado", String.class),
                        Optional.ofNullable(r.get("retencion_ref", UUID.class)),
                        r.get("version", Integer.class)));
    }
}
