package bo.aportaya.plataforma.web.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cablea {@link GuardiaDePermiso} en los catorce servicios de una vez.
 *
 * <p>No hay ningun archivo donde un servicio se anote: lo trae {@code comun-web} por el
 * barrido de {@code ConfiguracionComunWeb}. Si cada servicio tuviera que registrar su
 * guardia, el que se olvidara quedaria sin autorizacion y verde.
 */
@Configuration
public class RegistroDeGuardias implements WebMvcConfigurer {

    private final GuardiaDePermiso permiso;

    public RegistroDeGuardias(GuardiaDePermiso permiso) {
        this.permiso = permiso;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        // Sin lista de rutas: el interceptor mira la anotacion del manejador, y las
        // rutas que no tienen manejador —las sondas, el JWKS— no llegan a el.
        registro.addInterceptor(permiso);
    }
}
