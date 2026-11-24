package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.dominio.RolesParaPersonalInterno;
import bo.aportaya.identidad.web.generado.RolesApi;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RolesController implements RolesApi {

    private final GestionarUsuariosInternos casoDeUso;
    private final SesionDeLaPeticion sesion;

    public RolesController(GestionarUsuariosInternos casoDeUso, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.sesion = sesion;
    }

    @Override
    @Permiso("ROLES_INTERNOS_LEER")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<List<RolCatalogo>> listarRoles(Boolean asignablesAPersonalInterno) {
        ContextoSesion ctx = sesion.actual();
        List<RolCatalogo> roles = casoDeUso.listarRoles(ctx);
        if (Boolean.TRUE.equals(asignablesAPersonalInterno)) {
            // GU-13: el selector de roles de un operador no ofrece los de participante, grupo, anunciante ni servicio.
            roles = roles.stream()
                    .filter(rol -> RolesParaPersonalInterno.admite(rol.getCodigo(), rol.getAmbito()))
                    .toList();
        }
        return ResponseEntity.ok(roles);
    }
}
