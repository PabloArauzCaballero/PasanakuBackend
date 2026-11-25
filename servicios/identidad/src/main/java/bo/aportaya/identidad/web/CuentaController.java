package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.CambiarClaveDeCuenta;
import bo.aportaya.identidad.aplicacion.InvitacionesDeCuenta;
import bo.aportaya.identidad.aplicacion.RecuperacionesDeAcceso;
import bo.aportaya.identidad.web.generado.CuentaApi;
import bo.aportaya.identidad.web.generado.modelo.EntradaCambioDeClave;
import bo.aportaya.identidad.web.generado.modelo.EntradaCompletarInvitacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaConsultaInvitacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaDecisionDeRecuperacion;
import bo.aportaya.identidad.web.generado.modelo.EntradaPedirRecuperacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaCambioDeClave;
import bo.aportaya.identidad.web.generado.modelo.SalidaCompletarInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaConsultaInvitacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaInicioEnrolamientoTotp;
import bo.aportaya.identidad.web.generado.modelo.SalidaPedidoDeRecuperacion;
import bo.aportaya.identidad.web.generado.modelo.SalidaRecuperaciones;
import bo.aportaya.identidad.web.generado.modelo.SolicitudDeRecuperacion;
import bo.aportaya.plataforma.dominio.Traza;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code /cuenta/*}: invitacion de un solo uso, cambio de clave y recuperacion asistida del operador
 * (GU-02, GU-03, GU-04; CU-09 §10-§11). Traduce y delega.
 *
 * <p>Los tres de la invitacion y el pedido de recuperacion son publicos a proposito: la credencial es el
 * token de un solo uso del correo (va en el cuerpo, nunca en la URL) y no hay sesion todavia. Lo que cada
 * uno hace con un token malo o sin permiso lo deciden los casos de uso, que responden lo mismo en todos
 * los casos de rechazo. El rechazo se traduce DESPUES del commit para que el intento fallido quede escrito.
 */
@RestController
public class CuentaController implements CuentaApi {

    static final String MENSAJE_DEL_PEDIDO =
            "Si el correo corresponde a un operador activo, otra persona de seguridad va a revisar tu pedido."
                    + " Despues de un plazo de espera te llega un correo con un enlace de un solo uso.";

    private final InvitacionesDeCuenta invitaciones;
    private final RecuperacionesDeAcceso recuperaciones;
    private final CambiarClaveDeCuenta cambioDeClave;
    private final SesionDeLaPeticion sesion;
    private final CookieDeRefresco cookie;
    private final HttpServletRequest peticion;
    private final HttpServletResponse respuesta;

    public CuentaController(
            InvitacionesDeCuenta invitaciones,
            RecuperacionesDeAcceso recuperaciones,
            CambiarClaveDeCuenta cambioDeClave,
            SesionDeLaPeticion sesion,
            CookieDeRefresco cookie,
            HttpServletRequest peticion,
            HttpServletResponse respuesta) {
        this.invitaciones = invitaciones;
        this.recuperaciones = recuperaciones;
        this.cambioDeClave = cambioDeClave;
        this.sesion = sesion;
        this.cookie = cookie;
        this.peticion = peticion;
        this.respuesta = respuesta;
    }

    @Override
    @Publico("CU-09 §10: la credencial es el token de un solo uso del correo institucional; no hay sesion todavia")
    public ResponseEntity<SalidaConsultaInvitacion> consultarInvitacion(EntradaConsultaInvitacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", "invitacion-consultar");
        var consulta = invitaciones.verificarVigencia(cuerpo.getToken(), origen());
        var salida = new SalidaConsultaInvitacion();
        salida.setAlcance(consulta.alcance().name());
        salida.setNombres(consulta.nombres());
        salida.setVenceEn(consulta.venceEn());
        salida.setRequiereClaveActual(consulta.alcance().pideClaveActual());
        salida.setRequiereFactor(consulta.alcance().pideFactor());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(salida);
    }

    @Override
    @Publico("CU-09 §10: la credencial es el token de un solo uso del correo institucional; no hay sesion todavia")
    public ResponseEntity<SalidaInicioEnrolamientoTotp> iniciarFactorDeInvitacion(EntradaConsultaInvitacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", "invitacion-factor");
        var inicio = invitaciones.iniciarFactor(cuerpo.getToken(), origen());
        var salida = new SalidaInicioEnrolamientoTotp();
        salida.setFactorId(inicio.factorId());
        salida.setSemilla(inicio.semillaBase32());
        salida.setUriOtpauth(inicio.uriOtpauth());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(salida);
    }

