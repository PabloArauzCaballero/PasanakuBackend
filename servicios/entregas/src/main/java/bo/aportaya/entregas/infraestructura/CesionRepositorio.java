package bo.aportaya.entregas.infraestructura;

import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code cesion_derecho}, SOLO LECTURA: a quien le corresponde el pozo de un turno cuyo derecho de cobro se cedio
 * antes de retirar la cesion de turnos (D-L2-1, DECISIONES tanda 7).
 *
 * <p>La subasta y el mercado de turnos ya no crean cesiones. Las que quedaron con titulo asignado o liquidadas siguen
 * mandando sobre el beneficiario original: el comprador ya pago el precio y pagarle el pozo al vendedor seria pagar
 * dos veces el mismo turno. La tabla se retira (contraccion, regla 97.3.5) cuando no quede ninguna viva.
 */
@Component
public class CesionRepositorio {

    public Optional<UUID> destinoDelTitulo(DSLContext dsl, UUID turnoId) {
        return dsl.select(DSL.field("participante_destino_id", UUID.class))
                .from(DSL.table(DSL.name("entregas", "cesion_derecho")))
                .where(DSL.field("turno_id", UUID.class).eq(turnoId))
                .and(DSL.field("estado", String.class).in("TITULO_ASIGNADO", "LIQUIDADA"))
                .fetchOptional(f -> f.get("participante_destino_id", UUID.class));
    }
}
