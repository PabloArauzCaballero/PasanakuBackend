package bo.aportaya.plataforma.web.seguridad;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * La ruta la puede usar una sesion con alcance restringido (CU-134, D-6).
 *
 * <p>Una cuenta suspendida o bloqueada por seguridad entra con el reclamo {@code alcance=RESTRINGIDO}: puede ver
 * su medida, presentar su descargo, apelar, pagar lo que debe y reclamar, y nada mas. {@link GuardiaDePermiso}
 * niega con {@code 403} toda ruta que no lleve esta anotacion, aunque el token traiga el permiso que la ruta pide.
 * La lista es cerrada a proposito: <b>anotar una ruta es abrirla a una cuenta bajo medida</b>, y cada una se
 * justifica en el endpoint y tiene su prueba.
 *
 * <p>No reemplaza a {@code @Permiso}: la ruta sigue exigiendo su permiso como a cualquier sesion.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PermitidoEnRestriccion {}
