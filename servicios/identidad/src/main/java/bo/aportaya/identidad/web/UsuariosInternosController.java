package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
import bo.aportaya.identidad.web.generado.UsuariosInternosApi;
import bo.aportaya.identidad.web.generado.modelo.EntradaActualizarUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAltaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignarRolesUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaDesbloqueoUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaNombresDeUsuariosInternos;
import bo.aportaya.identidad.web.generado.modelo.NombreDeUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoDetalle;
import bo.aportaya.identidad.web.generado.modelo.UsuarioInternoResumen;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuariosInternosController implements UsuariosInternosApi {

    private final GestionarUsuariosInternos casoDeUso;
    private final ResolverNombresDeUsuariosInternos nombres;
    private final SesionDeLaPeticion sesion;

    public UsuariosInternosController(
            GestionarUsuariosInternos casoDeUso, ResolverNombresDeUsuariosInternos nombres, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.nombres = nombres;
        this.sesion = sesion;
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_LEER")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<List<UsuarioInternoResumen>> listarUsuariosInternos(
            String estado, String rol, String busqueda) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(casoDeUso.listar(estado, rol, busqueda, ctx));
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_GESTIONAR")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<UsuarioInternoDetalle> crearUsuarioInterno(
            UUID idempotencyKey, EntradaAltaUsuarioInterno entrada) {
        ContextoSesion ctx = sesion.actual();
        UsuarioInternoDetalle nuevo = casoDeUso.crear(entrada, ctx);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_LEER")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<UsuarioInternoDetalle> consultarUsuarioInterno(UUID id) {
        ContextoSesion ctx = sesion.actual();
        return casoDeUso.consultar(id, ctx).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound()
                .build());
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_GESTIONAR")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<UsuarioInternoDetalle> actualizarUsuarioInterno(
            UUID id, EntradaActualizarUsuarioInterno entrada) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(casoDeUso.actualizar(id, entrada, ctx));
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_GESTIONAR")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<UsuarioInternoDetalle> asignarRolesUsuarioInterno(
            UUID id, EntradaAsignarRolesUsuarioInterno entrada) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(casoDeUso.asignarRoles(id, entrada, ctx));
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_GESTIONAR")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<UsuarioInternoDetalle> desbloquearUsuarioInterno(
            UUID id, EntradaDesbloqueoUsuarioInterno entrada) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(casoDeUso.desbloquear(id, entrada.getMotivo(), ctx));
    }

    // CU-132: la pantalla de parametros financieros muestra quien propuso y quien aprobo. La abre quien administra
    // el tarifario; las alternativas son quienes ya leen la ficha completa del personal interno, asi que no abren
    // nada que no vieran. Un participante (cliente) no tiene ninguno de los tres: 403.
    @Override
    @Permiso("TARIFARIO_ADMINISTRAR")
    @PermisoAlternativo({"USUARIOS_INTERNOS_LEER", "ACCESOS_ADMINISTRAR"})
    public ResponseEntity<List<NombreDeUsuarioInterno>> resolverNombresDeUsuariosInternos(
            EntradaNombresDeUsuariosInternos entrada) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(nombres.ejecutar(entrada.getUsuarioIds(), ctx));
    }
}
