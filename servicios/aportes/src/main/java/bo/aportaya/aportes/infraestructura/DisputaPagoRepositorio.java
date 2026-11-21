package bo.aportaya.aportes.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** {@code disputa_pago}: separada de {@link PagoRepositorio} cuando el pago paso a llevar su historial. */
@Component
public class DisputaPagoRepositorio {

    /** Una disputa abierta por pago: la segunda del proveedor es un reenvio. */
    public boolean hayDisputaAbierta(DSLContext dsl, UUID pagoId) {
        return dsl.fetchCount(
                        DSL.table(DSL.name("aportes", "disputa_pago")),
                        DSL.field("pago_id", UUID.class).eq(pagoId),
                        DSL.field("estado").ne("RESUELTA"))
                > 0;
    }

    /**
     * Abre la disputa con su plazo **guardado**, no calculado al consultar.
     *
     * <p>Invariante 8: el plazo que corre es el que se fijo al recibirla. Recalcularlo
     * al mirar el tablero lo moveria cada vez que cambie la politica, y el proveedor
     * no acepta ese argumento.
     */
    public UUID abrirDisputa(
            DSLContext dsl,
            UUID pagoId,
            String tipo,
            String descripcion,
            Dinero montoDisputado,
            String evidenciasJson,
            OffsetDateTime abiertaEn,
            OffsetDateTime fechaLimite) {

        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("aportes", "disputa_pago")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("pago_id", UUID.class), pagoId)
                .set(DSL.field("tipo", String.class), tipo)
                .set(DSL.field("descripcion", String.class), descripcion)
                .set(DSL.field("monto_disputado", BigDecimal.class), montoDisputado.monto())
                .set(DSL.field("moneda", String.class), montoDisputado.moneda().name())
                .set(DSL.field("estado", String.class), "ABIERTA")
                .set(DSL.field("evidencias", org.jooq.JSONB.class), org.jooq.JSONB.valueOf(evidenciasJson))
                .set(DSL.field("abierta_en", OffsetDateTime.class), abiertaEn)
                .set(DSL.field("fecha_limite_respuesta", OffsetDateTime.class), fechaLimite)
                .execute();
        return id;
    }
}
