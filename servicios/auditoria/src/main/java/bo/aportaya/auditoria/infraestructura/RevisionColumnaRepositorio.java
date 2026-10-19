package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.dominio.RevisionDeColumna;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Lee y guarda {@code auditoria.revision_columna} (CU-131). Mismo patron que {@link FichaGobiernoRepositorio}: la
 * version es la condicion de la escritura, y cero filas afectadas significa que alguien guardo antes.
 */
@Component
public class RevisionColumnaRepositorio {

    private static final org.jooq.Name TABLA = DSL.name("auditoria", "revision_columna");

    /** Todas las revisiones guardadas, por {@code esquema.tabla.columna}. */
    public Map<String, Guardada> todas(DSLContext dsl) {
        Map<String, Guardada> revisiones = new HashMap<>();
        for (Record f : dsl.select().from(DSL.table(TABLA)).fetch()) {
            revisiones.put(
                    RevisionDeColumna.clave(
                            f.get("esquema", String.class),
                            f.get("tabla", String.class),
                            f.get("columna", String.class)),
                    leer(f));
        }
        return revisiones;
    }

    public Optional<Guardada> de(DSLContext dsl, String esquema, String tabla, String columna) {
        return dsl.select()
                .from(DSL.table(TABLA))
                .where(DSL.field("esquema", String.class).eq(esquema))
                .and(DSL.field("tabla", String.class).eq(tabla))
                .and(DSL.field("columna", String.class).eq(columna))
                .fetchOptional()
                .map(RevisionColumnaRepositorio::leer);
    }

    public Optional<UUID> guardar(
            DSLContext dsl,
            String esquema,
            String tabla,
            String columna,
            RevisionDeColumna revision,
            int versionLeida,
            UUID actor,
            OffsetDateTime ahora) {
        if (versionLeida == 0) {
            return dsl.insertInto(DSL.table(TABLA))
                    .set(DSL.field("esquema", String.class), esquema)
                    .set(DSL.field("tabla", String.class), tabla)
                    .set(DSL.field("columna", String.class), columna)
                    .set(valores(revision, actor, ahora))
                    .set(DSL.field("version", Integer.class), 1)
                    .onConflict(DSL.field("tabla"), DSL.field("columna"), DSL.field("esquema"))
                    .doNothing()
                    .returningResult(DSL.field("id", UUID.class))
                    .fetchOptional()
                    .map(r -> r.value1());
        }
        return dsl.update(DSL.table(TABLA))
                .set(valores(revision, actor, ahora))
                .set(DSL.field("version", Integer.class), versionLeida + 1)
                .where(DSL.field("esquema", String.class).eq(esquema))
                .and(DSL.field("tabla", String.class).eq(tabla))
                .and(DSL.field("columna", String.class).eq(columna))
                .and(DSL.field("version", Integer.class).eq(versionLeida))
                .returningResult(DSL.field("id", UUID.class))
                .fetchOptional()
                .map(r -> r.value1());
    }

    private static Map<org.jooq.Field<?>, Object> valores(RevisionDeColumna r, UUID actor, OffsetDateTime ahora) {
        Map<org.jooq.Field<?>, Object> v = new HashMap<>();
        v.put(DSL.field("decision", String.class), r.decision());
        v.put(DSL.field("confianza", String.class), r.confianza().orElse(null));
        v.put(DSL.field("motivo", String.class), r.motivo().orElse(null));
        v.put(DSL.field("revisado_por", UUID.class), actor);
        v.put(DSL.field("revisado_en", OffsetDateTime.class), ahora);
        return v;
    }

    private static Guardada leer(Record f) {
        return new Guardada(
                f.get("id", UUID.class),
                new RevisionDeColumna(
                        f.get("decision", String.class),
                        Optional.ofNullable(f.get("confianza", String.class)),
                        Optional.ofNullable(f.get("motivo", String.class)),
                        Optional.ofNullable(f.get("revisado_por", UUID.class)),
                        Optional.ofNullable(f.get("revisado_en", OffsetDateTime.class)),
                        f.get("version", Integer.class)));
    }

    public record Guardada(UUID id, RevisionDeColumna revision) {}
}
