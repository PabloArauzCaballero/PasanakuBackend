package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.nucleofinanciero.dominio.DiscrepanciaDelProveedor.Tipo;
import bo.aportaya.plataforma.dominio.Dinero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code discrepancia_proveedor}: lo que el proveedor dijo y no se pudo creer.
 *
 * <p>Append-only: una discrepancia no se corrige ni se borra. La misma evidencia
 * ({@code referencia}, {@code tipo}, {@code huella}) se registra una sola vez, asi que un
 * reintento hostil repetido no inunda la bandeja de quien la revisa.
 */
@Component
public class DiscrepanciaRepositorio {

    /** @return {@code true} si era nueva; {@code false} si esa evidencia ya estaba registrada. */
    public boolean registrar(
            DSLContext dsl,
            String referenciaTipo,
            UUID referenciaId,
            Tipo tipo,
            Optional<Dinero> esperado,
            Optional<Dinero> informado,
            String detalle,
            String huella,
            UUID correlacionId,
            OffsetDateTime ahora) {
        var moneda = esperado.or(() -> informado).map(d -> d.moneda().name()).orElse(null);
        return dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "discrepancia_proveedor")))
                        .set(DSL.field("id", UUID.class), UUID.randomUUID())
                        .set(DSL.field("referencia_tipo", String.class), referenciaTipo)
                        .set(DSL.field("referencia_id", UUID.class), referenciaId)
                        .set(DSL.field("tipo", String.class), tipo.name())
                        .set(
                                DSL.field("monto_esperado", BigDecimal.class),
                                esperado.map(Dinero::monto).orElse(null))
                        .set(
                                DSL.field("monto_informado", BigDecimal.class),
                                informado.map(Dinero::monto).orElse(null))
                        .set(DSL.field("moneda", String.class), moneda)
                        .set(DSL.field("detalle", String.class), detalle)
                        .set(DSL.field("huella", String.class), huella)
                        .set(DSL.field("correlacion_id", UUID.class), correlacionId)
                        .set(DSL.field("detectada_en", OffsetDateTime.class), ahora)
                        .onConflictDoNothing()
                        .execute()
                > 0;
    }
}
