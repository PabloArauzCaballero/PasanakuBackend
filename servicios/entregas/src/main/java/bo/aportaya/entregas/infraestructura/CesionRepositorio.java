package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.infraestructura.OfertaRepositorio.Oferta;
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

/**
 * {@code cesion_derecho}: el traspaso del derecho a cobrar un turno, del vendedor al comprador.
 *
 * <p>Igual que las ofertas ({@link OfertaRepositorio}), cada paso lleva su precondicion de estado
 * y version EN la escritura: quien pierde la carrera ve cero filas actualizadas, no un error.
 */
@Component
public class CesionRepositorio {

    public record Cesion(
            UUID id,
            UUID ofertaId,
            UUID turnoId,
            UUID origenId,
            UUID destinoId,
            UUID compradorUsuarioId,
            Dinero precio,
            String estado,
            UUID retencionRef,
            UUID liquidacionRef,
            OffsetDateTime creadaEn,
            int version) {}

    private static org.jooq.Table<?> cesiones() {
        return DSL.table(DSL.name("entregas", "cesion_derecho"));
    }

    // ------------------------------------------------------------------ cesiones

    public UUID crearCesion(
            DSLContext dsl,
            Oferta o,
            UUID destinoParticipante,
            UUID compradorUsuario,
            String clave,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(cesiones())
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("oferta_turno_id", UUID.class), o.id())
                .set(DSL.field("turno_id", UUID.class), o.turnoId())
                .set(DSL.field("participante_origen_id", UUID.class), o.participanteOrigenId())
                .set(DSL.field("participante_destino_id", UUID.class), destinoParticipante)
                .set(DSL.field("comprador_usuario_id", UUID.class), compradorUsuario)
                .set(DSL.field("moneda", String.class), o.precio().moneda().name())
                .set(DSL.field("monto_precio", BigDecimal.class), o.precio().monto())
                .set(DSL.field("estado", String.class), "VALIDADA")
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("creada_en", OffsetDateTime.class), ahora)
                .set(DSL.field("version", Integer.class), 0)
                .execute();
        return id;
    }

    public Optional<Cesion> cesionPorClave(DSLContext dsl, String clave) {
        return leerCesion(dsl, DSL.field("clave_idempotencia", String.class).eq(clave), false);
    }

    public Optional<Cesion> cesion(DSLContext dsl, UUID id, boolean bloqueando) {
        return leerCesion(dsl, DSL.field("id", UUID.class).eq(id), bloqueando);
    }

    public Optional<Cesion> vivaDelTurno(DSLContext dsl, UUID turnoId) {
        return leerCesion(
                dsl,
                DSL.field("turno_id", UUID.class)
                        .eq(turnoId)
                        .and(DSL.field("estado", String.class).ne("FALLIDA")),
                false);
    }

    /** Quien cobra el pozo de este turno, si el derecho ya se cedio: el titulo asignado o liquidado. */
    public Optional<UUID> destinoDelTitulo(DSLContext dsl, UUID turnoId) {
        return dsl.select(DSL.field("participante_destino_id", UUID.class))
                .from(cesiones())
                .where(DSL.field("turno_id", UUID.class).eq(turnoId))
                .and(DSL.field("estado", String.class).in("TITULO_ASIGNADO", "LIQUIDADA"))
                .fetchOptional(f -> f.get("participante_destino_id", UUID.class));
    }

    public List<UUID> colgadasAntesDe(DSLContext dsl, OffsetDateTime limite) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(cesiones())
                .where(DSL.field("estado", String.class).in("VALIDADA", "FONDOS_RETENIDOS"))
                .and(DSL.field("creada_en", OffsetDateTime.class).lt(limite))
                .orderBy(DSL.field("creada_en"), DSL.field("id"))
                .fetch(f -> f.get("id", UUID.class));
    }

    private Optional<Cesion> leerCesion(DSLContext dsl, Condition donde, boolean bloqueando) {
        var consulta = dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("oferta_turno_id", UUID.class),
                        DSL.field("turno_id", UUID.class),
                        DSL.field("participante_origen_id", UUID.class),
                        DSL.field("participante_destino_id", UUID.class),
                        DSL.field("comprador_usuario_id", UUID.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_precio", BigDecimal.class),
                        DSL.field("estado", String.class),
                        DSL.field("retencion_ref", UUID.class),
                        DSL.field("liquidacion_ref", UUID.class),
                        DSL.field("creada_en", OffsetDateTime.class),
                        DSL.field("version", Integer.class))
                .from(cesiones())
                .where(donde)
                .limit(1);
        Record fila = bloqueando ? consulta.forUpdate().fetchOne() : consulta.fetchOne();
        return Optional.ofNullable(fila)
                .map(f -> new Cesion(
                        f.get("id", UUID.class),
                        f.get("oferta_turno_id", UUID.class),
                        f.get("turno_id", UUID.class),
                        f.get("participante_origen_id", UUID.class),
                        f.get("participante_destino_id", UUID.class),
                        f.get("comprador_usuario_id", UUID.class),
                        Dinero.de(
                                f.get("monto_precio", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class))),
                        f.get("estado", String.class),
                        f.get("retencion_ref", UUID.class),
                        f.get("liquidacion_ref", UUID.class),
                        f.get("creada_en", OffsetDateTime.class),
                        f.get("version", Integer.class)));
    }

    /** Avanza la cesion un paso, con la precondicion del estado y la version leidos. */
    public boolean avanzar(
            DSLContext dsl,
            Cesion c,
            String desde,
            String hacia,
            UUID retencionRef,
            UUID liquidacionRef,
            String motivoFallo,
            OffsetDateTime ahora) {
        var q = dsl.update(cesiones())
                .set(DSL.field("estado", String.class), hacia)
                .set(DSL.field("version", Integer.class), c.version() + 1);
        if (retencionRef != null) {
            q = q.set(DSL.field("retencion_ref", UUID.class), retencionRef);
        }
        if (liquidacionRef != null) {
            q = q.set(DSL.field("liquidacion_ref", UUID.class), liquidacionRef)
                    .set(DSL.field("liquidada_en", OffsetDateTime.class), ahora);
        }
        if ("TITULO_ASIGNADO".equals(hacia)) {
            q = q.set(DSL.field("titulo_asignado_en", OffsetDateTime.class), ahora);
        }
        if (motivoFallo != null) {
            q = q.set(DSL.field("motivo_fallo", String.class), motivoFallo);
        }
        return q.where(DSL.field("id", UUID.class).eq(c.id()))
                        .and(DSL.field("estado", String.class).eq(desde))
                        .and(DSL.field("version", Integer.class).eq(c.version()))
                        .execute()
                == 1;
    }

    public boolean hayEntrega(DSLContext dsl, UUID turnoId) {
        return dsl.fetchExists(
                DSL.table(DSL.name("entregas", "entrega_fondo")),
                DSL.field("turno_id", UUID.class).eq(turnoId));
    }
}
