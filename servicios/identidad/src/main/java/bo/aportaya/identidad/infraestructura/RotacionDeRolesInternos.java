package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ASIGNACION_ROL;
import static bo.aportaya.identidad.generado.Tables.ROL;

import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Reemplaza los roles de un usuario interno (sacado de {@link UsuariosInternosRepositorio} para respetar el limite de
 * tamano de archivo): valida que los roles existan y no choquen por segregacion de funciones, revoca los vigentes sin
 * dejar la plataforma sin quien administre accesos, cierra sus sesiones y asigna los nuevos.
 */
final class RotacionDeRolesInternos {

    private RotacionDeRolesInternos() {}

    static void rotar(
            AccesosRepositorio accesos,
            DSLContext dsl,
            UUID uId,
            List<String> roles,
            String motivo,
            UUID operadorId,
            OffsetDateTime ahora) {
        Set<String> resultantes = new java.util.TreeSet<>();
        List<UUID> nuevosRolIds = new ArrayList<>();
        for (String codRol : roles) {
            UUID rId = dsl.select(ROL.ID)
                    .from(ROL)
                    .where(ROL.CODIGO.eq(codRol))
                    .fetchOptional(ROL.ID)
                    .orElseThrow(() -> new ErrorDeNegocio(CodigoError.de(8, 1), "El rol " + codRol + " no existe."));
            nuevosRolIds.add(rId);
            resultantes.addAll(accesos.permisosDelRol(dsl, rId));
        }

        SegregacionDeFunciones.conflicto(resultantes).ifPresent(par -> {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 3),
                    "Nadie autoriza y ejecuta la misma operacion.",
                    java.util.Map.of("parIncompatible", par));
        });

        // Revocar asignaciones vivas existentes garantizando permanencia de administracion de accesos
        List<UUID> asignacionesVivas = dsl.select(ASIGNACION_ROL.ID)
                .from(ASIGNACION_ROL)
                .where(ASIGNACION_ROL.USUARIO_ID.eq(uId))
                .and(ASIGNACION_ROL.REVOCADA_EN.isNull())
                .fetch(ASIGNACION_ROL.ID);

        for (UUID asigId : asignacionesVivas) {
            boolean otorgaLaAdministracion = accesos.rolDe(dsl, asigId)
                    .map(rol -> accesos.permisosDelRol(dsl, rol).contains("ACCESOS_ADMINISTRAR"))
                    .orElse(false);
            if (otorgaLaAdministracion && accesos.cuantosConservan(dsl, "ACCESOS_ADMINISTRAR", asigId) == 0) {
                throw new ErrorDeNegocio(
                        CodigoError.de(8, 6), "La plataforma no puede quedarse sin quien administre accesos.");
            }
            accesos.revocar(dsl, asigId, motivo, ahora);
        }

        // Invalidar sesiones activas del usuario al revocar/rotar roles
        accesos.cerrarSesionesDe(dsl, uId, motivo, ahora);

        // Asignar los nuevos roles
        for (UUID rId : nuevosRolIds) {
            accesos.asignar(dsl, uId, rId, "GLOBAL", Optional.empty(), operadorId, Optional.empty(), ahora);
        }
    }
}
