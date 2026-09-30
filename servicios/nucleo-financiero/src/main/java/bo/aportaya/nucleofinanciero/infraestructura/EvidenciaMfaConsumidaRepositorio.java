package bo.aportaya.nucleofinanciero.infraestructura;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code nucleo_financiero.evidencia_mfa_consumida} — el {@code jti} de la evidencia
 * step-up, consumido una sola vez (H2.S2.M2).
 *
 * <p>El {@code INSERT ... ON CONFLICT (jti) DO NOTHING} es la barrera real: dos
 * verificaciones con el MISMO {@code jti} —simultaneas o no— solo una gana la fila.
 * La tabla es append-only (`ck`/trigger `tg_evidencia_mfa_consumida_append_only`,
 * {@code REVOKE UPDATE, DELETE}): ni la aplicacion ni un error de programacion pueden
 * "renovar" un {@code jti} ya gastado.
 */
@Component
public class EvidenciaMfaConsumidaRepositorio {

    /** {@code true} si esta verificacion consumio el {@code jti}; {@code false} si ya estaba usado. */
    public boolean consumir(DSLContext dsl, UUID jti, UUID usuarioId, String proposito, OffsetDateTime ahora) {
        return dsl.insertInto(DSL.table(DSL.name("nucleo_financiero", "evidencia_mfa_consumida")))
                        .columns(
                                DSL.field("jti"),
                                DSL.field("usuario_id"),
                                DSL.field("proposito"),
                                DSL.field("consumida_en"))
                        .values(DSL.val(jti), DSL.val(usuarioId), DSL.val(proposito), DSL.val(ahora))
                        .onConflict(DSL.field("jti"))
                        .doNothing()
                        .execute()
                == 1;
    }
}
