package bo.aportaya.auditoria.infraestructura;

import bo.aportaya.auditoria.dominio.FichaDeGobierno;
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
 * Lee y guarda {@code auditoria.ficha_gobierno_tabla} (CU-130).
 *
 * <p><b>La version es la condicion de la escritura</b>, no un {@code if} previo: entre leer y escribir entra la
 * edicion de otra persona. La primera ficha de una tabla se inserta con {@code ON CONFLICT DO NOTHING} sobre el unico
 * (esquema, tabla); las siguientes son un {@code UPDATE ... WHERE version = leida}. En los dos casos, cero filas
 * afectadas significa que alguien guardo antes.
 */
@Component
public class FichaGobiernoRepositorio {

    private static final org.jooq.Name TABLA = DSL.name("auditoria", "ficha_gobierno_tabla");

    /** Todas las fichas guardadas, por {@code esquema.tabla}. */
    public Map<String, Guardada> todas(DSLContext dsl) {
        Map<String, Guardada> fichas = new HashMap<>();
        for (Record f : dsl.select().from(DSL.table(TABLA)).fetch()) {
            fichas.put(FichaDeGobierno.clave(f.get("esquema", String.class), f.get("tabla", String.class)), leer(f));
        }
        return fichas;
    }

    public Optional<Guardada> de(DSLContext dsl, String esquema, String tabla) {
        return dsl.select()
                .from(DSL.table(TABLA))
                .where(DSL.field("esquema", String.class).eq(esquema))
                .and(DSL.field("tabla", String.class).eq(tabla))
                .fetchOptional()
                .map(FichaGobiernoRepositorio::leer);
    }

    /**
     * Guarda la ficha con la version siguiente a {@code versionLeida}. Devuelve el id de la fila, o vacio si la version
     * leida ya no es la guardada.
     */
    public Optional<UUID> guardar(
            DSLContext dsl,
            String esquema,
            String tabla,
            FichaDeGobierno ficha,
            int versionLeida,
            UUID actor,
            OffsetDateTime ahora) {
        if (versionLeida == 0) {
            return dsl.insertInto(DSL.table(TABLA))
                    .set(DSL.field("esquema", String.class), esquema)
                    .set(DSL.field("tabla", String.class), tabla)
                    .set(valores(ficha, actor, ahora))
                    .set(DSL.field("version", Integer.class), 1)
                    .onConflict(DSL.field("esquema"), DSL.field("tabla"))
                    .doNothing()
                    .returningResult(DSL.field("id", UUID.class))
                    .fetchOptional()
                    .map(r -> r.value1());
        }
        return dsl.update(DSL.table(TABLA))
                .set(valores(ficha, actor, ahora))
                .set(DSL.field("version", Integer.class), versionLeida + 1)
                .where(DSL.field("esquema", String.class).eq(esquema))
                .and(DSL.field("tabla", String.class).eq(tabla))
                .and(DSL.field("version", Integer.class).eq(versionLeida))
                .returningResult(DSL.field("id", UUID.class))
                .fetchOptional()
                .map(r -> r.value1());
    }

    private static Map<org.jooq.Field<?>, Object> valores(FichaDeGobierno f, UUID actor, OffsetDateTime ahora) {
        Map<org.jooq.Field<?>, Object> v = new HashMap<>();
        v.put(DSL.field("estado", String.class), f.estado());
        v.put(DSL.field("estado_revision", String.class), f.estadoRevision());
        v.put(DSL.field("responsable", String.class), f.responsable().orElse(null));
        v.put(
                DSL.field("politica_retencion_id", UUID.class),
                f.politicaRetencionId().orElse(null));
        v.put(DSL.field("critica_para_auditoria", Boolean.class), f.criticaParaAuditoria());
        v.put(DSL.field("nota_revision", String.class), f.notaRevision().orElse(null));
        v.put(DSL.field("revisado_por", UUID.class), actor);
        v.put(DSL.field("actualizado_en", OffsetDateTime.class), ahora);
        return v;
    }

    private static Guardada leer(Record f) {
        return new Guardada(
                f.get("id", UUID.class),
                new FichaDeGobierno(
                        f.get("estado", String.class),
                        f.get("estado_revision", String.class),
                        Optional.ofNullable(f.get("responsable", String.class)),
                        Optional.ofNullable(f.get("politica_retencion_id", UUID.class)),
                        Boolean.TRUE.equals(f.get("critica_para_auditoria", Boolean.class)),
                        Optional.ofNullable(f.get("nota_revision", String.class)),
                        Optional.ofNullable(f.get("revisado_por", UUID.class)),
                        Optional.ofNullable(f.get("actualizado_en", OffsetDateTime.class)),
                        f.get("version", Integer.class)));
    }

    public record Guardada(UUID id, FichaDeGobierno ficha) {}
}
