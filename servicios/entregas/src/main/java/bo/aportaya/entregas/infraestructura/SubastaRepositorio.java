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

/** La subasta del cupo y sus pujas ({@code entregas.subasta_cupo}, {@code entregas.puja_subasta}). */
@Component
public class SubastaRepositorio {

    /** Los estados en que la subasta todavia ocupa el cupo: no se abre otra. */
    public static final List<String> VIVAS = List.of("ABIERTA", "ADJUDICADA", "LIQUIDANDO");

    public record Subasta(
            UUID id,
            UUID grupoId,
            UUID cupoId,
            UUID participanteOrigenId,
            UUID vendedorUsuarioId,
            String nombreDelGrupo,
            String periodicidad,
            Dinero aporte,
            Dinero base,
            Optional<Dinero> ganador,
            String estado,
            OffsetDateTime cierraEn,
            Optional<UUID> pujaGanadoraId,
            Optional<UUID> traspasoRef,
            int version) {}

    private static org.jooq.Table<?> subastas() {
        return DSL.table(DSL.name("entregas", "subasta_cupo"));
    }

    private static <T> org.jooq.Field<T> f(String nombre, Class<T> tipo) {
        return DSL.field(DSL.name(nombre), tipo);
    }

    /** Dos aperturas del mismo cupo se turnan: la segunda ve la primera y se rechaza. */
    public void serializarCupo(DSLContext dsl, UUID cupoId) {
        dsl.execute("select pg_advisory_xact_lock(hashtextextended(?, 0))", "subasta-del-cupo:" + cupoId);
    }

    // ------------------------------------------------------------------ subastas

    public UUID crear(DSLContext dsl, Subasta s, String clave, OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(subastas())
                .set(f("id", UUID.class), id)
                .set(f("grupo_id", UUID.class), s.grupoId())
                .set(f("cupo_id", UUID.class), s.cupoId())
                .set(f("participante_origen_id", UUID.class), s.participanteOrigenId())
                .set(f("vendedor_usuario_id", UUID.class), s.vendedorUsuarioId())
                .set(f("nombre_grupo", String.class), s.nombreDelGrupo())
                .set(f("periodicidad", String.class), s.periodicidad())
                .set(f("moneda", String.class), s.base().moneda().name())
                .set(f("monto_aporte", BigDecimal.class), s.aporte().monto())
                .set(f("monto_base", BigDecimal.class), s.base().monto())
                .set(f("estado", String.class), "ABIERTA")
                .set(f("abierta_en", OffsetDateTime.class), ahora)
                .set(f("cierra_en", OffsetDateTime.class), s.cierraEn())
                .set(f("clave_idempotencia", String.class), clave)
                .set(f("version", Integer.class), 0)
                .execute();
        return id;
    }

    public Optional<Subasta> porClave(DSLContext dsl, String clave) {
        return leer(dsl, f("clave_idempotencia", String.class).eq(clave), false);
    }

    public Optional<Subasta> ver(DSLContext dsl, UUID id) {
        return leer(dsl, f("id", UUID.class).eq(id), false);
    }

    public Optional<Subasta> bloquear(DSLContext dsl, UUID id) {
        return leer(dsl, f("id", UUID.class).eq(id), true);
    }

    public Optional<Subasta> vivaDelCupo(DSLContext dsl, UUID cupoId) {
        return leer(
                dsl,
                f("cupo_id", UUID.class)
                        .eq(cupoId)
                        .and(f("estado", String.class).in(VIVAS)),
                false);
    }

    /** La ultima subasta del cupo en cualquier estado: la pantalla del grupo muestra como termino. */
    public Optional<Subasta> ultimaDelCupo(DSLContext dsl, UUID cupoId) {
        return dsl.select(f("id", UUID.class))
                .from(subastas())
                .where(f("cupo_id", UUID.class).eq(cupoId))
                .orderBy(f("abierta_en", OffsetDateTime.class).desc(), f("id", UUID.class))
                .limit(1)
                .fetchOptional(f("id", UUID.class))
                .flatMap(id -> ver(dsl, id));
    }

    /** Las abiertas, con orden estable (cierre, id): la que cierra antes, primero. */
    public List<Subasta> abiertas(DSLContext dsl, OffsetDateTime ahora, int limite, int pagina) {
        return dsl
                .select(f("id", UUID.class))
                .from(subastas())
                .where(f("estado", String.class).eq("ABIERTA"))
                .and(f("cierra_en", OffsetDateTime.class).gt(ahora))
                .orderBy(f("cierra_en", OffsetDateTime.class), f("id", UUID.class))
                .limit(limite)
                .offset((long) limite * pagina)
                .fetch(f("id", UUID.class))
                .stream()
                .map(id -> ver(dsl, id).orElseThrow())
                .toList();
    }

    /** Las que ya tienen que cerrarse o seguir su saga: ABIERTA vencida, ADJUDICADA o LIQUIDANDO. */
    public List<UUID> porAvanzar(DSLContext dsl, OffsetDateTime ahora, int limite) {
        return dsl.select(f("id", UUID.class))
                .from(subastas())
                .where(f("estado", String.class)
                        .eq("ABIERTA")
                        .and(f("cierra_en", OffsetDateTime.class).le(ahora))
                        .or(f("estado", String.class).in("ADJUDICADA", "LIQUIDANDO")))
                .orderBy(f("cierra_en", OffsetDateTime.class), f("id", UUID.class))
                .limit(limite)
                .fetch(f("id", UUID.class));
    }

