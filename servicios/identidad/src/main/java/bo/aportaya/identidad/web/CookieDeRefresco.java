package bo.aportaya.identidad.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * La cookie del refresh del backoffice, con los atributos de ADR-010.
 *
 * <ul>
 *   <li>{@code HttpOnly}: ningun script de la pagina la lee — un XSS no se lleva la sesion.
 *   <li>{@code Secure}: solo viaja por HTTPS ({@code localhost} cuenta como seguro).
 *   <li>{@code SameSite=Strict}: otro sitio no puede hacer que el navegador la mande, que es
 *       lo que protege a {@code /sesion/refrescar} de CSRF.
 *   <li>{@code Path}: la ruta PUBLICA ({@code /api/v1/sesion}), no la que ve el servicio —el
 *       gateway saca el prefijo—; asi la cookie no viaja con ninguna otra peticion.
 * </ul>
 */
@Component
public class CookieDeRefresco {

    static final String NOMBRE = "aportaya_refresco";

    private final String ruta;

    public CookieDeRefresco(@Value("${aportaya.sesion.cookie-ruta:/api/v1/sesion}") String ruta) {
        this.ruta = ruta;
    }

    String emitir(String token, OffsetDateTime expiraEn) {
        long segundos = Math.max(
                0,
                Duration.between(OffsetDateTime.now(expiraEn.getOffset()), expiraEn)
                        .getSeconds());
        return base(token).maxAge(segundos).build().toString();
    }

    /** Borra la del navegador: un refresh rechazado no tiene que volver a mandarse. */
    String borrar() {
        return base("").maxAge(0).build().toString();
    }

    Optional<String> leer(HttpServletRequest peticion) {
        Cookie[] cookies = peticion.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> NOMBRE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valor) {
        return ResponseCookie.from(NOMBRE, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(ruta);
    }
}
