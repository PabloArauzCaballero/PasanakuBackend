package bo.aportaya.identidad.infraestructura;

import static bo.aportaya.identidad.generado.Tables.PERMISO;
import static bo.aportaya.identidad.generado.Tables.ROL;

import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Component;

/**
 * Consulta de catálogos de roles y permisos institucionales del sistema.
 */
@Component
public class RolesYPermisosRepositorio {

    private final AccesosRepositorio accesos;

    public RolesYPermisosRepositorio(AccesosRepositorio accesos) {
        this.accesos = accesos;
    }

    public List<RolCatalogo> listarRoles(DSLContext dsl) {
        var filas = dsl.select(
                        ROL.ID, ROL.CODIGO, ROL.NOMBRE, ROL.AMBITO, ROL.DEPARTAMENTO, ROL.DESCRIPCION, ROL.ES_SISTEMA)
                .from(ROL)
                .orderBy(ROL.DEPARTAMENTO.asc().nullsLast(), ROL.NOMBRE.asc())
                .fetch();

        List<RolCatalogo> lista = new ArrayList<>();
        for (Record r : filas) {
            UUID rolId = r.get(ROL.ID);
            Set<String> perms = accesos.permisosDelRol(dsl, rolId);

            RolCatalogo rc = new RolCatalogo();
            rc.setId(rolId);
            rc.setCodigo(r.get(ROL.CODIGO));
            rc.setNombre(r.get(ROL.NOMBRE));
            rc.setAmbito(r.get(ROL.AMBITO));
            rc.setDepartamento(r.get(ROL.DEPARTAMENTO));
            rc.setDescripcion(r.get(ROL.DESCRIPCION));
            rc.setEsSistema(r.get(ROL.ES_SISTEMA));
            rc.setPermisos(new ArrayList<>(perms));
            lista.add(rc);
        }
        return lista;
    }

    public List<PermisoCatalogo> listarPermisos(DSLContext dsl) {
        var filas = dsl.select(
                        PERMISO.ID,
                        PERMISO.CODIGO,
                        PERMISO.DESCRIPCION,
                        PERMISO.RECURSO,
                        PERMISO.ACCION,
                        PERMISO.NIVEL_RIESGO,
                        PERMISO.EXIGE_MOTIVO,
                        PERMISO.REQUIERE_MFA)
                .from(PERMISO)
                .orderBy(PERMISO.RECURSO.asc(), PERMISO.ACCION.asc())
                .fetch();

        List<PermisoCatalogo> lista = new ArrayList<>();
        for (Record r : filas) {
            PermisoCatalogo pc = new PermisoCatalogo();
            pc.setId(r.get(PERMISO.ID));
            pc.setCodigo(r.get(PERMISO.CODIGO));
            pc.setDescripcion(r.get(PERMISO.DESCRIPCION));
            pc.setRecurso(r.get(PERMISO.RECURSO));
            pc.setAccion(r.get(PERMISO.ACCION));
            pc.setNivelRiesgo(PermisoCatalogo.NivelRiesgoEnum.fromValue(r.get(PERMISO.NIVEL_RIESGO)));
            pc.setExigeMotivo(r.get(PERMISO.EXIGE_MOTIVO));
            pc.setRequiereMfa(r.get(PERMISO.REQUIERE_MFA));
            lista.add(pc);
        }
        return lista;
    }
}
