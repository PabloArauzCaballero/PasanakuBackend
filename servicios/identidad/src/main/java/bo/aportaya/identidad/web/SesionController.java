package bo.aportaya.identidad.web;

import bo.aportaya.identidad.aplicacion.EmitirAcceso;
import bo.aportaya.identidad.aplicacion.RenovarSesion;
import bo.aportaya.identidad.web.generado.SesionApi;
import bo.aportaya.identidad.web.generado.modelo.SalidaRenovacion;
import bo.aportaya.plataforma.dominio.SinContextoDeSesion;
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

    public SesionController(
            RenovarSesion refresco,
            EmitirAcceso acceso,
            CookieDeRefresco cookie,
            HttpServletRequest peticion,
            HttpServletResponse respuesta) {
        this.refresco = refresco;
        this.acceso = acceso;
        this.cookie = cookie;
        this.peticion = peticion;
        this.respuesta = respuesta;
    }

    @Override
    @Publico("ADR-010: la credencial es la cookie de refresh, no un token de acceso, que es justo lo que se renueva")
    public ResponseEntity<SalidaRenovacion> renovarSesion() {
        var renovacion = refresco.renovar(
                cookie.leer(peticion).orElse(null),
                Optional.ofNullable(peticion.getRemoteAddr()).orElse("0.0.0.0"),
                Optional.ofNullable(peticion.getHeader("User-Agent")).orElse("desconocido"),
                Traza.actual());

        if (!renovacion.renovada()) {
            // La cookie se borra: un refresh rechazado no tiene que volver a mandarse.
            respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie.borrar());
            throw new SinContextoDeSesion("el refresh no es valido (ausente, vencido, revocado o reusado)");
        }

        String rol =
                renovacion.esOperador() ? SesionesController.ROL_DE_OPERADOR : SesionesController.ROL_DE_PARTICIPANTE;
        var emitido =
                acceso.ejecutar(renovacion.usuarioId().orElseThrow(), rol, SesionesController.NIVEL_POR_OMISION, null);
        var siguiente = renovacion.siguiente().orElseThrow();

        SalidaRenovacion salida = new SalidaRenovacion();
        salida.setAcceso(emitido.token());
        salida.setRol(rol);
        salida.setPermisos(emitido.permisos());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.emitir(siguiente.token(), siguiente.expiraEn()))
                .body(salida);
    }
}
