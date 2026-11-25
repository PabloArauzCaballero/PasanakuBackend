package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

/** Las escrituras de CU-135 sobre {@code rol} y {@code rol_permiso}: lo que ocurre cuando el cambio se aplica. */
final class CambiosDeRoles {

    private final RolesPersonalizadosRepositorio roles;

    CambiosDeRoles(RolesPersonalizadosRepositorio roles) {
        this.roles = roles;
    }

    UUID aplicarCreacion(
            DSLContext dsl,
            String codigo,
            String nombre,
            String descripcion,
            Set<String> permisos,
            ContextoSesion ctx,
            OffsetDateTime ahora) {
        UUID rolId = roles.insertarRol(dsl, codigo, nombre, descripcion, ctx.usuarioId(), ahora);
        roles.agregarPermisos(dsl, rolId, permisos);
        return rolId;
    }

    void aplicarEdicion(
            DSLContext dsl,
            UUID rolId,
            String nombre,
            String descripcion,
            Set<String> agregados,
            Set<String> quitados) {
        if (!roles.actualizarRol(dsl, rolId, nombre, descripcion)) {
            throw new ErrorDeNegocio(ROL_DE_SISTEMA, "Ese rol no se puede editar.");
        }
        roles.agregarPermisos(dsl, rolId, agregados);
        roles.quitarPermisos(dsl, rolId, quitados);
    }
}
