package bo.aportaya.entregas.infraestructura;

import bo.aportaya.plataforma.web.clientes.CredencialDeServicio;
import bo.aportaya.plataforma.web.clientes.CredencialDeServicioConfigurada;
import java.net.URI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * La credencial de servicio de entregas hacia el libro ({@code SERVICIO_ENTREGAS}): cuenta de servicio en identidad
 * (se renueva sola), un token ya emitido, o nada (pagarAlVendedor falla cerrado). La eleccion y el ingreso por
 * {@code POST /sesiones} son de la plataforma; aca solo viven los nombres de las propiedades de ESTE servicio.
 */
@Configuration
class ConfiguracionDeCredencialDeServicio {

    @Bean
    CredencialDeServicio credencialDeServicio(
            @Value("${aportaya.servicios.identidad}") URI identidad,
            @Value("${aportaya.entregas.libro.timeout:PT3S}") Duration timeout,
            @Value("${aportaya.entregas.libro.credencial-servicio:}") String tokenEmitido,
            @Value("${aportaya.entregas.libro.cuenta-servicio.telefono:}") String telefono,
            @Value("${aportaya.entregas.libro.cuenta-servicio.clave:}") String clave,
            @Value("${aportaya.entregas.libro.cuenta-servicio.factor-totp-semilla:}") String semilla,
            @Value("${aportaya.entregas.libro.cuenta-servicio.factor-fijo:}") String factorFijo) {
        return CredencialDeServicioConfigurada.de(
                "entregas", "ENTREGAS", identidad, timeout, tokenEmitido, telefono, clave, semilla, factorFijo);
    }
}
