package bo.aportaya.cumplimiento.infraestructura;

import bo.aportaya.cumplimiento.dominio.ContratoPublicado;
import bo.aportaya.cumplimiento.dominio.VersionAceptable;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Lee {@code cumplimiento.contrato_adhesion} y el tarifario publicado. */
@Component
public class ContratoRepositorio {

    /** El contrato de ese tipo que rige hoy, con su version y su hash. */
    public Optional<Contrato> vigentePorTipo(DSLContext dsl, String tipo) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("version", Short.class),
                        DSL.field("estado", String.class),
                        DSL.field("hash_documento", String.class))
                .from(DSL.table(DSL.name("cumplimiento", "contrato_adhesion")))
                .where(DSL.field("tipo").eq(tipo))
                .and(DSL.field("estado").eq("VIGENTE"))
                // Si hubiera dos vigentes por error, gana la version mas alta: nunca
                // se ata a nadie a la mas vieja de dos que la base dejo pasar.
                .orderBy(DSL.field("version").desc())
                .limit(1)
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Contrato(
                        f.get("id", UUID.class),
                        f.get("version", Short.class),
                        f.get("estado", String.class),
                        f.get("hash_documento", String.class)));
    }

    /**
     * Todos los contratos que rigen hoy, uno por tipo, con lo que hace falta para
     * mostrarlos y para poder aceptarlos.
     *
     * <p>Uno por tipo y no todos los vigentes: si la base dejo pasar dos VIGENTE del
     * mismo tipo, gana la version mas alta —la misma regla que {@link
     * #vigentePorTipo}—. Devolver los dos obligaria a quien consulta a elegir, y esa
     * eleccion no es suya.
     */
    public List<ContratoPublicado> vigentes(DSLContext dsl) {
        var t = DSL.table(DSL.name("cumplimiento", "contrato_adhesion"));
        var tipo = DSL.field("tipo", String.class);
        var version = DSL.field("version", Short.class);
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("codigo", String.class),
                        version,
                        tipo,
                        DSL.field("url_documento", String.class),
                        DSL.field("hash_documento", String.class),
                        DSL.field("vigente_desde", OffsetDateTime.class),
                        DSL.field("numero_registro", String.class),
                        DSL.field("fecha_registro", LocalDate.class))
                .from(t)
                .where(DSL.field("estado").eq("VIGENTE"))
                // DISTINCT ON (tipo) con el orden de abajo: uno por tipo, el de version
                // mas alta. Se resuelve en la base y no en Java para no traer versiones
                // que despues se descartan.
                .and(version.eq(DSL.select(DSL.max(DSL.field("version", Short.class)))
                        .from(t.as("mas_nuevo"))
                        .where(DSL.field(DSL.name("mas_nuevo", "tipo"), String.class)
                                .eq(tipo))
                        .and(DSL.field(DSL.name("mas_nuevo", "estado"), String.class)
                                .eq("VIGENTE"))))
                .orderBy(tipo)
                .fetch(f -> new ContratoPublicado(
                        f.get("id", UUID.class),
                        f.get("codigo", String.class),
                        f.get("version", Short.class),
                        f.get("tipo", String.class),
                        f.get("url_documento", String.class),
                        f.get("hash_documento", String.class),
                        f.get("vigente_desde", OffsetDateTime.class),
                        Optional.ofNullable(f.get("numero_registro", String.class)),
                        Optional.ofNullable(f.get("fecha_registro", LocalDate.class))));
    }

    public Optional<Contrato> porId(DSLContext dsl, UUID contratoId) {
        Record fila = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("version", Short.class),
                        DSL.field("estado", String.class),
                        DSL.field("hash_documento", String.class))
                .from(DSL.table(DSL.name("cumplimiento", "contrato_adhesion")))
                .where(DSL.field("id", UUID.class).eq(contratoId))
                .fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Contrato(
                        f.get("id", UUID.class),
                        f.get("version", Short.class),
                        f.get("estado", String.class),
                        f.get("hash_documento", String.class)));
    }

    /**
     * R-CON-07: no se acepta un contrato si no hay tarifario publicado que mostrar.
     *
     * <p>Se exige {@code publicado_en} y no solo {@code estado='VIGENTE'} porque la
     * pregunta del caso de uso no es «existe un tarifario» sino «la persona pudo
     * verlo antes de aceptar». Un tarifario vigente sin publicar no lo pudo ver.
     */
    public boolean hayTarifarioPublicado(DSLContext dsl, LocalDate hoy) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("catalogo", "tarifario")),
                        DSL.field("estado").eq("VIGENTE"),
                        DSL.field("publicado_en").isNotNull(),
                        DSL.field("vigente_desde", LocalDate.class).le(hoy),
                        DSL.field("vigente_hasta")
                                .isNull()
                                .or(DSL.field("vigente_hasta", LocalDate.class).ge(hoy)))
                > 0;
    }

    public record Contrato(UUID id, short version, String estado, String hashDocumento) {

        public VersionAceptable comoVersion() {
            return new VersionAceptable(version, estado);
        }
    }
}
