package bo.aportaya.inversiones.infraestructura.clientes;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicioConfigurada;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La credencial de servicio de inversiones hacia el libro: cuenta de servicio en identidad (se renueva sola), un token
 * ya emitido, o nada (falla cerrado). La logica de eleccion y de {@code CredencialPorLogin} es de la plataforma
 * ({@link CredencialDeServicioConfigurada}); aca solo viven los nombres de las propiedades de ESTE servicio.
 */
@Configuration
class ConfiguracionDeCredencialDeServicio {

    @Bean
    CredencialDeServicio credencialDeServicio(
            @Value("${aportaya.servicios.identidad:http://identidad:8080}") URI identidad,
            @Value("${aportaya.inversiones.libro.timeout:PT3S}") Duration timeout,
            @Value("${aportaya.inversiones.libro.credencial-servicio:}") String tokenEmitido,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.telefono:}") String telefono,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.clave:}") String clave,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.factor-totp-semilla:}") String semilla,
            @Value("${aportaya.inversiones.libro.cuenta-servicio.factor-fijo:}") String factorFijo) {
        return CredencialDeServicioConfigurada.de(
                "inversiones", "INVERSIONES", identidad, timeout, tokenEmitido, telefono, clave, semilla, factorFijo);
    }
}
