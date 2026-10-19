package bo.aportaya.auditoria.infraestructura;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

/**
 * Acceso a {@code comun.bitacora_evento}, la bitacora inmutable encadenada por hash.
 */
@Component
public class BitacoraRepositorio {

    public record RegistroEvento(
            UUID id,
            Long secuencia,
            String entidad,
            UUID entidadId,
            String accion,
            UUID actorUsuarioId,
            String actorRol,
            String origen,
            String ipOrigen,
            String agenteUsuario,
            UUID correlationId,
            String valorAnteriorJson,
            String valorNuevoJson,
            String camposModificados,
            String motivo,
            String hashRegistro,
            String hashAnterior,
            OffsetDateTime fechaHora) {}

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

    public void registrarCambioDeCatalogo(
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
        dsl.insertInto(DSL.table(DSL.name("comun", "bitacora_evento")))
                .set(DSL.field("id", UUID.class), UUID.randomUUID())
                .set(DSL.field("entidad", String.class), entidad)
                .set(DSL.field("entidad_id", UUID.class), entidadId)
                .set(DSL.field("accion", String.class), accion)
                .set(DSL.field("actor_usuario_id", UUID.class), actorUsuarioId)
                .set(DSL.field("actor_rol", String.class), actorRol)
                .set(DSL.field("origen", String.class), "PANEL_ADMIN")
                .set(DSL.field("correlation_id", UUID.class), correlationId)
                .set(
                        DSL.field("valor_anterior", JSONB.class),
                        valorAnteriorJson == null ? null : JSONB.valueOf(valorAnteriorJson))
                .set(DSL.field("valor_nuevo", JSONB.class), JSONB.valueOf(valorNuevoJson))
                .set(DSL.field("motivo", String.class), motivo)
                .set(DSL.field("fecha_hora", OffsetDateTime.class), ahora)
                .execute();
    }

    public List<RegistroEvento> listar(
            DSLContext dsl,
            String entidad,
            String accion,
            UUID actorUsuarioId,
            OffsetDateTime desde,
            OffsetDateTime hasta,
            OffsetDateTime antesDe,
            UUID antesDeId,
            int limite) {
        return dsl.fetch(
                        """
                SELECT id, secuencia, entidad, entidad_id, accion, actor_usuario_id, actor_rol, origen,
                       ip_origen::varchar AS ip_origen, agente_usuario, correlation_id,
                       valor_anterior::text AS valor_anterior, valor_nuevo::text AS valor_nuevo,
                       campos_modificados, motivo, hash_registro, hash_anterior, fecha_hora
                  FROM comun.bitacora_evento
                 WHERE (?::varchar IS NULL OR entidad = ?::varchar)
                   AND (?::varchar IS NULL OR accion = ?::varchar)
                   AND (?::uuid IS NULL OR actor_usuario_id = ?::uuid)
                   AND (?::timestamptz IS NULL OR fecha_hora >= ?::timestamptz)
                   AND (?::timestamptz IS NULL OR fecha_hora <= ?::timestamptz)
                   AND (?::timestamptz IS NULL OR (fecha_hora, id) < (?::timestamptz, ?::uuid))
                 ORDER BY fecha_hora DESC, id DESC
                 LIMIT ?
                """,
                        entidad, entidad,
                        accion, accion,
                        actorUsuarioId, actorUsuarioId,
                        desde, desde,
                        hasta, hasta,
                        antesDe, antesDe, antesDeId,
                        limite)
                .map(this::mapearRegistro);
    }

    public Optional<RegistroEvento> buscarPorId(DSLContext dsl, UUID id) {
        return dsl.fetchOptional(
                        """
                SELECT id, secuencia, entidad, entidad_id, accion, actor_usuario_id, actor_rol, origen,
                       ip_origen::varchar AS ip_origen, agente_usuario, correlation_id,
                       valor_anterior::text AS valor_anterior, valor_nuevo::text AS valor_nuevo,
                       campos_modificados, motivo, hash_registro, hash_anterior, fecha_hora
                  FROM comun.bitacora_evento
                 WHERE id = ?
                 LIMIT 1
                """,
                        id)
                .map(this::mapearRegistro);
    }

    private RegistroEvento mapearRegistro(Record r) {
        return new RegistroEvento(
                r.get("id", UUID.class),
                r.get("secuencia", Long.class),
                r.get("entidad", String.class),
                r.get("entidad_id", UUID.class),
                r.get("accion", String.class),
                r.get("actor_usuario_id", UUID.class),
                r.get("actor_rol", String.class),
                r.get("origen", String.class),
                r.get("ip_origen", String.class),
                r.get("agente_usuario", String.class),
                r.get("correlation_id", UUID.class),
                r.get("valor_anterior", String.class),
                r.get("valor_nuevo", String.class),
                r.get("campos_modificados", String.class),
                r.get("motivo", String.class),
                r.get("hash_registro", String.class),
                r.get("hash_anterior", String.class),
                r.get("fecha_hora", OffsetDateTime.class));
    }
}
