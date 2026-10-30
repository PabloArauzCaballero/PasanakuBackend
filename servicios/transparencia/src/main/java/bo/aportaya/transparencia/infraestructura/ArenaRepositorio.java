package bo.aportaya.transparencia.infraestructura;

import bo.aportaya.transparencia.dominio.ArenasDeReputacion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code arena} y {@code desbloqueo_arena}: el catalogo de arenas de cada modelo de scoring.
 *
 * <p>Las arenas cuelgan del modelo (R-REP-07): si un modelo nuevo cambia la escala, trae sus
 * propias arenas en vez de reinterpretar las del anterior. Por eso se leen siempre por
 * modelo, nunca «todas».
 *
 * <p>Es catalogo de solo lectura para el servicio: se escribe con la semilla
 * ({@code seeders/minimos/16-reputacion-y-scoring.json}), nunca desde una operacion.
 */
@Component
public class ArenaRepositorio {

    /** Las arenas del modelo, de la mas baja a la mas alta, con su desbloqueo si lo tienen. */
    public List<ArenasDeReputacion.Arena> arenasDelModelo(DSLContext dsl, UUID modeloId) {
        var arena = DSL.table(DSL.name("transparencia", "arena")).as("a");
        var desbloqueo =
                DSL.table(DSL.name("transparencia", "desbloqueo_arena")).as("d");
        return dsl.select(
                        DSL.field(DSL.name("a", "codigo"), String.class),
                        DSL.field(DSL.name("a", "nombre"), String.class),
                        DSL.field(DSL.name("a", "orden"), Short.class),
                        DSL.field(DSL.name("a", "desde"), BigDecimal.class),
                        DSL.field(DSL.name("a", "hasta"), BigDecimal.class),
                        DSL.field(DSL.name("d", "cupos_max"), Short.class),
                        DSL.field(DSL.name("d", "aporte_max"), BigDecimal.class),
                        DSL.field(DSL.name("d", "moneda"), String.class),
                        DSL.field(DSL.name("d", "estado"), String.class))
                .from(arena)
                .leftJoin(desbloqueo)
                .on(DSL.field(DSL.name("d", "arena_id"), UUID.class).eq(DSL.field(DSL.name("a", "id"), UUID.class)))
                .where(DSL.field(DSL.name("a", "modelo_id"), UUID.class).eq(modeloId))
                .orderBy(DSL.field(DSL.name("a", "orden")).asc())
                .fetch(f -> {
                    Short cuposMax = f.get(5, Short.class);
                    var desbloqueoDeLaArena = cuposMax == null
                            ? null
                            : new ArenasDeReputacion.Desbloqueo(
                                    cuposMax,
                                    f.get(6, BigDecimal.class),
                                    f.get(7, String.class),
                                    f.get(8, String.class));
                    return new ArenasDeReputacion.Arena(
                            f.get(0, String.class),
                            f.get(1, String.class),
                            f.get(2, Short.class),
                            f.get(3, BigDecimal.class),
                            f.get(4, BigDecimal.class),
                            desbloqueoDeLaArena);
                });
    }

    /** El identificador del modelo que lleva esa version; vacio si la version no existe. */
    public Optional<UUID> modeloDeLaVersion(DSLContext dsl, String version) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("transparencia", "modelo_scoring")))
                .where(DSL.field("version", String.class).eq(version))
                .fetchOptional(f -> f.get(0, UUID.class));
    }
}
