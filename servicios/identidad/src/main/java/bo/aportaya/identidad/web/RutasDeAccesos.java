package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU08DarDeBaja;
import bo.aportaya.identidad.aplicacion.CU08InformeDeAccesos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.PedidoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeAsignacion;
import bo.aportaya.identidad.aplicacion.GobiernoDeAccesos.ResultadoDeRevocacion;
import bo.aportaya.identidad.web.generado.UsuariosInternosApi;
import bo.aportaya.identidad.web.generado.modelo.EntradaAsignacionDeRol;
import bo.aportaya.identidad.web.generado.modelo.EntradaBajaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.EntradaRevocacionDeRol;
import bo.aportaya.identidad.web.generado.modelo.PaginaDeAccesos;
import bo.aportaya.identidad.web.generado.modelo.SalidaAsignacionDeRol;
import bo.aportaya.identidad.web.generado.modelo.SalidaBajaUsuarioInterno;
import bo.aportaya.identidad.web.generado.modelo.SalidaRevocacionDeRol;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * CU-08 por asignacion (GU-05, GU-08, GU-14): traduce y delega.
 *
 * <p>Las rutas viven bajo {@code /usuarios-internos}, y el generador Spring arma UNA interfaz por primer
 * segmento de la ruta ({@code UsuariosInternosApi}): dos {@code @RestController} que la implementaran
 * registrarian dos veces cada ruta. Por eso, como {@code RutasDeContactoEnGrupo} e
 * {@code InvitacionesController}, estas rutas van en una clase abstracta que {@link UsuariosInternosController}
 * extiende: un solo controlador, archivos chicos. Las reglas —step-up, nadie sobre si mismo,
 * nunca sin administrador— viven en {@code aplicacion/}; aca no hay ningun {@code if} de negocio.
 *
 * <p>Abren estas operaciones {@code ACCESOS_ADMINISTRAR} (el permiso del CU) o, como el resto de la gestion de
 * personal interno, {@code USUARIOS_INTERNOS_GESTIONAR}: asi {@code INTERNAL_IDENTITY_ADMIN} administra sin
 * tener un permiso que no se le sembro (decision P-2b). El informe es lectura: alcanza
 * {@code USUARIOS_INTERNOS_LEER}.
 */
abstract class RutasDeAccesos implements UsuariosInternosApi {

    private final GobiernoDeAccesos gobierno;
    private final CU08DarDeBaja baja;
    private final CU08InformeDeAccesos informe;
    private final SesionDeLaPeticion sesion;

    protected RutasDeAccesos(
            GobiernoDeAccesos gobierno, CU08DarDeBaja baja, CU08InformeDeAccesos informe, SesionDeLaPeticion sesion) {
        this.gobierno = gobierno;
        this.baja = baja;
        this.informe = informe;
        this.sesion = sesion;
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaAsignacionDeRol> asignarRolDeOperador(
            UUID idempotencyKey, UUID id, EntradaAsignacionDeRol entrada) {
        ResultadoDeAsignacion r = gobierno.asignar(
                new PedidoDeAsignacion(
                        id,
                        entrada.getRolId(),
                        entrada.getAmbito().getValue(),
                        Optional.ofNullable(entrada.getAmbitoId()),
                        Optional.ofNullable(entrada.getVigenteHasta()),
                        entrada.getJustificacion(),
                        entrada.getEvidenciaMfa()),
                sesion.actual());
        SalidaAsignacionDeRol salida = new SalidaAsignacionDeRol();
        salida.setAsignacionId(r.asignacionId());
        salida.setUsuarioId(r.usuarioId());
        salida.setRolCodigo(r.rolCodigo());
        salida.setAmbito(SalidaAsignacionDeRol.AmbitoEnum.fromValue(r.ambito()));
        salida.setAmbitoId(r.ambitoId().orElse(null));
        salida.setVigenteHasta(r.vigenteHasta().orElse(null));
        salida.setPermisosEfectivos(r.permisosEfectivos().stream().sorted().toList());
        salida.setRequiereMfa(r.requiereMfa());
        salida.setYaExistia(r.yaExistia());
        return ResponseEntity.status(r.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(salida);
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaRevocacionDeRol> revocarAsignacionDeRol(
            UUID idempotencyKey, UUID asignacionId, EntradaRevocacionDeRol entrada) {
        ResultadoDeRevocacion r = gobierno.revocar(
                asignacionId,
                entrada.getMotivoRevocacion(),
                !Boolean.FALSE.equals(entrada.getCerrarSesiones()),
                entrada.getEvidenciaMfa(),
                sesion.actual());
        SalidaRevocacionDeRol salida = new SalidaRevocacionDeRol();
        salida.setAsignacionId(r.asignacionId());
        salida.setUsuarioId(r.usuarioId());
        salida.setRevocadaEn(r.revocadaEn());
        salida.setSesionesCerradas(r.sesionesCerradas());
        salida.setYaEstabaRevocada(r.yaEstaba());
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaBajaUsuarioInterno> darDeBajaUsuarioInterno(
            UUID idempotencyKey, UUID id, EntradaBajaUsuarioInterno entrada) {
        CU08DarDeBaja.ResultadoDeBaja r =
                baja.ejecutar(id, entrada.getMotivo(), entrada.getEvidenciaMfa(), sesion.actual());
        SalidaBajaUsuarioInterno salida = new SalidaBajaUsuarioInterno();
        salida.setUsuarioId(r.usuarioId());
        salida.setEstado(r.estado());
        salida.setAsignacionesRevocadas(r.asignacionesRevocadas());
        salida.setSesionesCerradas(r.sesionesCerradas());
        salida.setYaEstabaDeBaja(r.yaEstaba());
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_LEER")
    public ResponseEntity<PaginaDeAccesos> informeDeAccesos(String seccion, Integer pagina, Integer limite) {
        CU08InformeDeAccesos.Pagina p = informe.ejecutar(
                MapeoDelInforme.seccion(seccion),
                MapeoDelInforme.pagina(pagina),
                MapeoDelInforme.limite(limite),
                sesion.actual());
        return ResponseEntity.ok(MapeoDelInforme.aPagina(p));
    }
}
