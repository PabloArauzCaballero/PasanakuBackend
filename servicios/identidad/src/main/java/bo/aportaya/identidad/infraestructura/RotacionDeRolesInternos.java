package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.ROL;

import bo.aportaya.identidad.dominio.ContinuidadDeAdministracion;
import bo.aportaya.identidad.dominio.SegregacionDeFunciones;
import bo.aportaya.identidad.infraestructura.GobiernoDeAccesosRepositorio.Asignacion;
import bo.aportaya.plataforma.dominio.CodigoError;
import bo.aportaya.plataforma.dominio.ErrorDeNegocio;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.jooq.DSLContext;

/**
 * Lleva los roles de un usuario interno al conjunto pedido por {@code PUT /usuarios-internos/{id}/roles}
 * (DEPRECADO desde GU-05; sacado de {@link UsuariosInternosRepositorio} por el limite de tamano de archivo).
 *
 * <p>Desde GU-06 aplica la DIFERENCIA, no un reemplazo: los roles que siguen conservan su asignacion (y con ella
 * quien y cuando los otorgo), solo se revocan los que salen y solo se escriben los que entran. Las sesiones se
 * cierran solo si se quito algun rol vigente: agregar un rol no echa a nadie. La plataforma nunca queda sin una
 * persona ACTIVA con {@code ACCESOS_ADMINISTRAR} vigente (GU-11).
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
        Map<String, UUID> mapaRoles = dsl.select(ROL.CODIGO, ROL.ID)
                .from(ROL)
                .where(ROL.CODIGO.in(roles))
                .fetchMap(ROL.CODIGO, ROL.ID);
        Set<UUID> pedidos = new LinkedHashSet<>();
        Set<String> resultantes = new TreeSet<>();
        for (String codRol : roles) {
            UUID rId = mapaRoles.get(codRol);
            if (rId == null) {
                throw new ErrorDeNegocio(CodigoError.de(8, 1), "El rol " + codRol + " no existe.");
            }
            pedidos.add(rId);
            resultantes.addAll(accesos.permisosDelRol(dsl, rId));
        }
        SegregacionDeFunciones.conflicto(resultantes).ifPresent(par -> {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 3),
                    "Nadie autoriza y ejecuta la misma operacion.",
                    Map.of("parIncompatible", par));
        });

        // Se conserva la asignacion vigente de cada rol pedido; sale toda la que no se pidio o ya vencio.
        Set<UUID> conservados = new LinkedHashSet<>();
        List<Asignacion> vencidas = new ArrayList<>();
        List<UUID> quitar = new ArrayList<>();
        for (Asignacion a : new GobiernoDeAccesosRepositorio().sinRevocarDe(dsl, uId)) {
            if (!a.vigenteEn(ahora)) {
                vencidas.add(a);
            } else if (pedidos.contains(a.rolId()) && !conservados.contains(a.rolId())) {
                conservados.add(a.rolId());
            } else {
                quitar.add(a.id());
            }
        }

        // Las vencidas se cierran primero: no cuentan como acceso y liberan uq_asignacion_vigente.
        for (Asignacion vencida : vencidas) {
            accesos.revocar(dsl, vencida.id(), "VENCIMIENTO: vigente_hasta cumplido", ahora);
        }
        for (UUID rId : pedidos) {
            if (!conservados.contains(rId)) {
                accesos.asignar(dsl, uId, rId, "GLOBAL", Optional.empty(), operadorId, Optional.empty(), ahora);
            }
        }

        int antes = accesos.titularesVigentesDelPermiso(
                dsl, ContinuidadDeAdministracion.PERMISO, List.of(), Optional.empty(), ahora);
        int despues = accesos.titularesVigentesDelPermiso(
                dsl, ContinuidadDeAdministracion.PERMISO, quitar, Optional.empty(), ahora);
        if (ContinuidadDeAdministracion.laDejaSinAdministrador(antes, despues)) {
            throw new ErrorDeNegocio(
                    CodigoError.de(8, 6), "La plataforma no puede quedarse sin quien administre accesos.");
        }
        for (UUID asignacion : quitar) {
            accesos.revocar(dsl, asignacion, motivo, ahora);
        }
        if (!quitar.isEmpty()) {
            accesos.cerrarSesionesDe(dsl, uId, motivo, ahora);
        }
    }
}
