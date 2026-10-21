package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.jooq.Record;

final class UsuarioInternoMapeador {

    private UsuarioInternoMapeador() {}

    static UsuarioInternoResumen aResumen(Record r, UUID usuarioId, Set<String> rolesVigentes, boolean tieneMfa) {
        UsuarioInternoResumen resumen = new UsuarioInternoResumen();
        resumen.setId(r.get(USUARIO_INTERNO.ID));
        resumen.setUsuarioId(usuarioId);
        resumen.setCodigoPublico(r.get(USUARIO.CODIGO_PUBLICO));
        resumen.setNombres(r.get(USUARIO.NOMBRES));
        resumen.setApellidos(r.get(USUARIO.APELLIDOS));
        resumen.setCorreo(r.get(USUARIO.CORREO));
        resumen.setTelefono(r.get(USUARIO.TELEFONO_E164));
        resumen.setDepartamento(r.get(USUARIO_INTERNO.DEPARTAMENTO));
        resumen.setCargo(r.get(USUARIO_INTERNO.CARGO));
        resumen.setEstado(r.get(USUARIO.ESTADO));
        resumen.setDebeCambiarContrasena(r.get(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA));
        resumen.setTieneSegundoFactor(tieneMfa);
        resumen.setBloqueadoHasta(r.get(USUARIO_INTERNO.BLOQUEADO_HASTA));
        resumen.setMotivoBloqueo(r.get(USUARIO_INTERNO.MOTIVO_BLOQUEO));
        resumen.setRoles(new ArrayList<>(rolesVigentes));
        return resumen;
    }

    static UsuarioInternoDetalle aDetalle(
            Record r, UUID usuarioId, Set<String> rolesVigentes, Set<String> permisosEfectivos, boolean tieneMfa) {
        UsuarioInternoDetalle detalle = new UsuarioInternoDetalle();
        detalle.setId(r.get(USUARIO_INTERNO.ID));
        detalle.setUsuarioId(usuarioId);
        detalle.setCodigoPublico(r.get(USUARIO.CODIGO_PUBLICO));
        detalle.setNombres(r.get(USUARIO.NOMBRES));
        detalle.setApellidos(r.get(USUARIO.APELLIDOS));
        detalle.setCorreo(r.get(USUARIO.CORREO));
        detalle.setTelefono(r.get(USUARIO.TELEFONO_E164));
        detalle.setDepartamento(r.get(USUARIO_INTERNO.DEPARTAMENTO));
        detalle.setCargo(r.get(USUARIO_INTERNO.CARGO));
        detalle.setEstado(r.get(USUARIO.ESTADO));
        detalle.setDebeCambiarContrasena(r.get(USUARIO_INTERNO.DEBE_CAMBIAR_CONTRASENA));
        detalle.setTieneSegundoFactor(tieneMfa);
        detalle.setBloqueadoHasta(r.get(USUARIO_INTERNO.BLOQUEADO_HASTA));
        detalle.setMotivoBloqueo(r.get(USUARIO_INTERNO.MOTIVO_BLOQUEO));
        detalle.setCreadoEn(r.get(USUARIO.FECHA_REGISTRO));
        detalle.setRoles(new ArrayList<>(rolesVigentes));
        detalle.setPermisosEfectivos(new ArrayList<>(permisosEfectivos));
        return detalle;
    }
}
