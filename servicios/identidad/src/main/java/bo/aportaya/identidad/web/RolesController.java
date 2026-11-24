package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU135GobernarRoles;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.PedidoDeCreacion;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.PedidoDeEdicion;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Resultado;
import bo.aportaya.identidad.aplicacion.CU135GobernarRoles.Solicitud;
import bo.aportaya.identidad.aplicacion.GestionarUsuariosInternos;
import bo.aportaya.identidad.dominio.RolesParaPersonalInterno;
import bo.aportaya.identidad.web.generado.RolesApi;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeCreacionDeRol;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeEdicionDeRol;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeResolucionDeRol;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeRetiroDeRol;
import bo.aportaya.identidad.web.generado.modelo.RolCatalogo;
import bo.aportaya.identidad.web.generado.modelo.SalidaDeRol;
import bo.aportaya.identidad.web.generado.modelo.SolicitudDeRol;
import bo.aportaya.plataforma.dominio.ContextoSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RolesController implements RolesApi {

    private final GestionarUsuariosInternos casoDeUso;
    private final CU135GobernarRoles gobierno;
    private final SesionDeLaPeticion sesion;

    public RolesController(
            GestionarUsuariosInternos casoDeUso, CU135GobernarRoles gobierno, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.gobierno = gobierno;
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

    // ---- GU-16 · CU-135: aca solo se traduce; las reglas viven en CU135GobernarRoles ----

    @Override
    @Permiso("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRol> crearRol(UUID idempotencyKey, EntradaDeCreacionDeRol entrada) {
        Resultado r = gobierno.crear(
                new PedidoDeCreacion(
                        entrada.getCodigo(),
                        entrada.getNombre(),
                        entrada.getDescripcion(),
                        new TreeSet<>(entrada.getPermisos()),
                        entrada.getMotivo(),
                        entrada.getEvidenciaMfa()),
                sesion.actual());
        return ResponseEntity.status("APLICADO".equals(r.estado()) ? HttpStatus.CREATED : HttpStatus.ACCEPTED)
                .body(salida(r));
    }

    @Override
    @Permiso("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRol> editarRol(UUID idempotencyKey, UUID rolId, EntradaDeEdicionDeRol entrada) {
        Resultado r = gobierno.editar(
                rolId,
                new PedidoDeEdicion(
                        entrada.getNombre(),
                        entrada.getDescripcion(),
                        new TreeSet<>(entrada.getPermisos()),
                        entrada.getMotivo(),
                        entrada.getEvidenciaMfa()),
                sesion.actual());
        return ResponseEntity.status("APLICADO".equals(r.estado()) ? HttpStatus.OK : HttpStatus.ACCEPTED)
                .body(salida(r));
    }

    @Override
    @Permiso("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRol> retirarRol(UUID idempotencyKey, UUID rolId, EntradaDeRetiroDeRol entrada) {
        return ResponseEntity.ok(
                salida(gobierno.retirar(rolId, entrada.getMotivo(), entrada.getEvidenciaMfa(), sesion.actual())));
    }

    @Override
    @Permiso("ROLES_INTERNOS_LEER")
    @PermisoAlternativo("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<List<SolicitudDeRol>> listarSolicitudesDeRol() {
        return ResponseEntity.ok(gobierno.solicitudesPendientes(sesion.actual()).stream()
                .map(RolesController::salida)
                .toList());
    }

    @Override
    @Permiso("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRol> aprobarSolicitudDeRol(
            UUID idempotencyKey, UUID solicitudId, EntradaDeResolucionDeRol entrada) {
        return ResponseEntity.ok(
                salida(gobierno.aprobar(solicitudId, entrada.getMotivo(), entrada.getEvidenciaMfa(), sesion.actual())));
    }

    @Override
    @Permiso("ROLES_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRol> rechazarSolicitudDeRol(
            UUID idempotencyKey, UUID solicitudId, EntradaDeResolucionDeRol entrada) {
        return ResponseEntity.ok(salida(
                gobierno.rechazar(solicitudId, entrada.getMotivo(), entrada.getEvidenciaMfa(), sesion.actual())));
    }

    private static SalidaDeRol salida(Resultado r) {
        SalidaDeRol salida = new SalidaDeRol();
        salida.setEstado(SalidaDeRol.EstadoEnum.fromValue(r.estado()));
        salida.setRolId(r.rolId());
        salida.setSolicitudId(r.solicitudId());
        return salida;
    }

    private static SolicitudDeRol salida(Solicitud s) {
        SolicitudDeRol salida = new SolicitudDeRol();
        salida.setId(s.id());
        salida.setTipo(SolicitudDeRol.TipoEnum.fromValue(s.tipo()));
        salida.setRolId(s.rolId());
        salida.setCodigo(s.codigo());
        salida.setNombre(s.nombre());
        salida.setDescripcion(s.descripcion());
        salida.setPermisos(List.copyOf(s.permisos()));
        salida.setMotivo(s.motivo());
        salida.setPropuestaPor(s.propuestaPor());
        salida.setPropuestaEn(s.propuestaEn());
        return salida;
    }
}
