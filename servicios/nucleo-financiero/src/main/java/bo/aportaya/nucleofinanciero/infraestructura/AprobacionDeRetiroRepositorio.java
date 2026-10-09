package bo.aportaya.nucleofinanciero.infraestructura;

import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/** Las dos salidas de {@code EN_REVISION} sobre {@code orden_retiro}: la segunda firma autoriza o rechaza. */
@Component
public class AprobacionDeRetiroRepositorio {

    /**
     * EN_REVISION → AUTORIZADA (H3.S2): un aprobador DISTINTO del solicitante.
     *
     * <p>{@code aprobada_por <> solicitada_por} se repite aca aunque
     * {@code ck_retiro_doble_aprobacion} ya lo exige en la base: la base rechaza con
     * un error generico de restriccion, y {@code CU11.aprobar} necesita distinguir
     * "ya no esta en revision" (alguien mas la resolvio primero) de "sos el mismo que
     * la pidio" para dar el codigo de error correcto — R-SEG-04 no perdona, pero el
     * mensaje si puede ser util.
     */
    public boolean pasarAAutorizadaPorAprobacion(DSLContext dsl, UUID ordenId, UUID aprobadaPor) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                        .set(DSL.field("estado", String.class), "AUTORIZADA")
                        .set(DSL.field("aprobada_por", UUID.class), aprobadaPor)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("EN_REVISION"))
                        .and(DSL.field("solicitada_por", UUID.class).ne(aprobadaPor))
                        .execute()
                > 0;
    }

    /** EN_REVISION → RECHAZADA (H3.S2): el aprobador la rechaza, sin llegar a AUTORIZADA. */
    public boolean pasarARechazadaPorAprobacion(DSLContext dsl, UUID ordenId, UUID aprobadaPor) {
        return dsl.update(DSL.table(DSL.name("nucleo_financiero", "orden_retiro")))
                        .set(DSL.field("estado", String.class), "RECHAZADA")
                        .set(DSL.field("aprobada_por", UUID.class), aprobadaPor)
                        .where(DSL.field("id", UUID.class).eq(ordenId))
                        .and(DSL.field("estado").eq("EN_REVISION"))
                        .and(DSL.field("solicitada_por", UUID.class).ne(aprobadaPor))
                        .execute()
                > 0;
    }
}
