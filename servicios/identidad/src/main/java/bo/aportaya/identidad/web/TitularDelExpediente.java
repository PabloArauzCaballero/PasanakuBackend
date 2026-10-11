package bo.aportaya.identidad.web;

import bo.aportaya.plataforma.dominio.SinContextoDeSesion;
import bo.aportaya.plataforma.web.seguridad.SesionDeLaPeticion;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

/**
 * El control de objeto (IDOR) de la subida de fotos del expediente: quien escribe tiene que ser la
 * persona dueña de ese expediente.
 *
 * <p>Lo acredita con el token que devolvio SU alta ({@link TokenDeAlta}) o con su propia sesion. Se
 * decide ANTES de leer el archivo y de tocar el almacen o la base, de modo que la respuesta (el
 * {@code 403} de siempre, via {@code AccessDeniedException}) no depende de si el usuario existe.
 */
final class TitularDelExpediente {

    private static final Logger BITACORA = LoggerFactory.getLogger(TitularDelExpediente.class);

    private final TokenDeAlta tokenDeAlta;
    private final SesionDeLaPeticion sesion;

    TitularDelExpediente(TokenDeAlta tokenDeAlta, SesionDeLaPeticion sesion) {
        this.tokenDeAlta = tokenDeAlta;
        this.sesion = sesion;
    }

    void exigir(UUID usuarioId, String tokenPresentado) {
        if (tokenDeAlta.valido(tokenPresentado, usuarioId) || esLaSesionDe(usuarioId)) {
            return;
        }
        BITACORA.info("subida de documento denegada: ni token de alta de ese usuario ni su sesion");
        throw new AccessDeniedException("subida de documento denegada");
    }

    private boolean esLaSesionDe(UUID usuarioId) {
        try {
            return usuarioId.equals(sesion.actual().usuarioId());
        } catch (SinContextoDeSesion sinSesion) {
            return false;
        }
    }
}
