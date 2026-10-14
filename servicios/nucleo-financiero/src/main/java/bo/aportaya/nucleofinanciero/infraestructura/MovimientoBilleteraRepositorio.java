package bo.aportaya.nucleofinanciero.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Lectura ordenada del libro append-only, siempre bajo el contexto RLS del titular. */
@Component
public class MovimientoBilleteraRepositorio {

    public List<Movimiento> recientes(DSLContext dsl, UUID cuentaId, Moneda moneda, int limite) {
        var id = DSL.field("id", UUID.class);
        return dsl.select(
                        id,
                        DSL.field("sentido", String.class),
                        DSL.field("monto", BigDecimal.class),
                        DSL.field("saldo_disponible_posterior", BigDecimal.class),
                        DSL.field("saldo_retenido_posterior", BigDecimal.class),
                        DSL.field("glosa", String.class),
                        DSL.field("registrado_en", OffsetDateTime.class))
                .from(DSL.table(DSL.name("nucleo_financiero", "movimiento_billetera")))
                .where(DSL.field("cuenta_billetera_id", UUID.class).eq(cuentaId))
                .orderBy(DSL.field("registrado_en", OffsetDateTime.class).desc(), id.desc())
                .limit(limite)
                .fetch(fila -> new Movimiento(
                        fila.get(id),
                        fila.get("sentido", String.class),
                        Dinero.de(fila.get("monto", BigDecimal.class), moneda),
                        Dinero.de(fila.get("saldo_disponible_posterior", BigDecimal.class), moneda),
                        Dinero.de(fila.get("saldo_retenido_posterior", BigDecimal.class), moneda),
                        fila.get("glosa", String.class),
                        fila.get("registrado_en", OffsetDateTime.class)));
    }

    public record Movimiento(
            UUID id,
            String sentido,
            Dinero monto,
            Dinero saldoDisponiblePosterior,
            Dinero saldoRetenidoPosterior,
            String glosa,
            OffsetDateTime registradoEn) {}
}
