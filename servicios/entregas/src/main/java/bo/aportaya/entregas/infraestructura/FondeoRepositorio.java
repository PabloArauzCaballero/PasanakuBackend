package bo.aportaya.entregas.infraestructura;

import bo.aportaya.entregas.dominio.FondeoDelPozo;
import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code fondeo_entrega}: como se completo el pozo de un turno, y la deuda que queda si no se pudo.
 *
 * <p>Un fondeo por turno ({@code uq_fondeo_entrega_turno_id}). Las consultas por turno se
 * serializan con un candado de asesoria: dos pedidos simultaneos del mismo turno no se pisan
 * ni chocan contra el indice unico de la entrega.
 */
@Component
public class FondeoRepositorio {

    public record Fondeo(
            UUID id,
            UUID entregaId,
            UUID turnoId,
            String estado,
            FondeoDelPozo.Resultado cifras,
            UUID coberturaId,
            int version) {}

    public record EntregaDelTurno(UUID id, String estado, int version) {}

    /** El candado vive lo que la transaccion: no hay nada que liberar a mano. */
    public void serializarTurno(DSLContext dsl, UUID turnoId) {
        dsl.execute("select pg_advisory_xact_lock(hashtextextended(?, 0))", "fondeo-del-turno:" + turnoId);
    }

