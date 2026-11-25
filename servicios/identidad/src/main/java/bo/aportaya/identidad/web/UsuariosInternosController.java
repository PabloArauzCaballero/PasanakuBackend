package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU08DarDeBaja;
import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos;
import bo.aportaya.identidad.aplicacion.ListarUsuariosInternos;
import bo.aportaya.identidad.aplicacion.ResolverNombresDeUsuariosInternos;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuariosInternosController extends RutasDeAccesos {

    private final GestionarUsuariosInternos casoDeUso;
    private final ListarUsuariosInternos listado;
    private final ResolverNombresDeUsuariosInternos nombres;
    private final SesionDeLaPeticion sesion;

    public UsuariosInternosController(
            GestionarUsuariosInternos casoDeUso,
            ListarUsuariosInternos listado,
            ResolverNombresDeUsuariosInternos nombres,
            SesionDeLaPeticion sesion,
            GobiernoDeAccesos gobierno,
            CU08DarDeBaja baja,
            CU08InformeDeAccesos informe) {
        super(gobierno, baja, informe, sesion);
        this.casoDeUso = casoDeUso;
        this.listado = listado;
        this.nombres = nombres;
        this.sesion = sesion;
    }

    @Override
    @Permiso("USUARIOS_INTERNOS_LEER")
    @PermisoAlternativo("ACCESOS_ADMINISTRAR")
    public ResponseEntity<List<UsuarioInternoResumen>> listarUsuariosInternos(
            String estado, String rol, String busqueda, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        ContextoSesion ctx = sesion.actual();
        return ResponseEntity.ok(listado.listar(estado, rol, busqueda, limite, antesDe, antesDeId, ctx));
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

    /** DEPRECADO (GU-05): reemplazado por {@code RutasDeAccesos}; compatible mientras el backoffice migra. */
    @Deprecated
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
        return ResponseEntity.ok(casoDeUso.desbloquear(id, entrada.getMotivo(), entrada.getEvidenciaMfa(), ctx));
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