    /** Transicion condicionada al estado y a la version leida: dos cierres no avanzan la misma subasta dos veces. */
    public boolean mover(DSLContext dsl, Subasta s, String desde, String hacia) {
        return dsl.update(subastas())
                        .set(f("estado", String.class), hacia)
                        .set(f("version", Integer.class), s.version() + 1)
                        .where(f("id", UUID.class).eq(s.id()))
                        .and(f("estado", String.class).eq(desde))
                        .and(f("version", Integer.class).eq(s.version()))
                        .execute()
                == 1;
    }

    public boolean adjudicar(DSLContext dsl, Subasta s, PujaRepositorio.Puja ganadora) {
        return dsl.update(subastas())
                        .set(f("estado", String.class), "ADJUDICADA")
                        .set(f("puja_ganadora_id", UUID.class), ganadora.id())
                        .set(
                                f("monto_ganador", BigDecimal.class),
                                ganadora.monto().monto())
                        .set(f("version", Integer.class), s.version() + 1)
                        .where(f("id", UUID.class).eq(s.id()))
                        .and(f("estado", String.class).eq("ABIERTA"))
                        .and(f("version", Integer.class).eq(s.version()))
                        .execute()
                == 1;
    }

    public boolean traspasada(DSLContext dsl, Subasta s, UUID traspasoRef) {
        return dsl.update(subastas())
                        .set(f("estado", String.class), "LIQUIDANDO")
                        .set(f("traspaso_ref", UUID.class), traspasoRef)
                        .set(f("version", Integer.class), s.version() + 1)
                        .where(f("id", UUID.class).eq(s.id()))
                        .and(f("estado", String.class).eq("ADJUDICADA"))
                        .and(f("version", Integer.class).eq(s.version()))
                        .execute()
                == 1;
    }

    public boolean vendida(DSLContext dsl, Subasta s, UUID liquidacionRef) {
        return dsl.update(subastas())
                        .set(f("estado", String.class), "VENDIDA")
                        .set(f("liquidacion_ref", UUID.class), liquidacionRef)
                        .set(f("version", Integer.class), s.version() + 1)
                        .where(f("id", UUID.class).eq(s.id()))
                        .and(f("estado", String.class).eq("LIQUIDANDO"))
                        .and(f("version", Integer.class).eq(s.version()))
                        .execute()
                == 1;
    }

    public boolean fallida(DSLContext dsl, Subasta s, String motivo) {
        return dsl.update(subastas())
                        .set(f("estado", String.class), "FALLIDA")
                        .set(f("motivo_fallo", String.class), motivo)
                        .set(f("version", Integer.class), s.version() + 1)
                        .where(f("id", UUID.class).eq(s.id()))
                        .and(f("estado", String.class).eq("ADJUDICADA"))
                        .and(f("version", Integer.class).eq(s.version()))
                        .execute()
                == 1;
    }

    private Optional<Subasta> leer(DSLContext dsl, Condition donde, boolean bloqueando) {
        var consulta = dsl.select(
                        f("id", UUID.class),
                        f("grupo_id", UUID.class),
                        f("cupo_id", UUID.class),
                        f("participante_origen_id", UUID.class),
                        f("vendedor_usuario_id", UUID.class),
                        f("nombre_grupo", String.class),
                        f("periodicidad", String.class),
                        f("moneda", String.class),
                        f("monto_aporte", BigDecimal.class),
                        f("monto_base", BigDecimal.class),
                        f("monto_ganador", BigDecimal.class),
                        f("estado", String.class),
                        f("cierra_en", OffsetDateTime.class),
                        f("puja_ganadora_id", UUID.class),
                        f("traspaso_ref", UUID.class),
                        f("version", Integer.class))
                .from(subastas())
                .where(donde)
                .limit(1);
        Record fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(r -> {
            Moneda m = Moneda.valueOf(r.get("moneda", String.class));
            return new Subasta(
                    r.get("id", UUID.class),
                    r.get("grupo_id", UUID.class),
                    r.get("cupo_id", UUID.class),
                    r.get("participante_origen_id", UUID.class),
                    r.get("vendedor_usuario_id", UUID.class),
                    r.get("nombre_grupo", String.class),
                    r.get("periodicidad", String.class),
                    Dinero.de(r.get("monto_aporte", BigDecimal.class), m),
                    Dinero.de(r.get("monto_base", BigDecimal.class), m),
                    Optional.ofNullable(r.get("monto_ganador", BigDecimal.class))
                            .map(x -> Dinero.de(x, m)),
                    r.get("estado", String.class),
                    r.get("cierra_en", OffsetDateTime.class),
                    Optional.ofNullable(r.get("puja_ganadora_id", UUID.class)),
                    Optional.ofNullable(r.get("traspaso_ref", UUID.class)),
                    r.get("version", Integer.class));
        });
    }
}
