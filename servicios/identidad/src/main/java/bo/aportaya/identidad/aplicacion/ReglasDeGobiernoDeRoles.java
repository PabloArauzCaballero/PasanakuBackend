package bo.aportaya.identidad.aplicacion;

import static bo.aportaya.identidad.dominio.ReglasDeRolPersonalizado.*;

import bo.aportaya.identidad.dominio.PermisosEfectivos;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio.FilaDeRol;
import bo.aportaya.identidad.infraestructura.RolesPersonalizadosRepositorio.FilaDeSolicitud;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jooq.DSLContext;

/** Las reglas de CU-135 que leen la base: permisos validos, segregacion, rol editable, solicitud pendiente y step-up. */
final class ReglasDeGobiernoDeRoles {

    private final RolesPersonalizadosRepositorio roles;
    private final AccesosRepositorio accesos;
    private final EvidenciaDeStepUp evidencias;

    ReglasDeGobiernoDeRoles(
            RolesPersonalizadosRepositorio roles, AccesosRepositorio accesos, EvidenciaDeStepUp evidencias) {
        this.roles = roles;
        this.accesos = accesos;
        this.evidencias = evidencias;
    }

    static void validarForma(String codigo, String nombre, String descripcion, Set<String> permisos, String motivo) {
        if (!codigoValido(codigo) && !"ROL_EXISTENTE".equals(codigo)) {
            throw new ErrorDeNegocio(
                    CODIGO_INVALIDO,
                    "El codigo va en mayusculas, sin espacios, de 3 a 30 caracteres, y no puede empezar con"
                            + " SERVICIO_.");
        }
        if (nombre == null || nombre.isBlank() || nombre.length() > LARGO_MAXIMO_NOMBRE) {
            throw new ErrorDeNegocio(CODIGO_INVALIDO, "El nombre es obligatorio (hasta 60 caracteres).");
        }
        if (descripcion != null && descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
            throw new ErrorDeNegocio(CODIGO_INVALIDO, "La descripcion admite hasta 255 caracteres.");
        }
        if (permisos == null || permisos.isEmpty()) {
            throw new ErrorDeNegocio(SIN_PERMISOS, "Un rol necesita al menos un permiso.");
        }
        if (!motivoValido(motivo)) {
            throw new ErrorDeNegocio(MOTIVO_REQUERIDO, "Escribi el motivo (hasta 160 caracteres).");
        }
    }

    /** Los permisos existen y no rompen la segregacion; devuelve su riesgo. */
    Map<String, String> validarPermisos(DSLContext dsl, Set<String> permisos) {
        Set<String> faltan = roles.inexistentes(dsl, permisos);
        if (!faltan.isEmpty()) {
            throw new ErrorDeNegocio(
                    PERMISO_INEXISTENTE, "Hay permisos que no existen en el catalogo.", Map.of("permisos", faltan));
        }
        conflicto(permisos).ifPresent(par -> {
            throw new ErrorDeNegocio(
                    PAR_INCOMPATIBLE, "Nadie autoriza y ejecuta la misma operacion.", Map.of("parIncompatible", par));
        });
        return roles.riesgoDe(dsl, permisos);
    }

    /** El nuevo conjunto no puede dejar a un titular vigente con un par incompatible (R-SEG-04 sobre lo ya otorgado). */
    void exigirQueNadieQuedeEnConflicto(DSLContext dsl, UUID rolId, Set<String> nuevos, OffsetDateTime ahora) {
        for (UUID titular : roles.titularesVigentes(dsl, rolId, ahora)) {
            Set<String> resultantes = new TreeSet<>(PermisosEfectivos.de(accesos.asignacionesDe(dsl, titular), ahora));
            resultantes.addAll(nuevos);
            conflicto(resultantes).ifPresent(par -> {
                throw new ErrorDeNegocio(
                        ASIGNADO_QUEDARIA_EN_CONFLICTO,
                        "Un titular de este rol quedaria con un par incompatible de permisos.",
                        Map.of("parIncompatible", par));
            });
        }
    }

    FilaDeRol editable(DSLContext dsl, UUID rolId) {
        FilaDeRol rol =
                roles.rol(dsl, rolId).orElseThrow(() -> new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol no existe."));
        if (rol.esSistema()) {
            throw new ErrorDeNegocio(ROL_DE_SISTEMA, "Los roles del sistema no se editan ni se retiran.");
        }
        if (rol.retirado()) {
            throw new ErrorDeNegocio(ROL_INEXISTENTE, "Ese rol ya esta retirado.");
        }
        return rol;
    }

    FilaDeSolicitud pendiente(DSLContext dsl, UUID solicitudId, ContextoSesion ctx) {
        FilaDeSolicitud fila = roles.solicitud(dsl, solicitudId)
                .orElseThrow(() -> new ErrorDeNegocio(SOLICITUD_INEXISTENTE, "Esa solicitud no existe."));
        if (!"PENDIENTE".equals(fila.estado())) {
            throw new ErrorDeNegocio(SOLICITUD_YA_RESUELTA, "Esa solicitud ya se resolvio.");
        }
        if (fila.propuestaPor().equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(
                    APROBADOR_ES_EL_PROPONENTE,
                    "Lo tiene que resolver otra persona: no podes resolver lo que propusiste.");
        }
        return fila;
    }

    void exigirStepUp(ContextoSesion ctx, String evidencia) {
        if (evidencia == null || !evidencias.consumir(ctx.usuarioId(), evidencia)) {
            throw new ErrorDeNegocio(
                    STEP_UP_REQUERIDO, "Confirma esta accion con tu segundo factor y volve a intentarlo.");
        }
    }
}
