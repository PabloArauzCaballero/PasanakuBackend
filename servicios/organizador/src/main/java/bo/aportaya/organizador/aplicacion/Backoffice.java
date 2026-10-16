package bo.aportaya.organizador.aplicacion;

import bo.aportaya.plataforma.dominio.ContextoSesion;
import org.springframework.security.access.AccessDeniedException;

/** Backoffice con sesión propia: el permiso de la ruta no basta, el caso de uso lo vuelve a exigir. */
final class Backoffice {
    private Backoffice() {}

    static void exigir(ContextoSesion ctx) {
        if (!"BACKOFFICE".equals(ctx.rol()) && !"ADMIN_PLATAFORMA".equals(ctx.rol())) {
            throw new AccessDeniedException("solo backoffice");
        }
    }
}
