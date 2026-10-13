package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.CerrarSesion;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.web.generado.SesionApi;
import bo.aportaya.identidad.web.generado.modelo.SalidaRenovacion;
import bo.aportaya.plataforma.dominio.SinContextoDeSesion;
import bo.aportaya.plataforma.web.seguridad.Permiso;
import bo.aportaya.plataforma.web.seguridad.PermisoAlternativo;
import bo.aportaya.plataforma.web.seguridad.Publico;
import bo.aportaya.plataforma.web.traza.Traza;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /sesion/refrescar}: el backoffice recupera la sesion al recargar (ADR-010).
 *
 * <p>Traduce y delega, como el resto de las paginas: la rotacion, la ventana de gracia y la
 * revocacion por reuso son de {@link RenovarSesion} y de la base. Lo unico que decide aca
 * es el codigo HTTP, despues del {@code COMMIT}: un 401 por reuso deja escrita la revocacion.
 */
@RestController
public class SesionController implements SesionApi {

    private final RenovarSesion refresco;
    private final EmitirAcceso acceso;
    private final CookieDeRefresco cookie;
    private final HttpServletRequest peticion;
    private final HttpServletResponse respuesta;
    private final CerrarSesion cerrarSesion;
    private final bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion sesion;

    public SesionController(
            RenovarSesion refresco,
            EmitirAcceso acceso,
            CookieDeRefresco cookie,
            HttpServletRequest peticion,
            HttpServletResponse respuesta,
            CerrarSesion cerrarSesion,
            bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion sesion) {
        this.refresco = refresco;
        this.acceso = acceso;
        this.cookie = cookie;
        this.peticion = peticion;
        this.respuesta = respuesta;
        this.cerrarSesion = cerrarSesion;
        this.sesion = sesion;
    }

    @Override
    @org.springframework.web.bind.annotation.RequestMapping(
            method = org.springframework.web.bind.annotation.RequestMethod.POST,
            value = "/sesion/refrescar",
            consumes = org.springframework.http.MediaType.ALL_VALUE)
    @Publico("ADR-010: la credencial es la cookie de refresh, no un token de acceso, que es justo lo que se renueva")
    public ResponseEntity<SalidaRenovacion> renovarSesion(
            bo.aportaya.identidad.web.generado.modelo.EntradaRenovacion entradaRenovacion) {
        Optional<String> cookiePresentada = cookie.leer(peticion);
        String token = cookiePresentada.orElseGet(() -> entradaRenovacion == null
                ? null
                : entradaRenovacion.getRefresco());
        var renovacion = refresco.renovar(
                token,
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido"),
                Traza.actual());

        if (!renovacion.renovada()) {
            // La cookie se borra: un refresh rechazado no tiene que volver a mandarse.
            if (cookiePresentada.isPresent()) respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.borrar());
            throw new SinContextoDeSesion("el refresh no es valido (ausente, vencido, revocado o reusado)");
        }

        String rol =
                renovacion.esOperador() ? SesionesController.ROL_DE_OPERADOR : SesionesController.ROL_DE_PARTICIPANTE;
        var emitido = acceso.ejecutar(
                renovacion.usuarioId().orElseThrow(),
                rol,
                SesionesController.NIVEL_POR_OMISION,
                null,
                renovacion.sesionId().orElseThrow());
        var siguiente = renovacion.siguiente().orElseThrow();

        SalidaRenovacion salida = new SalidaRenovacion();
        salida.setAcceso(emitido.token());
        salida.setRol(rol);
        salida.setPermisos(emitido.permisos());
        if (cookiePresentada.isPresent()) {
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.emitir(siguiente.token(), siguiente.expiraEn()))
                    .body(salida);
        }
        salida.setRefresco(siguiente.token());
        return ResponseEntity.ok(salida);
    }

    @Override
    @Permiso("PARTICIPANTE")
    @PermisoAlternativo({"BACKOFFICE"})
    public ResponseEntity<Void> cerrarSesion() {
        cerrarSesion.ejecutar(sesion.actual().usuarioId(), sesion.idDeLaSesion(), Traza.actual());
        if (cookie.leer(peticion).isPresent()) respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.borrar());
        return ResponseEntity.noContent().build();
    }
}
