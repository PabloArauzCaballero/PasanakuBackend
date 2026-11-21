package bo.aportaya.garantia.infraestructura;

import bo.aportaya.plataforma.dominio.Dinero;
import bo.aportaya.plataforma.dominio.Moneda;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code deuda_participante}: lo que queda debiendo quien hizo que el fondo pagara.
 *
 * <p>Vive aparte de {@link FondoRepositorio} porque es otra cosa. El fondo es un saldo
 * colectivo que sube con aportes y baja con coberturas; la deuda es una obligacion
 * personal, con acreedor, exigibilidad y prescripcion. Mezclarlas en un repositorio
 * hacia que cada cambio de una hiciera releer la otra.
 *
 * <p>Los plazos —exigibilidad y prescripcion— se guardan al crear la deuda y no se
 * recalculan al consultarla (invariante 8): una deuda que prescribe segun cuando se la
 * mire no prescribe nunca.
 */
@Component
public class DeudaRepositorio {

    public Optional<Deuda> deudaDe(DSLContext dsl, UUID registroId) {
        return dsl.select(
                        DSL.field("id", UUID.class),
                        DSL.field("usuario_id", UUID.class),
                        DSL.field("saldo_actual", BigDecimal.class),
                        DSL.field("moneda", String.class),
                        DSL.field("estado", String.class),
                        DSL.field("es_subrogada", Boolean.class),
                        DSL.field("version", Integer.class))
                .from(DSL.table(DSL.name("garantia", "deuda_participante")))
                .where(DSL.field("registro_id", UUID.class).eq(registroId))
                .fetchOptional(f -> new Deuda(
                        f.get("id", UUID.class),
                        f.get("usuario_id", UUID.class),
                        Dinero.de(
                                f.get("saldo_actual", BigDecimal.class), Moneda.valueOf(f.get("moneda", String.class))),
                        f.get("estado", String.class),
                        f.get("es_subrogada", Boolean.class),
                        f.get("version", Integer.class)));
    }

    public record Deuda(UUID id, UUID usuarioId, Dinero saldoActual, String estado, boolean esSubrogada, int version) {}
}
