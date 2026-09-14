package bo.aportaya.plataforma.archivos;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Registra el almacen de archivos en cualquier servicio que declare la conexion.
 *
 * <p>Va por autoconfiguracion y no por `@ComponentScan`: el paquete
 * `bo.aportaya.plataforma.archivos` esta fuera del paquete raiz de cada servicio, asi
 * que un `@Component` ahi adentro no lo ve nadie — el bean no existia y el servicio
 * no levantaba. Es el mismo mecanismo que ya usan comun-datos y comun-web.
 *
 * <p>`@ConditionalOnProperty`: un servicio que no guarda archivos no necesita el
 * cliente ni la configuracion, y no tiene por que fallar al arrancar por algo que no
 * usa.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "aportaya.archivos", name = "url")
public class ConfiguracionDeArchivos {

    @Bean
    @ConditionalOnMissingBean
    public AlmacenDeArchivos almacenDeArchivos(
            @Value("${aportaya.archivos.url}") String url,
            @Value("${aportaya.archivos.usuario}") String usuario,
            @Value("${aportaya.archivos.clave}") String clave,
            @Value("${aportaya.archivos.bucket}") String bucket) {
        return new AlmacenS3Adaptador(url, usuario, clave, bucket);
    }
}
