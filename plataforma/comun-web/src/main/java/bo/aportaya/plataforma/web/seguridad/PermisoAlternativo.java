package bo.aportaya.plataforma.web.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Otros codigos del catalogo que tambien abren este endpoint, ademas del de {@link Permiso}.
 *
 * <p>Existe para el caso donde la ruta la usan dos poblaciones distintas por motivos distintos y un solo
 * codigo no las nombra a las dos: el administrador del grupo (rol de grupo) y el backoffice (permiso de
 * operacion). Cada codigo sigue la regla de {@link GuardiaDePermiso}: pasa si el token trae ese permiso o su
 * rol es ese codigo. <b>Abrir una alternativa es abrir acceso</b>: cada una debe estar justificada en el
 * endpoint y tener su prueba negativa.
 *
 * <p>Va APARTE de {@code @Permiso} y no como un atributo suyo a proposito: las herramientas que leen el
 * permiso principal de cada ruta (matriz de pruebas, coleccion, inventario) siguen viendo exactamente
 * {@code @Permiso("X")}, y una alternativa nunca puede ocultar la declaracion principal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PermisoAlternativo {

    String[] value();
}
