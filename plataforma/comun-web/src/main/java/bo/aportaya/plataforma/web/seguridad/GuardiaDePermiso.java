package bo.aportaya.plataforma.web.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Hace cumplir {@link Permiso}. Antes de esto, la anotacion no autorizaba nada.
 *
 * <p><b>Por que existe.</b> {@link TodoEndpointDecideSuAcceso} comprobaba al arrancar
 * que cada endpoint declarara {@code @Permiso} o {@code @Publico}, y
 * {@link ConfiguracionDeSeguridad} exigia <i>sesion</i> a todo lo que no fuera
 * {@code @Publico}. Nadie leia el valor de la anotacion en una peticion. El efecto:
 * los treinta y dos controladores declaraban su permiso, el token ya lo traia, y
 * cualquier participante autenticado podia llamar {@code ENTREGA_AUTORIZAR} o
 * {@code REVERSO_AUTORIZAR}. La declaracion existia; la autorizacion no.
 *
 * <p><b>La regla.</b> El valor de {@code @Permiso} es un codigo del catalogo, y el
 * catalogo tiene dos tablas: {@code rol} y {@code permiso}. Ningun codigo esta en las
 * dos, asi que no hay ambiguedad que resolver — la peticion pasa si el token trae ese
 * permiso entre sus efectivos, o si su rol es exactamente ese codigo.
 *
 * <p>Los permisos <b>no se consultan</b>: viajan en el token porque CU-08 los calculo
 * al asignar el rol (ADR-024). Preguntarselos a {@code identidad} en cada peticion
 * ataria la disponibilidad de los catorce servicios a la de uno.
 *
 * <p>El fallo es {@link AccessDeniedException} y no una respuesta escrita a mano, para
 * que salga por {@code ManejadorGlobalDeErrores} con el mismo cuerpo que todo lo demas
 * y sin decir <i>por que</i>: explicar la negativa ya es contar que el recurso existe.
 *
 * <p>Falta de sesion y falta de permiso son cosas distintas y salen distinto —
 * {@code 401} contra {@code 403}. Confundirlas le impide al cliente saber si tiene que
 * volver a entrar o si simplemente no le corresponde.
 */
@Component
public class GuardiaDePermiso implements HandlerInterceptor {

    private static final Logger BITACORA = LoggerFactory.getLogger(GuardiaDePermiso.class);

    private final SesionDeLaPeticion sesion;

    public GuardiaDePermiso(SesionDeLaPeticion sesion) {
        this.sesion = sesion;
    }

    @Override
    public boolean preHandle(HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
        if (!(manejador instanceof HandlerMethod metodo)) {
            return true;
        }
        Permiso exigido = exigidoPor(metodo);
        if (exigido == null) {
            // O declara @Publico, o es de infraestructura. Que no declare ninguna de
            // las dos ya lo impidio el arranque; no se decide de nuevo aca.
            return true;
        }

        // Sin token esto lanza SinContextoDeSesion, que sale 401. No se traduce a 403:
        // son dos situaciones distintas para quien llama.
        var contexto = sesion.actual();
        String codigo = exigido.value();

        if (codigo.equals(contexto.rol()) || sesion.permisos().contains(codigo)) {
            return true;
        }

        BITACORA.info("acceso denegado: el rol {} no tiene {}", contexto.rol(), codigo);
        throw new AccessDeniedException("falta " + codigo);
    }

    /** Lo del metodo gana sobre lo de la clase: lo mas especifico manda, como en Spring. */
    private static Permiso exigidoPor(HandlerMethod metodo) {
        Permiso propio = metodo.getMethodAnnotation(Permiso.class);
        if (propio != null) {
            return propio;
        }
        if (metodo.getMethod().isAnnotationPresent(Publico.class)) {
            return null;
        }
        return metodo.getBeanType().getAnnotation(Permiso.class);
    }
}
