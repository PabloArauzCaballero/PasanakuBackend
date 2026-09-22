package bo.aportaya.aportes.infraestructura;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * {@code comun.bitacora_evento}: la bitácora encadenada por hash (R-AUD-02/03,
 * H5/H7.S6 del carril PR4-seguridad).
 *
 * <p><b>Hallazgo, declarado antes de escribir esta clase</b>: al llegar a este
 * carril, {@code comun.bitacora_evento} existía en SQL (con su disparador de cadena
 * de hash y su bloqueo consultivo, {@code sql/40_reglas/restricciones.sql:90-104})
 * pero **ningún servicio la escribía** (`grep -rn "bitacora_evento" --include=*.java
 * servicios` → cero resultados en código de producción). El único escritor de
 * cualquier tabla de auditoría era {@code auditoria.ReporteRepositorio}, para su
 * propio {@code comun.registro_acceso_datos} — un mecanismo distinto, para un caso
 * distinto (acceso a datos sensibles en un reporte, no una operación crítica).
 *
 * <p>Este repositorio es lo que hace que {@code aportes} SÍ deje rastro en las
 * operaciones que {@code AuditoriaCriticaTest} exige. {@code hash_registro} y
 * {@code hash_anterior} los calcula el disparador de la base — no se envían aquí a
 * propósito (R-AUD-09: la aplicación no firma su propia huella).
 */
@Component
public class AuditoriaRepositorio {

    public UUID registrar(
            DSLContext dsl,
            String entidad,
            UUID entidadId,
            String accion,
            UUID actorUsuarioId,
            String actorRol,
            UUID correlationId,
            String valorAnteriorJson,
            String valorNuevoJson,
            String motivo,
            OffsetDateTime ahora) {
        UUID id = UUID.randomUUID();
        dsl.insertInto(DSL.table(DSL.name("comun", "bitacora_evento")))
                .set(DSL.field("id", UUID.class), id)
                .set(DSL.field("entidad", String.class), entidad)
                .set(DSL.field("entidad_id", UUID.class), entidadId)
                .set(DSL.field("accion", String.class), accion)
                .set(DSL.field("actor_usuario_id", UUID.class), actorUsuarioId)
                .set(DSL.field("actor_rol", String.class), actorRol)
                // API_PUBLICA: aportes no distingue todavia canal app/web (hallazgo
                // menor, no bloquea H5 — el catalogo de `origen` lo admite igual).
                .set(DSL.field("origen", String.class), "API_PUBLICA")
                .set(DSL.field("correlation_id", UUID.class), correlationId)
                .set(
                        DSL.field("valor_anterior", JSONB.class),
                        valorAnteriorJson == null ? null : JSONB.valueOf(valorAnteriorJson))
                .set(
                        DSL.field("valor_nuevo", JSONB.class),
                        valorNuevoJson == null ? null : JSONB.valueOf(valorNuevoJson))
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("fecha_hora", OffsetDateTime.class), ahora)
                .execute();
        return id;
    }
}
