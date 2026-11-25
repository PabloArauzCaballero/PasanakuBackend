package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Solicitud;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.Reloj;
import bo.aportaya.plataforma.mensajeria.EventoDominio;
import bo.aportaya.plataforma.mensajeria.Outbox;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Los eventos de bitacora de CU-135 (forma v1): quien actuo, con que rol, desde donde y por que. Nunca datos
 * personales. Van por outbox, en la misma transaccion que el acto (regla 98.3.3).
 */
final class EventosDeRoles {

    private final Outbox outbox;
    private final Reloj reloj;

    EventosDeRoles(Outbox outbox, Reloj reloj) {
        this.outbox = outbox;
        this.reloj = reloj;
    }

    void emitir(
            DSLContext dsl,
            String tipo,
            UUID agregadoId,
            ContextoSesion ctx,
            String motivo,
            Map<String, Object> detalle) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("version", "1");
        carga.put("actorId", ctx.usuarioId().toString());
        carga.put("actorRol", ctx.rol());
        carga.put("origen", "PANEL_ADMIN");
        carga.put("motivo", motivo);
        carga.put("ocurridoEn", reloj.ahora().atOffset(ZoneOffset.UTC).toString());
        carga.putAll(detalle);
        outbox.emitir(
                dsl,
                new EventoDominio(
                        tipo,
                        "rol",
                        agregadoId,
                        carga,
                        UUID.fromString(ctx.traza().id())));
    }

    static Map<String, Object> detalleDeCreacion(String codigo, Set<String> permisos) {
        return Map.of("rolCodigo", codigo, "permisosAgregados", List.copyOf(new TreeSet<>(permisos)));
    }

    static Map<String, Object> detalleDeEdicion(String codigo, Set<String> agregados, Set<String> quitados) {
        return Map.of(
                "rolCodigo", codigo,
                "permisosAgregados", List.copyOf(new TreeSet<>(agregados)),
                "permisosQuitados", List.copyOf(new TreeSet<>(quitados)));
    }

    static Map<String, Object> detalleDeAprobacion(Solicitud s, Map<String, Object> base) {
        Map<String, Object> detalle = new LinkedHashMap<>(base);
        detalle.put("solicitudId", s.id().toString());
        detalle.put("propuestaPor", s.propuestaPor().toString());
        return detalle;
    }
}
