package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.USUARIO;
import static bo.aportaya.identidad.generado.Tables.USUARIO_INTERNO;

import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.Record;

final class UsuarioInternoMapeador {

    /** El bloqueo de {@code bloqueo_cuenta} que rige ahora; {@code hasta} es nulo si es indefinido. */
    record BloqueoVigente(OffsetDateTime hasta, String motivo) {}

    private UsuarioInternoMapeador() {}

    static UsuarioInternoResumen aResumen(
            Record r, UUID usuarioId, Set<String> rolesVigentes, boolean tieneMfa, Optional<BloqueoVigente> bloqueo) {
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
        resumen.setBloqueadoHasta(bloqueo.map(BloqueoVigente::hasta).orElse(null));
        resumen.setMotivoBloqueo(bloqueo.map(BloqueoVigente::motivo).orElse(null));
        resumen.setRoles(new ArrayList<>(rolesVigentes));
        resumen.setCreadoEn(r.get(USUARIO.FECHA_REGISTRO));
        return resumen;
    }

    static UsuarioInternoDetalle aDetalle(
            Record r,
            UUID usuarioId,
            Set<String> rolesVigentes,
            Set<String> permisosEfectivos,
            boolean tieneMfa,
            Optional<BloqueoVigente> bloqueo) {
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
        detalle.setBloqueadoHasta(bloqueo.map(BloqueoVigente::hasta).orElse(null));
        detalle.setMotivoBloqueo(bloqueo.map(BloqueoVigente::motivo).orElse(null));
        detalle.setCreadoEn(r.get(USUARIO.FECHA_REGISTRO));
        detalle.setRoles(new ArrayList<>(rolesVigentes));
        detalle.setPermisosEfectivos(new ArrayList<>(permisosEfectivos));
        return detalle;
    }
}