    @Override
    @Publico("CU-09 §10-§11: la credencial es el token de un solo uso del correo; no hay sesion todavia")
    public ResponseEntity<SalidaCompletarInvitacion> completarInvitacion(EntradaCompletarInvitacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", "invitacion-completar");
        var resultado = invitaciones.completar(
                new InvitacionesDeCuenta.Entrada(
                        cuerpo.getToken(),
                        cuerpo.getClaveNueva(),
                        cuerpo.getClaveActual(),
                        cuerpo.getFactorId(),
                        cuerpo.getCodigo()),
                origen());
        if (!resultado.exito()) {
            // Despues del commit: el intento fallido (codigo o clave actual) ya quedo contado.
            throw resultado.rechazo();
        }
        var salida = new SalidaCompletarInvitacion();
        salida.setAlcance(resultado.alcance().name());
        salida.setSesionesCerradas(resultado.sesionesCerradas());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(salida);
    }

    @Override
    @Permiso("PARTICIPANTE")
    @PermisoAlternativo({"BACKOFFICE"})
    public ResponseEntity<SalidaCambioDeClave> cambiarClave(EntradaCambioDeClave cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", "cambiar-clave");
        var salida = cambioDeClave.cambiar(
                cuerpo.getClaveActual(), cuerpo.getClaveNueva(), sesion.idDeLaSesion(), sesion.actual());
        if (salida.cierraLaSesionActual() && cookie.leer(peticion).isPresent()) {
            respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.borrar());
        }
        var cuerpoDeSalida = new SalidaCambioDeClave();
        cuerpoDeSalida.setSesionesCerradas(salida.sesionesCerradas());
        cuerpoDeSalida.setCierraLaSesionActual(salida.cierraLaSesionActual());
        return ResponseEntity.ok(cuerpoDeSalida);
    }

    @Override
    @Publico("CU-09 §11: quien perdio la clave o el telefono no tiene sesion; el pedido solo abre un expediente")
    public ResponseEntity<SalidaPedidoDeRecuperacion> pedirRecuperacion(EntradaPedirRecuperacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", "recuperacion-pedir");
        recuperaciones.solicitar(cuerpo.getCorreo(), cuerpo.getAlcance(), cuerpo.getMotivo(), origen());
        var salida = new SalidaPedidoDeRecuperacion();
        salida.setMensaje(MENSAJE_DEL_PEDIDO);
        return ResponseEntity.accepted().body(salida);
    }

    @Override
    @Permiso("SEGURIDAD_ACCESO_RESTABLECER")
    public ResponseEntity<SalidaRecuperaciones> listarRecuperaciones(
            String estado, Integer limite, OffsetDateTime antesDe, UUID antesDeId) {
        return ResponseEntity.ok(recuperaciones.listar(estado, limite, antesDe, antesDeId, sesion.actual()));
    }

    @Override
    @Permiso("SEGURIDAD_ACCESO_RESTABLECER")
    public ResponseEntity<SolicitudDeRecuperacion> aprobarRecuperacion(UUID id, EntradaDecisionDeRecuperacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", id.toString());
        return ResponseEntity.ok(
                recuperaciones.aprobar(id, cuerpo.getMotivo(), cuerpo.getEvidenciaMfa(), sesion.actual()));
    }

    @Override
    @Permiso("SEGURIDAD_ACCESO_RESTABLECER")
    public ResponseEntity<SolicitudDeRecuperacion> rechazarRecuperacion(UUID id, EntradaDecisionDeRecuperacion cuerpo) {
        bo.aportaya.plataforma.web.traza.Traza.marcarCasoDeUso("CU-09", id.toString());
        return ResponseEntity.ok(recuperaciones.rechazar(id, cuerpo.getMotivo(), sesion.actual()));
    }

    private InvitacionesDeCuenta.Origen origen() {
        return new InvitacionesDeCuenta.Origen(
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido"),
                new Traza(bo.aportaya.plataforma.web.traza.Traza.actual()));
    }
}
