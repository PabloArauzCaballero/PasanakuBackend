package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * La cola de la segunda aprobacion (ADR-049) sobre {@code orden_retiro}: solo lectura.
 *
 * <p>Cursor por {@code (solicitada_en, id)}: el desempate por id hace estable el orden aunque dos
 * ordenes compartan el instante, y una orden resuelta entre dos paginas no corre a las demas (con
 * OFFSET si). Mismo patron que la bandeja de habilitaciones de {@code organizador}; la consulta
 * usa {@code ix_orden_retiro_estado}.
 */
@Component
public class BandejaDeRetirosRepositorio {

    public List<EnRevision> enRevision(
            DSLContext dsl, Optional<OffsetDateTime> despuesDeFecha, Optional<UUID> despuesDeId, int limite) {
        var solicitadaEn = DSL.field("solicitada_en", OffsetDateTime.class);
        var id = DSL.field("id", UUID.class);
        var condicion = DSL.field("estado", String.class).eq("EN_REVISION");
        if (despuesDeFecha.isPresent() && despuesDeId.isPresent()) {
            condicion = condicion.and(DSL.row(solicitadaEn, id).gt(despuesDeFecha.get(), despuesDeId.get()));
        }
        return dsl.select(
                        id,
                        DSL.field("cuenta_billetera_id", UUID.class),
                        DSL.field("solicitada_por", UUID.class),
                        DSL.field("monto_solicitado", BigDecimal.class),
                        DSL.field("costo_retiro", BigDecimal.class),
                        DSL.field("monto_neto", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        DSL.field("mfa_verificado", Boolean.class),
                        solicitadaEn,
                        DSL.field("ventana_enfriamiento_hasta", OffsetDateTime.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                .where(condicion)
                .orderBy(solicitadaEn, id)
                .limit(limite)
                .fetch(f -> {
                    Moneda moneda = Moneda.valueOf(f.get("moneda", String.class));
                    return new EnRevision(
                            f.get(id),
                            f.get("cuenta_billetera_id", UUID.class),
                            f.get("solicitada_por", UUID.class),
                            Dinero.de(f.get("monto_solicitado", BigDecimal.class), moneda),
                            Dinero.de(f.get("costo_retiro", BigDecimal.class), moneda),
                            Dinero.de(f.get("monto_neto", BigDecimal.class), moneda),
                            f.get("mfa_verificado", Boolean.class),
                            f.get(solicitadaEn),
                            Optional.ofNullable(f.get("ventana_enfriamiento_hasta", OffsetDateTime.class)));
                });
    }

    /** Una fila de la bandeja: lo necesario para decidir, sin destino ni datos de la persona. */
    public record EnRevision(
            UUID id,
            UUID cuentaId,
            UUID solicitadaPor,
            Dinero solicitado,
            Dinero costo,
            Dinero neto,
            boolean mfaVerificado,
            OffsetDateTime solicitadaEn,
            Optional<OffsetDateTime> enfriamientoHasta) {}
}
