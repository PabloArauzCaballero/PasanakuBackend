package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Alta;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.PedidoDeAlta;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.PedidoDeRotacion;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Revocacion;
import bo.aportaya.identidad.aplicacion.CU136GobernarCuentasDeServicio.Rotacion;
import bo.aportaya.identidad.infraestructura.CuentasDeServicioRepositorio.Fila;
import bo.aportaya.identidad.web.generado.CuentasServicioApi;
import bo.aportaya.identidad.web.generado.modelo.CuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeAltaDeCuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeRevocacionDeCuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.EntradaDeRotacionDeCuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.SalidaDeAltaDeCuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.SalidaDeRevocacionDeCuentaDeServicio;
import bo.aportaya.identidad.web.generado.modelo.SalidaDeRotacionDeCuentaDeServicio;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * GU-18 · CU-136: traduce y delega. Las reglas viven en {@link CU136GobernarCuentasDeServicio}.
 *
 * <p>La credencial que devuelve la rotacion sale con {@code Cache-Control: no-store} y nunca se registra: ni este
 * controlador ni el caso de uso la escriben en un log.
 */
@RestController
public class CuentasServicioController implements CuentasServicioApi {

    private final CU136GobernarCuentasDeServicio casoDeUso;
    private final SesionDeLaPeticion sesion;

    public CuentasServicioController(CU136GobernarCuentasDeServicio casoDeUso, SesionDeLaPeticion sesion) {
        this.casoDeUso = casoDeUso;
        this.sesion = sesion;
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_LEER")
    public ResponseEntity<List<CuentaDeServicio>> listarCuentasDeServicio() {
        return ResponseEntity.ok(casoDeUso.listar(sesion.actual()).stream()
                .map(CuentasServicioController::salida)
                .toList());
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeAltaDeCuentaDeServicio> registrarCuentaDeServicio(
            UUID idempotencyKey, EntradaDeAltaDeCuentaDeServicio entrada) {
        Alta alta = casoDeUso.registrar(
                new PedidoDeAlta(
                        entrada.getUsuarioId(),
                        entrada.getNombre(),
                        entrada.getVenceEn(),
                        entrada.getMotivo(),
                        entrada.getEvidenciaMfa()),
                sesion.actual());
        SalidaDeAltaDeCuentaDeServicio salida = new SalidaDeAltaDeCuentaDeServicio();
        salida.setCuentaId(alta.cuentaId());
        salida.setUsuarioId(alta.usuarioId());
        salida.setRolCodigo(alta.rolCodigo());
        salida.setVenceEn(alta.venceEn());
        return ResponseEntity.status(HttpStatus.CREATED).body(salida);
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRotacionDeCuentaDeServicio> rotarCredencialDeCuentaDeServicio(
            UUID idempotencyKey, UUID cuentaId, EntradaDeRotacionDeCuentaDeServicio entrada) {
        Rotacion r = casoDeUso.rotar(
                cuentaId,
                new PedidoDeRotacion(
                        Optional.ofNullable(entrada.getVenceEn()), entrada.getMotivo(), entrada.getEvidenciaMfa()),
                sesion.actual());
        SalidaDeRotacionDeCuentaDeServicio salida = new SalidaDeRotacionDeCuentaDeServicio();
        salida.setCuentaId(r.cuentaId());
        salida.setCredencial(r.credencial());
        salida.setVenceEn(r.venceEn());
        salida.setSesionesCerradas(r.sesionesCerradas());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(salida);
    }

    @Override
    @Permiso("ACCESOS_ADMINISTRAR")
    @PermisoAlternativo("USUARIOS_INTERNOS_GESTIONAR")
    public ResponseEntity<SalidaDeRevocacionDeCuentaDeServicio> revocarCuentaDeServicio(
            UUID idempotencyKey, UUID cuentaId, EntradaDeRevocacionDeCuentaDeServicio entrada) {
        Revocacion r = casoDeUso.revocar(cuentaId, entrada.getMotivo(), entrada.getEvidenciaMfa(), sesion.actual());
        SalidaDeRevocacionDeCuentaDeServicio salida = new SalidaDeRevocacionDeCuentaDeServicio();
        salida.setCuentaId(r.cuentaId());
        salida.setAsignacionesRevocadas(r.asignacionesRevocadas());
        salida.setSesionesCerradas(r.sesionesCerradas());
        return ResponseEntity.ok(salida);
    }

    private static CuentaDeServicio salida(Fila f) {
        CuentaDeServicio c = new CuentaDeServicio();
        c.setId(f.id());
        c.setUsuarioId(f.usuarioId());
        c.setRolCodigo(f.rolCodigo());
        c.setNombre(f.nombre());
        c.setEstado(CuentaDeServicio.EstadoEnum.fromValue(f.estado()));
        c.setVenceEn(f.venceEn());
        c.setCreadaEn(f.creadaEn());
        c.setCredencialRotadaEn(f.credencialRotadaEn());
        c.setRevocadaEn(f.revocadaEn().orElse(null));
        return c;
    }
}