    public Optional<Fondeo> delTurno(DSLContext dsl, UUID turnoId) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("entrega_id", UUID.class),
                        DSL.field("turno_id", UUID.class),
                        DSL.field("estado", String.class),
                        DSL.field("moneda", String.class),
                        DSL.field("monto_pozo", BigDecimal.class),
                        DSL.field("monto_confirmado", BigDecimal.class),
                        DSL.field("monto_cubierto_mutual", BigDecimal.class),
                        DSL.field("monto_faltante", BigDecimal.class),
                        DSL.field("monto_cubierto_empresa", BigDecimal.class),
                        DSL.field("monto_pendiente", BigDecimal.class),
                        DSL.field("cobertura_respaldo_id", UUID.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("entregas", "fondeo_entrega")))
                .where(DSL.field("turno_id", UUID.class).eq(turnoId))
                .forUpdate()
                .fetchOptional(f -> {
                    Moneda m = Moneda.valueOf(f.get("moneda", String.class));
                    return new Fondeo(
                            f.get("id", UUID.class),
                            f.get("entrega_id", UUID.class),
                            f.get("turno_id", UUID.class),
                            f.get("estado", String.class),
                            new FondeoDelPozo.Resultado(
                                    Dinero.de(f.get("monto_pozo", BigDecimal.class), m),
                                    Dinero.de(f.get("monto_confirmado", BigDecimal.class), m),
                                    Dinero.de(f.get("monto_cubierto_mutual", BigDecimal.class), m),
                                    Dinero.de(f.get("monto_faltante", BigDecimal.class), m),
                                    Dinero.de(f.get("monto_cubierto_empresa", BigDecimal.class), m),
                                    Dinero.de(f.get("monto_pendiente", BigDecimal.class), m)),
                            f.get("cobertura_respaldo_id", UUID.class),
                            f.get("version", Integer.class));
                });
    }

    public Optional<EntregaDelTurno> entregaDelTurno(DSLContext dsl, UUID turnoId) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("estado", String.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("entregas", "entrega_fondo")))
                .where(DSL.field("turno_id", UUID.class).eq(turnoId))
                .forUpdate()
                .fetchOptional(f -> new EntregaDelTurno(
                        f.get("id", UUID.class), f.get("estado", String.class), f.get("version", Integer.class)));
    }

    public UUID crear(
            DSLContext dsl,
            UUID entregaId,
            UUID turnoId,
            FondeoDelPozo.Resultado c,
            UUID coberturaId,
            String clave,
            OffsetDateTime corte,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("entregas", "fondeo_entrega")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("entrega_id", UUID.class), entregaId)
                .set(DSL.field("turno_id", UUID.class), turnoId)
                .set(DSL.field("moneda", String.class), c.pozo().moneda().name())
                .set(DSL.field("monto_pozo", BigDecimal.class), c.pozo().monto())
                .set(
                        DSL.field("monto_confirmado", BigDecimal.class),
                        c.confirmado().monto())
                .set(
                        DSL.field("monto_cubierto_mutual", BigDecimal.class),
                        c.cubiertoMutual().monto())
                .set(DSL.field("monto_faltante", BigDecimal.class), c.faltante().monto())
                .set(
                        DSL.field("monto_cubierto_empresa", BigDecimal.class),
                        c.cubiertoEmpresa().monto())
                .set(
                        DSL.field("monto_pendiente", BigDecimal.class),
                        c.pendiente().monto())
                .set(DSL.field("cobertura_respaldo_id", UUID.class), coberturaId)
                .set(DSL.field("estado", String.class), c.completo() ? "FONDEADO" : "CON_PENDIENTE")
                .set(DSL.field("corte_en", OffsetDateTime.class), corte)
                .set(DSL.field("fondeada_en", OffsetDateTime.class), c.completo() ? ahora : null)
                .set(DSL.field("clave_idempotencia", String.class), clave)
                .set(DSL.field("version", Integer.class), 0)
                .execute();
        return id;
    }

    /** Reintento de un fondeo con pendiente: la version de lectura es la precondicion de la escritura. */
    public boolean completar(
            DSLContext dsl,
            Fondeo previo,
            FondeoDelPozo.Resultado c,
            UUID coberturaId,
            OffsetDateTime corte,
            OffsetDateTime ahora) {
        return dsl.update(DSL.table(DSL.name("entregas", "fondeo_entrega")))
                        .set(
                                DSL.field("monto_confirmado", BigDecimal.class),
                                c.confirmado().monto())
                        .set(
                                DSL.field("monto_cubierto_mutual", BigDecimal.class),
                                c.cubiertoMutual().monto())
                        .set(
                                DSL.field("monto_faltante", BigDecimal.class),
                                c.faltante().monto())
                        .set(
                                DSL.field("monto_cubierto_empresa", BigDecimal.class),
                                c.cubiertoEmpresa().monto())
                        .set(
                                DSL.field("monto_pendiente", BigDecimal.class),
                                c.pendiente().monto())
                        .set(DSL.field("cobertura_respaldo_id", UUID.class), coberturaId)
                        .set(DSL.field("estado", String.class), c.completo() ? "FONDEADO" : "CON_PENDIENTE")
                        .set(DSL.field("corte_en", OffsetDateTime.class), corte)
                        .set(DSL.field("fondeada_en", OffsetDateTime.class), c.completo() ? ahora : null)
                        .set(DSL.field("version", Integer.class), previo.version() + 1)
                        .where(DSL.field("id", UUID.class).eq(previo.id()))
                        .and(DSL.field("version", Integer.class).eq(previo.version()))
                        .and(DSL.field("estado", String.class).eq("CON_PENDIENTE"))
                        .execute()
                == 1;
    }

    public Optional<UUID> incidenciaAbierta(DSLContext dsl, UUID entregaId) {
        return dsl.select(DSL.field("id", UUID.class))
                .from(DSL.table(DSL.name("entregas", "incidencia_entrega")))
                .where(DSL.field("entrega_id", UUID.class).eq(entregaId))
                .and(DSL.field("tipo", String.class).eq("FONDO_INCOMPLETO"))
                .and(DSL.field("estado", String.class).in("ABIERTA", "EN_GESTION", "ESCALADA"))
                .fetchOptional(f -> f.get("id", UUID.class));
    }

    public UUID abrirIncidencia(
            DSLContext dsl, UUID entregaId, UUID reportadaPor, String descripcion, int slaHoras, OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("entregas", "incidencia_entrega")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("entrega_id", UUID.class), entregaId)
                .set(DSL.field("tipo", String.class), "FONDO_INCOMPLETO")
                .set(DSL.field("severidad", String.class), "CRITICA")
                .set(DSL.field("descripcion", String.class), descripcion)
                .set(DSL.field("reportada_por", UUID.class), reportadaPor)
                .set(DSL.field("estado", String.class), "ABIERTA")
                .set(DSL.field("sla_horas", Short.class), (short) slaHoras)
                .set(DSL.field("fecha_limite_sla", OffsetDateTime.class), ahora.plusHours(slaHoras))
                .set(DSL.field("evidencias", JSONB.class), JSONB.valueOf("[]"))
                .set(DSL.field("abierta_en", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }

    public void resolverIncidencia(DSLContext dsl, UUID incidenciaId, String resolucion, OffsetDateTime ahora) {
        dsl.update(DSL.table(DSL.name("entregas", "incidencia_entrega")))
                .set(DSL.field("estado", String.class), "RESUELTA")
                .set(DSL.field("resolucion", String.class), resolucion)
                .set(DSL.field("resuelta_en", OffsetDateTime.class), ahora)
                .where(DSL.field("id", UUID.class).eq(incidenciaId))
                .execute();
    }
}
