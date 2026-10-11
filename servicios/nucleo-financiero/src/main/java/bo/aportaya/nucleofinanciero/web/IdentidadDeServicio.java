package bo.aportaya.nucleofinanciero.web;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;

/**
 * La identidad de servicio, tal como la emite identidad (ADR-024): el reclamo {@code rol} trae el piso de la sesion
 * (PARTICIPANTE o BACKOFFICE) y el rol de servicio viaja en {@code permisos}. {@code @Permiso} ya exigio que el token
 * lo traiga (firmado por identidad); el caso de uso lo vuelve a exigir por el rol del contexto, asi que aca el
 * contexto toma el rol de servicio que ese permiso acredita. Un token sin el permiso no se disfraza de servicio.
 */
final class IdentidadDeServicio {

    private IdentidadDeServicio() {}

    static ContextoSesion como(SesionDeLaPeticion sesion, String servicio) {
        ContextoSesion ctx = sesion.actual();
        if (servicio.equals(ctx.rol()) || !sesion.permisos().contains(servicio)) {
            return ctx;
        }
        return new ContextoSesion(ctx.usuarioId(), servicio, ctx.traza(), ctx.dispositivo());
    }
}
