package bo.aportaya.plataforma.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Un cuerpo con un campo que el contrato no declara se rechaza, no se ignora.
 *
 * <p>Los contratos dicen {@code additionalProperties: false} y la regla 96.2.2 pide lista
 * blanca de campos; Jackson, por omision, descarta en silencio lo que no conoce, de modo que
 * {@code {"esPep": false, "rol": "ADMIN"}} pasaba por un contrato que prometia no aceptarlo
 * (mass assignment). El rechazo sale como {@code 400 AP-VAL-02} por el manejador global.
 *
 * <p><b>Solo la entrada del servidor.</b> Se reemplaza el convertidor del lado MVC y no el
 * {@code ObjectMapper} del contexto: ese tambien lo usan los clientes HTTP hacia otros
 * servicios, y ahi la tolerancia es la correcta — que un productor agregue un campo opcional a
 * su respuesta no puede romper a quien lo consume (regla 98.1.3).
 */
@Configuration
public class CuerposEstrictos implements WebMvcConfigurer {

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> convertidores) {
        convertidores.replaceAll(convertidor -> {
            if (convertidor instanceof MappingJackson2HttpMessageConverter json) {
                ObjectMapper estricto = json.getObjectMapper().copy();
                estricto.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
                return new MappingJackson2HttpMessageConverter(estricto);
            }
            return convertidor;
        });
    }
}
