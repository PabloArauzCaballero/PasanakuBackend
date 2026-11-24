package bo.aportaya.identidad.infraestructura;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code identidad.evidencia_step_up_consumida}: el {@code jti} de la evidencia step-up de un acto de gobierno de
 * accesos, consumido UNA sola vez entre replicas y reinicios (GU-11, decision de Pablo 2026-11-24).
 *
 * <p>El {@code INSERT ... ON CONFLICT (jti) DO NOTHING} es la barrera real (patron
 * {@code EvidenciaMfaConsumidaRepositorio} de nucleo-financiero): dos actos con el MISMO {@code jti} —simultaneos o
 * no, en la misma replica o en otra— solo uno gana la fila. La tabla es append-only
 * ({@code tg_evidencia_step_up_consumida_append_only}): nada puede «renovar» un {@code jti} gastado.
 */
@Component
public class EvidenciaStepUpConsumidaRepositorio {

    private final DSLContext dsl;

    public EvidenciaStepUpConsumidaRepositorio(DSLContext dsl) {
        this.dsl = dsl;
    }

    /** {@code true} si este acto consumio el {@code jti}; {@code false} si ya estaba usado. */
    public boolean consumir(UUID jti, UUID usuarioId, String proposito, OffsetDateTime ahora, OffsetDateTime vence) {
        return dsl.insertInto(DSL.table(DSL.name("identidad", "evidencia_step_up_consumida")))
                        .columns(
                                DSL.field(DSL.name("jti")),
                                DSL.field(DSL.name("usuario_id")),
                                DSL.field(DSL.name("proposito")),
                                DSL.field(DSL.name("consumida_en")),
                                DSL.field(DSL.name("vence_en")))
                        .values(DSL.val(jti), DSL.val(usuarioId), DSL.val(proposito), DSL.val(ahora), DSL.val(vence))
                        .onConflict(DSL.field(DSL.name("jti")))
                        .doNothing()
                        .execute()
                == 1;
    }
}
