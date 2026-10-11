package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.OfertaVisible;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code oferta_turno}: la publicacion del derecho a cobrar un turno en el mercado.
 *
 * <p>Toda transicion lleva su precondicion EN la escritura ({@code WHERE estado = ... AND version
 * = ...}): entre un {@code SELECT} y un {@code UPDATE} entra el otro comprador, o la cancelacion,
 * o el vencimiento. Quien pierde la carrera ve cero filas actualizadas, no un error de la base.
 * La cesion que nace de comprar una oferta vive en {@link CesionRepositorio}.
 */
@Component
public class OfertaRepositorio {

    public record Oferta(
            UUID id,
            UUID grupoId,
            UUID turnoId,
            UUID cupoId,
            UUID participanteOrigenId,
            UUID vendedorUsuarioId,
            Dinero derecho,
            Dinero precio,
            Dinero cargos,
            String estado,
            OffsetDateTime vigenteHasta,
            int version) {}

    private static org.jooq.Table<?> ofertas() {
        return DSL.table(DSL.name("entregas", "oferta_turno"));
    }

    // ------------------------------------------------------------------ ofertas

    public UUID crearOferta(DSLContext dsl, Oferta o, String clave, OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(ofertas())
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("grupo_id", UUID.class), o.grupoId())
                .set(DSL.field("turno_id", UUID.class), o.turnoId())
                .set(DSL.field("cupo_id", UUID.class), o.cupoId())
                .set(DSL.field("participante_origen_id", UUID.class), o.participanteOrigenId())
                .set(DSL.field("vendedor_usuario_id", UUID.class), o.vendedorUsuarioId())
                .set(DSL.field("moneda", String.class), o.precio().moneda().name())
                .set(DSL.field("monto_derecho", BigDecimal.class), o.derecho().monto())
                .set(DSL.field("monto_precio", BigDecimal.class), o.precio().monto())
                .set(DSL.field("monto_cargos", BigDecimal.class), o.cargos().monto())
                .set(DSL.field("estado", String.class), "PUBLICADA")
                .set(DSL.field("vigente_hasta", OffsetDateTime.class), o.vigenteHasta())
                .set(DSL.field("publicada_en", OffsetDateTime.class), ahora)
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("version", Integer.class), 0)
                .execute();
        return id;
    }

    public Optional<Oferta> ofertaPorClave(DSLContext dsl, String clave) {
        return leerOferta(dsl, DSL.field("clave_idempotencia", String.class).eq(clave), false);
    }

    public Optional<Oferta> ver(DSLContext dsl, UUID id) {
        return leerOferta(dsl, DSL.field("id", UUID.class).eq(id), false);
    }

    public Optional<Oferta> bloquear(DSLContext dsl, UUID id) {
        return leerOferta(dsl, DSL.field("id", UUID.class).eq(id), true);
    }

    public Optional<Oferta> activaDelTurno(DSLContext dsl, UUID turnoId) {
        return leerOferta(
                dsl,
                DSL.field("turno_id", UUID.class)
                        .eq(turnoId)
                        .and(DSL.field("estado", String.class).in("PUBLICADA", "RESERVADA", "LIQUIDANDO")),
                false);
    }

    private Optional<Oferta> leerOferta(DSLContext dsl, Condition donde, boolean bloqueando) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("turno_id", UUID.class),
                        DSL.field("cupo_id", UUID.class),
                        DSL.field("participante_origen_id", UUID.class),
                        DSL.field("vendedor_usuario_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_derecho", BigDecimal.class),
                        DSL.field("monto_precio", BigDecimal.class),
                        DSL.field("monto_cargos", BigDecimal.class),
                        DSL.field("estado", String.class),
                        DSL.field("vigente_hasta", OffsetDateTime.class),
                        DSL.field("version", Integer.class))
                .from(ofertas())
                .where(donde)
                .limit(1);
        Record fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila).map(f -> {
            Moneda m = Moneda.valueOf(f.get("moneda", String.class));
            return new Oferta(
                    f.get("id", UUID.class),
                    f.get("grupo_id", UUID.class),
                    f.get("turno_id", UUID.class),
                    f.get("cupo_id", UUID.class),
                    f.get("participante_origen_id", UUID.class),
                    f.get("vendedor_usuario_id", UUID.class),
                    Dinero.de(f.get("monto_derecho", BigDecimal.class), m),
                    Dinero.de(f.get("monto_precio", BigDecimal.class), m),
                    Dinero.de(f.get("monto_cargos", BigDecimal.class), m),
                    f.get("estado", String.class),
                    f.get("vigente_hasta", OffsetDateTime.class),
                    f.get("version", Integer.class));
        });
    }

    /** La oferta pasa de un estado a otro solo si sigue donde la leimos. Cero filas = perdiste la carrera. */
    public boolean mover(DSLContext dsl, Oferta o, List<String> desde, String hacia) {
        return dsl.update(ofertas())
                        .set(DSL.field("estado", String.class), hacia)
                        .set(DSL.field("version", Integer.class), o.version() + 1)
                        .where(DSL.field("id", UUID.class).eq(o.id()))
                        .and(DSL.field("estado", String.class).in(desde))
                        .and(DSL.field("version", Integer.class).eq(o.version()))
                        .execute()
                == 1;
    }

    /** Reservar es mover PUBLICADA a RESERVADA solo si sigue vigente: vencimiento y compra no se cruzan. */
    public boolean reservar(DSLContext dsl, Oferta o, OffsetDateTime ahora) {
        return dsl.update(ofertas())
                        .set(DSL.field("estado", String.class), "RESERVADA")
                        .set(DSL.field("version", Integer.class), o.version() + 1)
                        .where(DSL.field("id", UUID.class).eq(o.id()))
                        .and(DSL.field("estado", String.class).eq("PUBLICADA"))
                        .and(DSL.field("version", Integer.class).eq(o.version()))
                        .and(DSL.field("vigente_hasta", OffsetDateTime.class).gt(ahora))
                        .execute()
                == 1;
    }

    /** El vencimiento solo toca las PUBLICADAS: una compra en curso no se vence. */
    public int vencer(DSLContext dsl, OffsetDateTime ahora) {
        return dsl.update(ofertas())
                .set(DSL.field("estado", String.class), "VENCIDA")
                .set(
                        DSL.field("version", Integer.class),
                        DSL.field("version", Integer.class).plus(1))
                .where(DSL.field("estado", String.class).eq("PUBLICADA"))
                .and(DSL.field("vigente_hasta", OffsetDateTime.class).le(ahora))
                .execute();
    }

    /** Sin identidad del vendedor, solo de grupos donde quien mira es miembro, con orden determinista. */
    public List<OfertaVisible> listar(
            DSLContext dsl,
            Set<UUID> grupos,
            UUID excluirVendedor,
            OffsetDateTime ahora,
            int limite,
            int desplazamiento) {
        if (grupos.isEmpty()) {
            return List.of();
        }
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("grupo_id", UUID.class),
                        DSL.field("turno_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_derecho", BigDecimal.class),
                        DSL.field("monto_precio", BigDecimal.class),
                        DSL.field("monto_cargos", BigDecimal.class),
                        DSL.field("vigente_hasta", OffsetDateTime.class))
                .from(ofertas())
                .where(DSL.field("estado", String.class).eq("PUBLICADA"))
                .and(DSL.field("vigente_hasta", OffsetDateTime.class).gt(ahora))
                .and(DSL.field("grupo_id", UUID.class).in(grupos))
                .and(DSL.field("vendedor_usuario_id", UUID.class).ne(excluirVendedor))
                .orderBy(DSL.field("vigente_hasta"), DSL.field("id"))
                .limit(limite)
                .offset(desplazamiento)
                .fetch(f -> {
                    Moneda m = Moneda.valueOf(f.get("moneda", String.class));
                    return new OfertaVisible(
                            f.get("id", UUID.class),
                            f.get("grupo_id", UUID.class),
                            f.get("turno_id", UUID.class),
                            Dinero.de(f.get("monto_derecho", BigDecimal.class), m),
                            Dinero.de(f.get("monto_precio", BigDecimal.class), m),
                            Dinero.de(f.get("monto_cargos", BigDecimal.class), m),
                            f.get("vigente_hasta", OffsetDateTime.class));
                });
    }
}
