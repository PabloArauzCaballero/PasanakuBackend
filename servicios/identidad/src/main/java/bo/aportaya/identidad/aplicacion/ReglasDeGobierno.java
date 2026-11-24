package bo.aportaya.identidad.aplicacion;

import bo.aportaya.identidad.dominio.ContinuidadDeAdministracion;
import bo.aportaya.identidad.dominio.puertos.EvidenciaDeStepUp;
import bo.aportaya.identidad.infraestructura.AccesosRepositorio;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * Las tres reglas que valen para TODO acto de gobierno de accesos (GU-11, decision P-7): step-up de quien
 * actua, nadie actua sobre si mismo y la plataforma nunca queda sin administrador de accesos.
 *
 * <p>Una sola pieza para que alta, suspension, reactivacion, baja, desbloqueo, asignacion y revocacion
 * digan lo mismo con el mismo codigo: tres copias de una regla de seguridad divergen la primera vez que
 * alguien arregla una.
 */
@Component
public class ReglasDeGobierno {

    private final EvidenciaDeStepUp evidencias;

    public ReglasDeGobierno(EvidenciaDeStepUp evidencias) {
        this.evidencias = evidencias;
    }

    /** AP-CU08-09: sin evidencia step-up valida de quien actua, el acto no ocurre. */
    public void exigirStepUp(ContextoSesion ctx, String evidencia) {
        if (!evidencias.consumir(ctx.usuarioId(), evidencia)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 9), "Confirma esta accion con tu segundo factor y volve a intentarlo.");
        }
    }

    /** AP-CU08-04: nadie se amplia, se quita ni se cambia a si mismo el acceso. */
    public static void noSobreSiMismo(ContextoSesion ctx, UUID afectado) {
        if (afectado.equals(ctx.usuarioId())) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 4), "Nadie gobierna su propio acceso: lo tiene que hacer otra persona.");
        }
    }

    /**
     * AP-CU08-06: el acto no puede dejar cero personas ACTIVAS con {@code ACCESOS_ADMINISTRAR} vigente.
     *
     * @param asignacionesQueSalen las asignaciones que el acto revoca
     * @param usuarioQueSale el usuario que deja de contar entero (suspension o baja), si lo hay
     */
    public static void noDejaSinAdministrador(
            AccesosRepositorio accesos,
            DSLContext dsl,
            Collection<UUID> asignacionesQueSalen,
            Optional<UUID> usuarioQueSale,
            OffsetDateTime ahora) {
        int antes = accesos.titularesVigentesDelPermiso(
                dsl, ContinuidadDeAdministracion.PERMISO, List.of(), Optional.empty(), ahora);
        int despues = accesos.titularesVigentesDelPermiso(
                dsl, ContinuidadDeAdministracion.PERMISO, asignacionesQueSalen, usuarioQueSale, ahora);
        if (ContinuidadDeAdministracion.laDejaSinAdministrador(antes, despues)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 6), "La plataforma no puede quedarse sin quien administre accesos.");
        }
    }
}
