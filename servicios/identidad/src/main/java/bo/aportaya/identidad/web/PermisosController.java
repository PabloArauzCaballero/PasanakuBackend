package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.web.generado.PermisosApi;
import bo.aportaya.identidad.web.generado.modelo.PermisoCatalogo;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermisosController implements PermisosApi {

    private final GestionarUsuariosInternos casoDeUso;
    private final SesionDeLaPeticion sesion;

    public PermisosController(GestionarUsuariosInternos casoDeUso, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.sesion = sesion;
    }

    @Override
    @Permiso("PERMISOS_INTERNOS_LEER")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<List<PermisoCatalogo>> listarPermisos() {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(casoDeUso.listarPermisos(ctx));
    }
}
