package bo.aportaya.auditoria.infraestructura;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Escribe en {@code comun.bitacora_evento}, la bitacora encadenada por hash. El hash lo calcula la base (R-AUD-09):
 * aca solo se inserta el hecho. Mismo patron que {@code aportes.AuditoriaRepositorio}.
 */
@Component
public class BitacoraRepositorio {

    public void registrarCambioDeConfiguracion(
            DSLContext dsl,
            String entidad,
            UUID entidadId,
            UUID actorUsuarioId,
            String actorRol,
            UUID correlationId,
            String valorAnteriorJson,
            String valorNuevoJson,
            OffsetDateTime ahora) {
        dsl.insertInto(DSL.table(DSL.name("comun", "bitacora_evento")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("entidad", String.class), entidad)
                .set(DSL.field("entidad_id", UUID.class), entidadId)
                .set(DSL.field("accion", String.class), "CAMBIO_CONFIGURACION")
                .set(DSL.field("actor_usuario_id", UUID.class), actorUsuarioId)
                .set(DSL.field("actor_rol", String.class), actorRol)
                .set(DSL.field("origen", String.class), "PANEL_ADMIN")
                .set(DSL.field("correlation_id", UUID.class), correlationId)
                .set(
                        DSL.field("valor_anterior", JSONB.class),
                        valorAnteriorJson == null ? null : JSONB.valueOf(valorAnteriorJson))
                .set(DSL.field("valor_nuevo", JSONB.class), JSONB.valueOf(valorNuevoJson))
                .set(DSL.field("fecha_hora", OffsetDateTime.class), ahora)
                .execute();
    }
}
